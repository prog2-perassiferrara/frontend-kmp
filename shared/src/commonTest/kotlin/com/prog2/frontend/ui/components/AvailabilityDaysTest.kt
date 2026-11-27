package com.prog2.frontend.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class AvailabilityDaysTest {
    private val now = Instant.parse("2026-12-01T02:00:00Z").toEpochMilliseconds()

    @Test
    fun theFirstRowUsesTheAttentionDateAndCrossesTheMonthBoundary() {
        val days = availabilityDays("", nowMillis = now)

        assertEquals(listOf("2026-11-30", "2026-12-01", "2026-12-02", "2026-12-03"), days.map { it.date })
        assertEquals(listOf("Hoy", "Mañana"), days.take(2).map { it.label })
        assertEquals("2026-12-04", availabilityDays(days.first().date, 4, now).first().date)
    }

    @Test
    fun aValidSearchDatePrefillsTheRowAndBackwardPagingStopsAtToday() {
        assertEquals("2026-12-02", availabilityDays("2026-12-02", nowMillis = now).first().date)
        assertEquals("2026-11-30", availabilityDays("2026-12-02", -4, now).first().date)
        assertEquals("2026-11-30", availabilityDays("2026-12-32", nowMillis = now).first().date)
    }
}
