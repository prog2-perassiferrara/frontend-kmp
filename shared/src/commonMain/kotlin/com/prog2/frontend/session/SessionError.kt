package com.prog2.frontend.session

/** Fallos presentables sin cuerpos HTTP, credenciales ni detalles internos. */
enum class SessionError(val message: String) {
    INVALID_INPUT("Revisá los datos ingresados."),
    INVALID_CREDENTIALS("Usuario, correo o contraseña incorrectos."),
    UNAVAILABLE("No se pudo conectar con el servicio. Podés volver a intentar."),
    INVALID_RESPONSE("El servicio no devolvió una sesión utilizable. Volvé a intentar."),
    STORAGE_UNAVAILABLE("No se pudo acceder al almacenamiento seguro de la sesión."),
    SESSION_REQUIRED("La sesión venció o dejó de ser válida. Iniciá sesión nuevamente."),
    SESSION_CHANGED("La operación pertenece a una sesión anterior."),
}

/** Propaga un fallo conocido sin conservar una excepción que pueda contener secretos. */
class SessionException(val error: SessionError) : Exception(error.message)
