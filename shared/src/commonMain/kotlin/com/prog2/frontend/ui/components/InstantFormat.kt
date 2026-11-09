package com.prog2.frontend.ui.components

/**
 * Zona horaria con la que el servicio informa las horas locales de atención y con
 * la que la aplicación presenta sus fechas. Los instantes UTC del contrato se
 * convierten a esta misma zona para no mezclar dos convenciones en pantalla.
 */
internal const val ATTENTION_TIME_ZONE = "America/Argentina/Buenos_Aires"

/**
 * Convierte un instante ISO-8601 (`2026-11-09T15:30:00Z`) a `DD/MM/AAAA · HH:mm` en
 * [timeZone]. Devuelve `null` si el valor no es un instante interpretable, de modo
 * que el llamador no invente una hora.
 */
internal expect fun formatInstantInZone(value: String, timeZone: String): String?
