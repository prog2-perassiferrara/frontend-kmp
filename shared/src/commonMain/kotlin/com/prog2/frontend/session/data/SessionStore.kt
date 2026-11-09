package com.prog2.frontend.session.data

/** Conserva exclusivamente el JWT de usuario mediante el almacenamiento protegido de la plataforma. */
interface SessionStore {
    /**
     * @return JWT conservado, o null cuando no existe una sesión local.
     * @throws com.prog2.frontend.session.SessionException si no puede leerse de forma segura.
     */
    suspend fun read(): String?

    /**
     * Guarda el JWT antes de publicar el acceso; nunca recibe la contraseña.
     * @throws com.prog2.frontend.session.SessionException si no puede completarse la escritura protegida.
     */
    suspend fun save(token: String)

    /**
     * Invalida el almacenamiento anterior; el cierre no depende de la red.
     * @throws com.prog2.frontend.session.SessionException si queda pendiente la limpieza local.
     */
    suspend fun clear()
}
