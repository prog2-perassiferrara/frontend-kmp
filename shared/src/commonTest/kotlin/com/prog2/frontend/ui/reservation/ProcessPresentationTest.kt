package com.prog2.frontend.ui.reservation

import com.prog2.frontend.reservation.data.ReservationProcess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProcessPresentationTest {
    @Test
    fun theLocalTimeHidesTheSecondsButKeepsTheInformedValue() {
        assertEquals("12:30", displayTime("12:30:00"))
        assertEquals("12:30", displayTime("12:30"))
    }

    @Test
    fun theExpirationIsShownInTheSameTimeZoneAsTheAppointmentsWithoutTheDeviceClock() {
        assertEquals("09/11/2026 · 12:30", displayExpiration("2026-11-09T15:30:00Z"))
        assertFalse(displayExpiration("2026-11-09T15:30:00Z").contains("UTC"))
        assertEquals("09/11/2026 · 23:30", displayExpiration("2026-11-10T02:30:00Z"))
    }

    @Test
    fun onlyTheConfirmedStateAnnouncesAConfirmedReservation() {
        assertEquals("Reserva confirmada", processView(process("CONFIRMED")).title)
        assertNotEquals("Reserva confirmada", processView(process("WAITING_FOR_PHONE")).title)
        assertEquals("Turno cancelado", processView(process("CANCELLED")).title)
    }

    @Test
    fun theOpenPhasesDistinguishPendingRequestPendingPhoneAndSentPhone() {
        assertEquals(
            "Esperando que se solicite el teléfono",
            processView(process("WAITING_FOR_REQUEST")).title,
        )
        assertEquals("Falta completar información", processView(process("WAITING_FOR_PHONE")).title)
        assertEquals("Teléfono enviado", processView(process("PHONE_QUEUED")).title)
        assertEquals("Reserva confirmada", processView(process("CONFIRMED")).title)

        val sent = processView(process("PHONE_QUEUED"))
        assertFalse(sent.title.contains("Falta"))
        assertFalse(sent.detail.contains("confirmada"))
    }

    @Test
    fun theExpirationIsOnlyShownWhileTheRequestIsOpen() {
        assertEquals(
            "La solicitud vence el 09/11/2026 · 12:30.",
            expirationNotice(process("WAITING_FOR_PHONE", expiresAt = "2026-11-09T15:30:00Z")),
        )
        assertNull(expirationNotice(process("WAITING_FOR_PHONE")))

        // Un proceso finalizado puede conservar `expiresAt` en el DTO.
        listOf("CONFIRMED", "CANCELLED", "FAILED", "EXPIRED").forEach { state ->
            assertNull(expirationNotice(process(state, expiresAt = "2026-11-09T15:30:00Z")))
        }
    }

    @Test
    fun failedExpiredAndCancelledRequestsAreDistinguished() {
        assertEquals("Solicitud fallida", processView(process("FAILED")).title)
        assertEquals("Solicitud vencida", processView(process("EXPIRED")).title)
        assertEquals("Turno cancelado", processView(process("CANCELLED")).title)
    }

    @Test
    fun thePhoneFormIsOnlyAnnouncedWhenTheServiceEnablesIt() {
        assertNull(phoneNotice(process("WAITING_FOR_PHONE", canSubmitPhone = true)))
        assertNull(phoneNotice(process("WAITING_FOR_PHONE")))
        assertNotNull(phoneNotice(process("WAITING_FOR_PHONE", requiredField = "PHONE_NUMBER")))
    }

    @Test
    fun aSentPhoneIsPendingAndNeverConfirmed() {
        val notice = phoneNotice(process("PHONE_QUEUED", requiredField = "PHONE_NUMBER"))
        assertTrue(notice!!.contains("ya fue enviado"))
        assertFalse(notice.contains("confirmada"))
        assertEquals(
            "El teléfono ya fue enviado. Todavía no recibimos la confirmación de la reserva.",
            phoneNotice(process("WAITING_FOR_RESULT", requiredField = "PHONE_NUMBER")),
        )
    }

    private fun process(
        state: String,
        requiredField: String? = null,
        canSubmitPhone: Boolean = false,
        expiresAt: String? = null,
    ) = ReservationProcess(
        id = "b7273764-9bc3-43fd-b42c-8a9ca65c5639",
        professionalId = 201,
        date = "2026-11-09",
        startTime = "12:30:00",
        state = state,
        expiresAt = expiresAt,
        reasonCode = null,
        requiredField = requiredField,
        canSubmitPhone = canSubmitPhone,
        professionalFirstName = null,
        professionalLastName = null,
    )
}
