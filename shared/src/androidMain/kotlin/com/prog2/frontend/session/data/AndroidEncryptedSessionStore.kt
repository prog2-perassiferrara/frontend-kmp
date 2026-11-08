package com.prog2.frontend.session.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.prog2.frontend.session.SessionError
import com.prog2.frontend.session.SessionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Guarda solo el JWT cifrado. SessionManager serializa sus operaciones; AtomicFile
 * protege la escritura ante interrupciones, pero no proporciona exclusión mutua.
 * El archivo no participa del backup y la clave nunca sale de Android Keystore.
 */
class AndroidEncryptedSessionStore(context: Context) : SessionStore {
    private val file = AtomicFile(File(context.noBackupFilesDir, FILE_NAME))
    private val keys by lazy { KeyStore.getInstance("AndroidKeyStore").apply { load(null) } }

    override suspend fun read(): String? = protectedOperation {
        val bytes = try {
            file.readFully()
        } catch (failure: FileNotFoundException) {
            if (file.baseFile.exists()) throw failure
            return@protectedOperation null
        }
        require(bytes.size >= 30 && bytes[0] == 1.toByte() && bytes[1] == 12.toByte())
        val key = keys.getKey(KEY_ALIAS, null) as? SecretKey
            ?: throw GeneralSecurityException("No existe la clave de sesión.")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(2, 14)))
        cipher.doFinal(bytes.copyOfRange(14, bytes.size)).decodeToString()
    }

    override suspend fun save(token: String) = protectedOperation {
        val key = keys.getKey(KEY_ALIAS, null) as? SecretKey ?: createKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val bytes = byteArrayOf(1, cipher.iv.size.toByte()) + cipher.iv + cipher.doFinal(token.encodeToByteArray())
        val output = file.startWrite()
        try {
            output.write(bytes)
            file.finishWrite(output)
        } catch (failure: IOException) {
            file.failWrite(output)
            throw failure
        }
    }

    override suspend fun clear(): Unit = protectedOperation {
        // Se intenta quitar ambos elementos aunque uno falle. La clave también invalida
        // cualquier ciphertext residual; no se declara éxito con archivos o alias restantes.
        var keyFailure: GeneralSecurityException? = null
        try {
            keys.deleteEntry(KEY_ALIAS)
        } catch (failure: GeneralSecurityException) {
            keyFailure = failure
        }
        file.delete()
        if (listOf("", ".bak", ".new").any { File(file.baseFile.path + it).exists() }) {
            throw IOException("No se pudo borrar el archivo de sesión.")
        }
        keyFailure?.let { throw it }
    }

    private fun createKey(): SecretKey = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        .apply {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
        }.generateKey()

    private suspend fun <T> protectedOperation(block: () -> T): T = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (_: GeneralSecurityException) {
            throw SessionException(SessionError.STORAGE_UNAVAILABLE)
        } catch (_: ProviderException) {
            throw SessionException(SessionError.STORAGE_UNAVAILABLE)
        } catch (_: IOException) {
            throw SessionException(SessionError.STORAGE_UNAVAILABLE)
        } catch (_: IllegalArgumentException) {
            throw SessionException(SessionError.STORAGE_UNAVAILABLE)
        }
    }

    internal companion object {
        const val FILE_NAME = "user-session.bin"
        const val KEY_ALIAS = "frontend.user-session"
    }
}
