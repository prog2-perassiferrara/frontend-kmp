package com.prog2.frontend.ui.reservation

import com.prog2.frontend.reservation.data.ReservationProcess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
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
        assertEquals("Reserva confirmada", processStatusLabel(process("CONFIRMED")))
        assertNotEquals("Reserva confirmada", processStatusLabel(process("WAITING_FOR_PHONE")))
        assertEquals("Turno cancelado", processStatusLabel(process("CANCELLED")))
    }

    @Test
    fun theOpenPhasesDistinguishPendingRequestPendingPhoneAndSentPhone() {
        assertEquals("Solicitud pendiente", processStatusLabel(process("WAITING_FOR_REQUEST")))
        assertEquals("Completá tu teléfono", processStatusLabel(process("WAITING_FOR_PHONE", canSubmitPhone = true)))
        assertEquals("Teléfono enviado", processStatusLabel(process("PHONE_QUEUED")))
    }

    @Test
    fun theExpirationIsOnlyShownWhileTheRequestIsOpen() {
        assertEquals(
            "Vence el 09/11/2026 · 12:30.",
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
        assertEquals("Solicitud fallida", processStatusLabel(process("FAILED")))
        assertEquals("Solicitud vencida", processStatusLabel(process("EXPIRED")))
        assertEquals("Turno cancelado", processStatusLabel(process("CANCELLED")))
    }

    @Test
    fun thePhonePromptAppearsOnlyWhenTurnosEnablesSending() {
        assertEquals("Completá tu teléfono", processStatusLabel(process("WAITING_FOR_PHONE", canSubmitPhone = true)))
        assertEquals(
            "Envío de teléfono no disponible",
            processStatusLabel(process("WAITING_FOR_PHONE", requiredField = "PHONE_NUMBER")),
        )
        assertEquals("Teléfono rechazado.", processReason("PHONE_REJECTED"))
    }

    @Test
    fun aSentPhoneIsPendingAndNeverConfirmed() {
        val label = processStatusLabel(process("WAITING_FOR_RESULT"))
        assertTrue(label.startsWith("Teléfono enviado"))
        assertFalse(label.contains("confirmada"))
        assertEquals("Esperando el resultado", processStatusLabel(process("RECONCILIATION_REQUIRED")))
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
