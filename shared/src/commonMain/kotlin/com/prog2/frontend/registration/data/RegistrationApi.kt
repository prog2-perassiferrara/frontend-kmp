package com.prog2.frontend.registration.data

import com.prog2.frontend.registration.RegistrationException

/**
 * Datos obligatorios del alta. `langKey` fija el idioma por defecto del contrato.
 * La contraseña solo vive en memoria y nunca se incluye en representaciones de texto.
 */
data class NewAccount(
    val login: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val langKey: String = "es",
) {
    override fun toString(): String = "NewAccount(login=$login, credentials=REDACTED)"
}

/** Contrato de alta pública; no requiere bearer ni devuelve sesión. */
interface RegistrationApi {
    /** @throws RegistrationException con el resultado presentable del alta. */
    suspend fun register(account: NewAccount)
}
