package com.prog2.frontend.session

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.io.encoding.Base64
import kotlin.time.Instant

/**
 * Extrae solo el vencimiento del JWT. La firma y los permisos pertenecen a los
 * backends; el cliente únicamente evita enviar un token que ya sabe vencido.
 */
class JwtExpiry {
    /** @throws SessionException si el token no permite leer un `exp` utilizable. */
    fun read(raw: String): Instant = try {
        val parts = raw.split('.')
        require(parts.size == 3 && parts.all { it.isNotEmpty() })
        val decoder = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
        val claims = Json.parseToJsonElement(decoder.decode(parts[1]).decodeToString()).jsonObject
        val seconds = claims["exp"]?.jsonPrimitive?.longOrNull ?: throw IllegalArgumentException()
        Instant.fromEpochSeconds(seconds)
    } catch (_: IllegalArgumentException) {
        throw SessionException(SessionError.INVALID_RESPONSE)
    }
}
