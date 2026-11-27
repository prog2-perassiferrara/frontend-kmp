package com.prog2.frontend.ui.components

import kotlin.time.Clock

internal data class AvailabilityDay(val date: String, val label: String)

/** Genera cuatro días desde la fecha indicada o desde hoy en la zona de atención. */
internal expect fun availabilityDays(
    start: String,
    offset: Int = 0,
    nowMillis: Long = Clock.System.now().toEpochMilliseconds(),
): List<AvailabilityDay>
