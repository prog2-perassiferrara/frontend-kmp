package com.prog2.frontend.registration

/** Fallos presentables del alta; no conservan cuerpos HTTP, credenciales ni detalles internos. */
enum class RegistrationError(val message: String) {
    INVALID_INPUT("Revisá los datos ingresados."),
    IDENTIFIER_ALREADY_USED("El usuario o el correo ya están registrados. Probá con otros datos o iniciá sesión."),
    UNCERTAIN("No pudimos confirmar si la cuenta se creó. Iniciá sesión para verificarlo antes de volver a intentarlo."),
}

/**
 * Propaga un fallo conocido del alta sin conservar la respuesta original.
 *
 * @property fieldErrors nombres de campo observados en `fieldErrors`, disponible
 *   solo para [RegistrationError.INVALID_INPUT]; el código del backend no se usa.
 */
class RegistrationException(
    val error: RegistrationError,
    val fieldErrors: Set<String> = emptySet(),
) : Exception(error.message)
