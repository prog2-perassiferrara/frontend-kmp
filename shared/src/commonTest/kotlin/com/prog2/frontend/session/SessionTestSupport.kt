package com.prog2.frontend.session

import com.prog2.frontend.session.data.AuthApi
import com.prog2.frontend.session.data.SessionStore
import kotlinx.coroutines.CompletableDeferred
import kotlin.io.encoding.Base64
import kotlin.time.Clock
import kotlin.time.Instant

/** JWT ficticio: el cliente solo lee `exp`; la firma no se valida localmente. */
internal fun testToken(expiresAt: Long = 2_000): String {
    val payload = """{"exp":$expiresAt}"""
    val encoder = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
    return listOf("""{"alg":"RS256"}""", payload, "test-signature").joinToString(".") {
        encoder.encode(it.encodeToByteArray())
    }
}

internal class TestClock(var seconds: Long = 1_000) : Clock {
    override fun now(): Instant = Instant.fromEpochSeconds(seconds)
}

internal class MemorySessionStore(var saved: String? = null) : SessionStore {
    var reads = 0
    var clears = 0
    var failSave = false
    var failRead = false
    var failClear = false
    var saveGate: CompletableDeferred<Unit>? = null

    override suspend fun read(): String? {
        reads++
        if (failRead) throw SessionException(SessionError.STORAGE_UNAVAILABLE)
        return saved
    }

    override suspend fun save(token: String) {
        saveGate?.await()
        if (failSave) throw SessionException(SessionError.STORAGE_UNAVAILABLE)
        saved = token
    }

    override suspend fun clear() {
        clears++
        if (failClear) throw SessionException(SessionError.STORAGE_UNAVAILABLE)
        saved = null
    }
}

internal class TestAuthApi(
    var answer: suspend (String, String, Boolean) -> String = { _, _, _ -> testToken() },
) : AuthApi {
    var requests = 0
    var username: String? = null
    var password: String? = null
    var rememberMe: Boolean? = null

    override suspend fun authenticate(username: String, password: String, rememberMe: Boolean): String {
        requests++
        this.username = username
        this.password = password
        this.rememberMe = rememberMe
        return answer(username, password, rememberMe)
    }
}
