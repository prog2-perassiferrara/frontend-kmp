package com.prog2.frontend.session.data

import androidx.test.platform.app.InstrumentationRegistry
import com.prog2.frontend.session.SessionException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.File
import java.security.KeyStore

/** Ejercita APIs nativas reales; el almacenamiento pertenece al APK instrumental. */
class AndroidEncryptedSessionStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val file = File(context.noBackupFilesDir, AndroidEncryptedSessionStore.FILE_NAME)
    private val store = AndroidEncryptedSessionStore(context)

    @Before fun setUp() = runBlocking { store.clear() }
    @After fun tearDown() = runBlocking { store.clear() }

    @Test fun encryptsAndRestoresFromAnotherInstanceWithFreshIv() = runBlocking {
        val value = "fictitious-user-token"
        store.save(value)
        val first = file.readBytes()
        assertFalse(first.decodeToString().contains(value))
        assertEquals(value, AndroidEncryptedSessionStore(context).read())
        store.save(value)
        assertFalse(first.contentEquals(file.readBytes()))
        assertEquals(context.noBackupFilesDir, file.parentFile)
        store.clear()
        assertNull(AndroidEncryptedSessionStore(context).read())
        assertFalse(file.exists())
        val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        assertFalse(keys.containsAlias(AndroidEncryptedSessionStore.KEY_ALIAS))
    }

    @Test fun rejectsAlteredCiphertext(): Unit = runBlocking {
        store.save("fictitious-user-token")
        val bytes = file.readBytes()
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        file.writeBytes(bytes)
        assertThrows(SessionException::class.java) { runBlocking { store.read() } }
    }

    @Test fun rejectsAFileWhoseKeyNoLongerExists(): Unit = runBlocking {
        store.save("fictitious-user-token")
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(AndroidEncryptedSessionStore.KEY_ALIAS) }
        assertThrows(SessionException::class.java) { runBlocking { store.read() } }
    }
}
