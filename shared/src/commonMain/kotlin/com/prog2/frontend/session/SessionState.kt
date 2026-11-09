package com.prog2.frontend.session

/** Estado público de la sesión; nunca contiene el bearer ni la contraseña. */
data class SessionState(
    val status: SessionStatus = SessionStatus.RESTORING,
    val epoch: Long = 0,
    val error: SessionError? = null,
)

enum class SessionStatus { RESTORING, SIGNED_OUT, SIGNED_IN }

/**
 * Credencial de una petición concreta. El epoch distingue aperturas de sesión
 * aunque pertenezcan a la misma cuenta.
 */
internal class SessionSnapshot(val epoch: Long, val token: String) {
    override fun toString(): String = "SessionSnapshot(epoch=$epoch, token=REDACTED)"
}
