# TASKS: Completar el teléfono solicitado

**SPEC:** [SPEC.md](SPEC.md) · **PLAN:** [PLAN.md](PLAN.md) · **CU:** [CU-07](../../use-cases/CU-07-completar-informacion.md) · **Issue:** #10
**Estado:** Implementada y verificada con pruebas y con un recorrido real en emulador (2026-11-09).

## Coherencia previa

Cada RF tiene CA y validación en el PLAN; cada tarea referencia sus RF/CA y no agrega alcance. Sin decisiones bloqueantes.

## Tareas

- [x] **T1 · Contrato, DTO y errores** (RF-03, RF-05, RF-06)
  - `ReservationsApi.SubmitPhone`, `submitPhone`, `KtorReservationsApi` y mapeo de `INVALID_PHONE_NUMBER`, `PHONE_NOT_REQUESTED`, `PHONE_SUBMISSION_ACTIVE`, `PHONE_WINDOW_CLOSED`, `REQUEST_ID_CONFLICT`, `RESERVATION_PROCESS_NOT_FOUND` y `503`.
  - Validación: `KtorReservationsApiTest` (15 pruebas, todas en verde).
- [x] **T2 · Estado, validación y envío único** (RF-01, RF-02, RF-04, RF-06, RF-09)
  - `PhoneStatus`, `phoneNumber`, `phoneMessage` y `canSubmitPhone` derivado en `ReservationUiState`; `changePhone`, `submitPhone`, `verifyPhone` y normalización en `ReservationViewModel`.
  - Validación: `ReservationViewModelTest` (31 pruebas, todas en verde).
- [x] **T3 · Formulario y cableado** (RF-01, RF-04, RF-05, RF-07, RF-08)
  - `ProcessScreen` con formulario condicionado, avisos de envío y verificación; `phoneNotice` en `ProcessPresentation`; acciones en `App`.
  - Validación: `ProcessPresentationTest` (5 pruebas, todas en verde), build y lint.
- [x] **T4 · Verificación integral** (CA-01–CA-09)
  - Suite host, build y lint; recorrido real en emulador con una cuenta ficticia, un inicio de reserva y un envío de teléfono.
  - El recorrido real se detalla en «Prueba de integración real».

## Criterios de aceptación

| CA | Estado | Evidencia |
| --- | --- | --- |
| CA-01 | Verificado | `ReservationViewModelTest.thePhoneRequirementAloneDoesNotEnableTheSubmission` y `thePhoneFormOpensOnlyWhenTurnosEnablesTheSubmission`; `ProcessPresentationTest.thePhoneFormIsOnlyAnnouncedWhenTurnosEnablesIt`. |
| CA-02 | Verificado | `ReservationViewModelTest.anInvalidPhoneIsNotSentAndCanBeCorrected`; `submittingThePhoneSendsItOnceNormalizedAndShowsTheReportedProcess` comprueba `+5491155551234`. |
| CA-03 | Verificado | `KtorReservationsApiTest.phoneSendsTheContractBodyAndBearerAndAcceptsBothSuccessCodes`: método, ruta, bearer y cuerpo exactos para `202` y `200`. |
| CA-04 | Verificado | `ReservationViewModelTest.aSecondPhoneSubmitWhileTheRequestIsInFlightDoesNotSendAnotherOne`. |
| CA-05 | Verificado | `KtorReservationsApiTest.phoneMapsKnownRejectionsAndUncertainOutcomes` y `aTransportFailureOnPhoneIsUncertain`; `ReservationViewModelTest.aRejectedPhoneIsReconciledAndAllowsCorrection` y `aRejectedPhoneWhoseReadFailsBlocksResendingUntilVerified`. |
| CA-06 | Verificado | `KtorReservationsApiTest` acepta ambos éxitos; `ReservationViewModelTest` contrasta el rechazo con `GET`, bloquea el reenvío tras `404` o lectura fallida y exige verificación antes de repetir un envío incierto (`aNotFoundPhoneBlocksResendingUntilTheStateIsVerified`, `aRejectedPhoneWhoseReadFailsBlocksResendingUntilVerified`). |
| CA-07 | Verificado | `ProcessPresentationTest.aSentPhoneIsPendingAndNeverConfirmed`; `ReservationViewModelTest.submittingThePhoneSendsItOnceNormalizedAndShowsTheReportedProcess`. |
| CA-08 | Verificado | `thePhoneRequirementAloneDoesNotEnableTheSubmission` y `ProcessPresentationTest.thePhoneFormIsOnlyAnnouncedWhenTurnosEnablesIt`; en emulador el proceso real ya finalizado no se ofreció como pendiente. |
| CA-09 | Verificado | `ReservationViewModelTest.aLatePhoneResponseAfterResetDoesNotPublishState`; el estado del teléfono solo vive en el ViewModel y `reset()` lo descarta. |

### Pruebas ejecutadas (2026-11-09)

- `./gradlew :shared:testAndroidHostTest`: 130 pruebas, 0 fallos. Incluye 15 de contrato de reservas, 31 del ViewModel y 5 de presentación.
- `./gradlew :androidApp:assembleDebug :androidApp:lintDebug`: `BUILD SUCCESSFUL`; 5 avisos de lint preexistentes (`AndroidGradlePluginVersion`, `NewerVersionAvailable`, `ObsoleteSdkInt`).

### Prueba de integración real (2026-11-09)

- Cuenta ficticia propia `cu04patient` (id 35, sin datos personales) en el emulador; Catálogo, Turnos y la API central respondían `200` y Kafka era alcanzable.
- **Un** `POST /api/reservation-processes` desde la app a las 03:54:35Z para Romina Acosta (218), 2026-11-10 14:00. Proceso `0fe8784f-beea-45c5-a886-29b17023f806` en `WAITING_FOR_PHONE`, con hold `hold-cbeaf36a-…` y `expiresAt 04:04:35.867969Z`.
- `GET /api/reservations` `200` a las 03:54:59Z: `pending` con `state=WAITING_FOR_PHONE`, `requiredField=PHONE_NUMBER` y `canSubmitPhone=true`. La app mostró el formulario solo después de «Actualizar».
- **Un** `POST /api/reservation-processes/{id}/phone` con `+5491100000000` a las 03:55:10Z. La app mostró «El teléfono ya fue enviado. Turnos todavía no confirmó la reserva.» y retiró el formulario.
- La consulta SQL confirmó **una sola** fila en `reservation_phone_submission` para el proceso (`615f7faf-…`, `occurred_at 03:55:10.571323`): no hubo repetición del POST. La primera publicación quedó `PHONE_PUBLICATION_UNCERTAIN` con reintento interno (`cycle_attempt=2`); se publicó a las 03:55:25.539Z y el proceso cerró `CONFIRMED` a las 03:55:26.938Z con `reservation_id=1675`.
- Tras «Actualizar», la primera lectura falló con «No pudimos consultar el proceso en Turnos»; «Reintentar» mostró «Reserva confirmada». `GET /api/reservation-processes/{id}` respondió `200 CONFIRMED` y `GET /api/reservations` `200` con `pending=null`; no se repitió ningún POST.
- «Volver» regresó a la búsqueda; force-stop y reapertura conservaron la sesión, no mostraron banner ni formulario y no alteraron la fila de envío ni la cantidad de procesos.

### Límites

- Los estados HTTP del inicio y del teléfono se infieren del contrato (`202` para una nueva intención y para la aceptación, `200` para una repetición equivalente) y de sus efectos en Turnos; el cuerpo HTTP no se capturó porque la app no registra tráfico y no se instrumentó la red.
- No se retomó el proceso mientras seguía pendiente: Turnos lo confirmó 16 s después del envío. La salida, el regreso y la recuperación se comprobaron después del cierre y con el `GET` exitoso.
- Un fallo transitorio de lectura tras la confirmación requirió «Reintentar»; no se reprodujo ni se atribuyó a un defecto del envío.
- Sin cambios de código en `backend-turnos` ni en los CU-06/CU-08 ya cerrados.
