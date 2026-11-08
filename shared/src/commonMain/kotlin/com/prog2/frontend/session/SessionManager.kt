package com.prog2.frontend.session

import com.prog2.frontend.session.data.AuthApi
import com.prog2.frontend.session.data.SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Coordina la sesión del proceso. El `epoch` identifica cada apertura, de modo que
 * una respuesta tardía no publique datos ni cierre una sesión distinta. La red
 * nunca se espera con el mutex tomado.
 */
class SessionManager(
    private val authApi: AuthApi,
    private val store: SessionStore,
    private val expiry: JwtExpiry,
    private val clock: Clock,
) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(SessionState())
    private var restored = false
    private var epoch = 0L
    private var token: String? = null
    private var expiresAt: Instant? = null

    /** Queda activo si el almacenamiento no pudo limpiarse; bloquea un login nuevo. */
    private var cleanupRequired = false

    val state: StateFlow<SessionState> = mutableState.asStateFlow()

    /** Restaura una sola vez; un fallo de lectura nunca habilita acceso por omisión. */
    suspend fun restore() {
        mutex.withLock {
            if (restored) return
            restored = true
            try {
                val saved = store.read()
                if (saved == null) {
                    mutableState.value = SessionState(SessionStatus.SIGNED_OUT, epoch)
                    return@withLock
                }
                val savedExpiry = expiry.read(saved)
                if (clock.now() < savedExpiry) {
                    token = saved
                    expiresAt = savedExpiry
                    mutableState.value = SessionState(SessionStatus.SIGNED_IN, epoch)
                } else {
                    clearLocked(SessionError.SESSION_REQUIRED)
                }
            } catch (cancelled: CancellationException) {
                restored = false
                throw cancelled
            } catch (failure: SessionException) {
                val reason = if (failure.error == SessionError.INVALID_RESPONSE) {
                    SessionError.SESSION_REQUIRED
                } else {
                    failure.error
                }
                clearLocked(reason)
            }
        }
    }

    /**
     * Autentica y publica la sesión solo si el intento sigue siendo el vigente.
     *
     * @throws SessionException con el fallo presentable correspondiente.
     */
    suspend fun signIn(username: String, password: String, rememberMe: Boolean) {
        restore()
        val attempt = mutex.withLock {
            if (token != null) throw SessionException(SessionError.SESSION_CHANGED)
            if (cleanupRequired && !cleanupStoreLocked()) throw SessionException(SessionError.STORAGE_UNAVAILABLE)
            ++epoch
            epoch
        }
        val raw = authApi.authenticate(username, password, rememberMe)
        currentCoroutineContext().ensureActive()
        val receivedExpiry = expiry.read(raw)
        mutex.withLock {
            if (attempt != epoch) throw SessionException(SessionError.SESSION_CHANGED)
            if (clock.now() >= receivedExpiry) throw SessionException(SessionError.INVALID_RESPONSE)
            withContext(NonCancellable) {
                try {
                    store.save(raw)
                } catch (failure: SessionException) {
                    clearLocked(failure.error)
                    throw failure
                }
                token = raw
                expiresAt = receivedExpiry
                mutableState.value = SessionState(SessionStatus.SIGNED_IN, epoch)
            }
        }
    }

    /** Cierre exclusivamente local; si la limpieza falla, un login nuevo la reintenta. */
    suspend fun signOut() {
        restore()
        mutex.withLock { clearLocked(null) }
    }

    /** Control al volver al foreground; no existe temporizador de expulsión. */
    suspend fun checkValidity() {
        restore()
        mutex.withLock {
            val currentExpiry = expiresAt ?: return@withLock
            if (clock.now() >= currentExpiry) clearLocked(SessionError.SESSION_REQUIRED)
        }
    }

    /** Copia inmutable para una petición concreta; no se envía un token ya vencido. */
    internal suspend fun snapshot(): SessionSnapshot {
        restore()
        return mutex.withLock {
            val current = token ?: throw SessionException(SessionError.SESSION_REQUIRED)
            val currentExpiry = expiresAt ?: throw SessionException(SessionError.SESSION_REQUIRED)
            if (clock.now() >= currentExpiry) {
                clearLocked(SessionError.SESSION_REQUIRED)
                throw SessionException(SessionError.SESSION_REQUIRED)
            }
            SessionSnapshot(epoch, current)
        }
    }

    /** Falla si la respuesta pertenece a una apertura anterior o a ninguna sesión. */
    internal suspend fun requireCurrent(snapshot: SessionSnapshot) = mutex.withLock {
        if (snapshot.epoch != epoch || token == null) throw SessionException(SessionError.SESSION_CHANGED)
    }

    /** Un 401 tardío no puede cerrar una sesión posterior, ni siquiera de la misma cuenta. */
    internal suspend fun reject(snapshot: SessionSnapshot) = mutex.withLock {
        if (snapshot.epoch != epoch || token == null) throw SessionException(SessionError.SESSION_CHANGED)
        clearLocked(SessionError.SESSION_REQUIRED)
    }

    private suspend fun clearLocked(reason: SessionError?) = withContext(NonCancellable) {
        ++epoch
        token = null
        expiresAt = null
        val cleaned = cleanupStoreLocked()
        mutableState.value = SessionState(
            SessionStatus.SIGNED_OUT,
            epoch,
            if (cleaned) reason else SessionError.STORAGE_UNAVAILABLE,
        )
    }

    private suspend fun cleanupStoreLocked(): Boolean = try {
        store.clear()
        cleanupRequired = false
        true
    } catch (_: SessionException) {
        cleanupRequired = true
        // Si el borrado falla, se invalida el contenido para que un proceso nuevo
        // tampoco pueda restaurar la sesión cerrada desde el token restante.
        try {
            store.save("")
        } catch (_: SessionException) {
            // El fallo se informa; un login nuevo seguirá bloqueado por cleanupRequired.
        }
        false
    }
}
