package com.prog2.frontend.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InstantFormatTest {
    @Test
    fun theUtcInstantIsConvertedToTheAttentionZone() {
        assertEquals(
            "09/11/2026 · 12:30",
            formatInstantInZone("2026-11-09T15:30:00Z", ATTENTION_TIME_ZONE),
        )
    }

    @Test
    fun theConversionCanMoveTheDateToThePreviousDay() {
        assertEquals(
            "09/11/2026 · 23:30",
            formatInstantInZone("2026-11-10T02:30:00Z", ATTENTION_TIME_ZONE),
        )
    }

    @Test
    fun fractionsAndAnExplicitOffsetAreAccepted() {
        assertEquals(
            "09/11/2026 · 12:30",
            formatInstantInZone("2026-11-09T15:30:00.123456Z", ATTENTION_TIME_ZONE),
        )
        assertEquals(
            "09/11/2026 · 12:30",
            formatInstantInZone("2026-11-09T15:30:00+00:00", ATTENTION_TIME_ZONE),
        )
    }

    @Test
    fun anUninterpretableValueIsNotInvented() {
        assertNull(formatInstantInZone("sin-fecha", ATTENTION_TIME_ZONE))
        assertNull(formatInstantInZone("2026-11-09", ATTENTION_TIME_ZONE))
    }
}
