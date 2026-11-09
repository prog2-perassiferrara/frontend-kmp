package com.prog2.frontend.ui.components

import java.util.Calendar
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidDatePickerModalTest {
    @Test
    fun minimumUsesTheAttentionDateEvenWhenTheDeviceHasAnotherDay() {
        val original = TimeZone.getDefault()
        try {
            // 10 de noviembre en Tokio, todavía 9 de noviembre en Buenos Aires.
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
            val now = java.time.Instant.parse("2026-11-09T15:30:00Z").toEpochMilli()
            val minimum = Calendar.getInstance().apply { timeInMillis = startOfToday(now) }
            assertEquals(2026, minimum.get(Calendar.YEAR))
            assertEquals(Calendar.NOVEMBER, minimum.get(Calendar.MONTH))
            assertEquals(9, minimum.get(Calendar.DAY_OF_MONTH))
            assertEquals(0, minimum.get(Calendar.HOUR_OF_DAY))

            // 9 de noviembre en Los Ángeles, ya 10 en Buenos Aires.
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            val later = java.time.Instant.parse("2026-11-10T03:30:00Z").toEpochMilli()
            val nextMinimum = Calendar.getInstance().apply { timeInMillis = startOfToday(later) }
            assertEquals(10, nextMinimum.get(Calendar.DAY_OF_MONTH))
            assertEquals(0, nextMinimum.get(Calendar.HOUR_OF_DAY))
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
