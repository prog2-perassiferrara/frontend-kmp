package com.prog2.frontend.ui.reservation

import com.prog2.frontend.reservation.data.CancellationProgress
import com.prog2.frontend.reservation.data.ReservationProcess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MyReservationsPresentationTest {
    @Test
    fun knownStatesAreTranslatedAndUnknownStatesAreShownAsInformed() {
        assertEquals("Confirmada", reservationStateLabel("CONFIRMED"))
        assertEquals("Cancelada", reservationStateLabel("CANCELLED"))
        assertEquals("Fallida", reservationStateLabel("FAILED"))
        assertEquals("Vencida", reservationStateLabel("EXPIRED"))
        assertEquals("WAITING_FOR_PHONE", reservationStateLabel("WAITING_FOR_PHONE"))
    }

    @Test
    fun aMissingHistoricalNameIsReplacedByTheRealIdentifier() {
        assertEquals("Ana Paredes", reservationProfessional(process(professionalFirstName = "Ana")))
        assertEquals("Profesional 218", reservationProfessional(process(professionalFirstName = null)))
    }

    @Test
    fun theScheduleUsesTheDateAndLocalTimeInformedByTurnos() {
        assertEquals("2026-11-10 · 14:00", reservationSchedule(process()))
    }

    @Test
    fun onlyTheReportedCancellationProgressProducesANotice() {
        assertNull(reservationCancellationNotice(process()))
        assertEquals(
            "Cancelación en curso. Actualizá para conocer el resultado.",
            reservationCancellationNotice(
                process(cancellation = CancellationProgress("PENDING", canRetry = false)),
            ),
        )
        assertEquals(
            "La cancelación quedó pendiente de reintento.",
            reservationCancellationNotice(
                process(cancellation = CancellationProgress("RETRY_REQUIRED", canRetry = true)),
            ),
        )
        assertEquals(
            "Turnos confirmó la cancelación.",
            reservationCancellationNotice(process(state = "CANCELLED")),
        )
    }

    private fun process(
        state: String = "CONFIRMED",
        professionalFirstName: String? = "Luciana",
        cancellation: CancellationProgress? = null,
    ) = ReservationProcess(
        id = "0fe8784f-beea-45c5-a886-29b17023f806",
        professionalId = 218,
        date = "2026-11-10",
        startTime = "14:00:00",
        state = state,
        expiresAt = null,
        reasonCode = null,
        requiredField = null,
        canSubmitPhone = false,
        professionalFirstName = professionalFirstName,
        professionalLastName = if (professionalFirstName == null) null else "Paredes",
        cancellation = cancellation,
    )
}
