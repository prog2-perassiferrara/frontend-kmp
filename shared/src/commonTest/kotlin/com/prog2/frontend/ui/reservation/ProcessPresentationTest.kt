package com.prog2.frontend.ui.reservation

import com.prog2.frontend.reservation.data.ReservationProcess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ProcessPresentationTest {
    @Test
    fun theLocalTimeHidesTheSecondsButKeepsTheInformedValue() {
        assertEquals("12:30", displayTime("12:30:00"))
        assertEquals("12:30", displayTime("12:30"))
    }

    @Test
    fun theExpirationIsShownAsUtcWithoutUsingTheDeviceClock() {
        assertEquals("2026-11-09 15:30:00 UTC", displayUtc("2026-11-09T15:30:00Z"))
        assertTrue(displayUtc("2026-11-09T15:30:00Z").endsWith(" UTC"))
    }

    @Test
    fun onlyTheConfirmedStateAnnouncesAConfirmedReservation() {
        assertEquals("Reserva confirmada", processView(process("CONFIRMED")).title)
        assertNotEquals("Reserva confirmada", processView(process("WAITING_FOR_PHONE")).title)
        assertEquals("Proceso finalizado", processView(process("CANCELLED")).title)
    }

    private fun process(state: String) = ReservationProcess(
        id = "b7273764-9bc3-43fd-b42c-8a9ca65c5639",
        professionalId = 201,
        date = "2026-11-09",
        startTime = "12:30:00",
        state = state,
        expiresAt = null,
        reasonCode = null,
        requiredField = null,
        canSubmitPhone = false,
        professionalFirstName = null,
        professionalLastName = null,
    )
}
