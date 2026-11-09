package com.prog2.frontend.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val INSTANT_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX"
private const val DISPLAY_PATTERN = "dd/MM/yyyy · HH:mm"

/**
 * Implementación Android con las mismas clases de fecha que ya usa el resto de la
 * aplicación (`SimpleDateFormat`), disponibles desde la API mínima del proyecto.
 */
internal actual fun formatInstantInZone(value: String, timeZone: String): String? {
    val instant = parseInstant(value) ?: return null
    return SimpleDateFormat(DISPLAY_PATTERN, Locale.US)
        .apply { this.timeZone = TimeZone.getTimeZone(timeZone) }
        .format(instant)
}

/** Acepta el instante con `Z` u offset y descarta fracciones de segundo opcionales. */
private fun parseInstant(value: String): Date? = runCatching {
    SimpleDateFormat(INSTANT_PATTERN, Locale.US)
        .apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        .parse(value.replace(FRACTION_SECONDS, ""))
}.getOrNull()

private val FRACTION_SECONDS = Regex("\\.\\d+")
