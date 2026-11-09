package com.prog2.frontend.session.data

/** Autentica usuarios finales contra Turnos; no conserva credenciales ni sesión. */
interface AuthApi {
    /**
     * @return JWT de usuario; su vigencia se controla en [com.prog2.frontend.session.SessionManager].
     * @throws com.prog2.frontend.session.SessionException ante rechazo o fallo de comunicación.
     */
    suspend fun authenticate(username: String, password: String, rememberMe: Boolean): String
}
