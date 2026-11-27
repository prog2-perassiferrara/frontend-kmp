package com.prog2.frontend.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class DatePresentationTest {
    @Test
    fun theDisplayedDateIsReadableWithoutChangingTheInternalValue() {
        assertEquals("10/11/2026", displayDate("2026-11-10"))
        assertEquals("09/01/2026", displayDate("2026-01-09"))
        assertEquals("sin-fecha", displayDate("sin-fecha"))
    }
}
