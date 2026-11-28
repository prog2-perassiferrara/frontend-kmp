package com.prog2.frontend.ui.components

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

internal actual fun availabilityDays(start: String, offset: Int, nowMillis: Long): List<AvailabilityDay> {
    val zone = TimeZone.getTimeZone(ATTENTION_TIME_ZONE)
    val calendar = Calendar.getInstance(zone).apply { timeInMillis = nowMillis }
    val iso = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        timeZone = zone
        isLenient = false
    }
    val weekday = SimpleDateFormat("EEE", Locale.forLanguageTag("es-AR")).apply { timeZone = zone }
    val today = iso.format(calendar.time)
    calendar.add(Calendar.DATE, 1)
    val tomorrow = iso.format(calendar.time)
    calendar.timeInMillis = nowMillis
    val parsedStart = iso.parse(start, ParsePosition(0))
    if (parsedStart != null && start >= today && iso.format(parsedStart) == start) {
        calendar.time = parsedStart
    }
    calendar.add(Calendar.DATE, offset)
    if (iso.format(calendar.time) < today) calendar.timeInMillis = nowMillis

    return List(4) {
        val date = iso.format(calendar.time)
        val label = when (date) {
            today -> "Hoy"
            tomorrow -> "Mañana"
            else -> weekday.format(calendar.time).replaceFirstChar { it.uppercaseChar() }
        }
        calendar.add(Calendar.DATE, 1)
        AvailabilityDay(date, label)
    }
}
