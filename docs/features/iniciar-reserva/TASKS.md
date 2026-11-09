# TASKS: Iniciar una reserva

**SPEC:** [SPEC.md](SPEC.md) · **PLAN:** [PLAN.md](PLAN.md) · **CU:** [CU-06](../../use-cases/CU-06-iniciar-reserva.md) · **Issue:** #9
**Estado:** Implementada y verificada con un proceso y un hold reales el 2026-11-09; pendiente de teléfono, sin confirmar.

## Coherencia previa

Cada RF tiene CA y validación en el PLAN; cada tarea referencia sus RF/CA y no agrega alcance. Sin decisiones bloqueantes.

## Tareas

- [x] **T1 · Cliente POST y contrato** (RF-01, RF-03, RF-04)
  - `AuthenticatedApiClient.post`, `ReservationsApi`, `KtorReservationsApi`, `ReservationFailure` y modelos del proceso.
  - Validación: `KtorReservationsApiTest` (11 pruebas, todas en verde).
- [x] **T2 · Inicio, rechazos e incertidumbre** (RF-02–RF-08)
  - `ReservationUiState`, `ReservationViewModel.start`, `verify`, `retryStart`, `dismiss` y cancelación de respuestas tardías.
  - Validación: `ReservationViewModelTest` (21 pruebas, todas en verde).
- [x] **T3 · Pantalla y cableado** (RF-09, RF-10)
  - `ProcessScreen`, botón “Solicitar reserva”, `ReservationBanner`, `App`, `AppContainer` y `MainActivity`.
  - Validación: build, lint y recorrido de solo lectura.
- [x] **T4 · Verificación integral** (CA-01–CA-10)
  - Suite host, build, lint y recorrido real CU-05 → CU-06 con un único `POST` el 2026-11-09.
  - Proceso creado con hold real y fase `WAITING_FOR_PHONE`; no se envió teléfono ni se canceló.

## Cierre

### CA y evidencia

| CA | Estado | Evidencia |
| --- | --- | --- |
| CA-01 | Verificado | `KtorReservationsApiTest.startSendsTheContractBodyAndBearer`: método, ruta, bearer y cuerpo exactos. |
| CA-02 | Verificado | `ReservationViewModelTest.aSecondStartWhileTheRequestIsInFlightDoesNotSendAnotherOne`: una sola llamada. |
| CA-03 | Verificado | `KtorReservationsApiTest.aTwoHundredIsAcceptedAsTheSameProcess` y `ProcessPresentationTest.onlyTheConfirmedStateAnnouncesAConfirmedReservation`: la fase recibida se muestra sin confirmación implícita. |
| CA-04 | Verificado | `KtorReservationsApiTest.startSendsTheContractBodyAndBearer` compara el `requestId`; `ReservationViewModelTest.startSendsTheSelectionOnceAndShowsTheReportedProcess` lo conserva antes y después de la respuesta. |
| CA-05 | Verificado | `KtorReservationsApiTest.mapsKnownRejectionsAndKeepsTheProcessWithoutRetrying` (400/409/403/503/500) y `ReservationViewModelTest.aKnownRejectionIsPresentedWithoutRepeatingTheStart`. |
| CA-06 | Verificado | `ReservationViewModelTest.anErrorWithAProcessIdLoadsThatProcessInsteadOfStartingAnother`; si el `GET` falla no se afirma rechazo (`anActiveProcessWhoseReadFailsRemainsVerifiable`). |
| CA-07 | Verificado | `KtorReservationsApiTest.aTransportFailureOnStartIsUncertain` y `ReservationViewModelTest.anUncertainStartIsNotPresentedAsSuccessNorAsFailure`. |
| CA-08 | Verificado | `ReservationViewModelTest.verifyingAnUnregisteredStartAllowsRetryWithTheSameRequestId`; un 400/403 de verificación no habilita el reintento (`KtorReservationsApiTest.onlyAnAuthenticNotFoundEnablesRetryingTheStart`, `ReservationViewModelTest.aFailedVerificationKeepsTheUncertaintyAndDoesNotEnableAnotherStart`). |
| CA-09 | Verificado | `ReservationViewModelTest.aPendingProcessHiddenByTheUserRemainsAvailableThroughTheBanner` y `dismissDiscardsFinalRejectedAndUnregisteredProcesses`; en emulador, “Volver” mostró el banner “Tenés un proceso de reserva en curso” y “Ver estado” reabrió el proceso con un `GET` actualizado. |
| CA-10 | Verificado | `ReservationViewModelTest.thePhoneRequirementIsOnlyAnnounced`; `ProcessScreen` solo muestra el aviso, sin campo ni envío (build/lint). |

### Regresión

- `./gradlew :shared:testAndroidHostTest`: 114 pruebas, 0 fallos (incluye 11 de contrato, 21 del ViewModel y 3 de presentación).
- `./gradlew :androidApp:assembleDebug :androidApp:lintDebug`: BUILD SUCCESSFUL; 5 avisos de lint preexistentes (versión de AGP, versión disponible y `ObsoleteSdkInt`), ninguno en el código nuevo.
- Simplificación verificada: salir durante una actualización no bloquea el siguiente intento porque la visibilidad vive en `App` y la guarda `loading` se libera al terminar o al `reset`. “Ver estado” consulta Turnos (`refresh`/`verify`), cubierto por `aPendingProcessHiddenByTheUserRemainsAvailableThroughTheBanner`.
- Invalidación por cancelación: `aLateStartResponseAfterResetDoesNotPublishState`, `aLateRefreshResponseAfterResetDoesNotPublishState` y `aResponseFromAPreviousOperationDoesNotReplaceTheNewerOne`.

### Prueba real de CU-06/CU-08 (2026-11-09)

- Catálogo y Turnos respondieron `200` en health. La disponibilidad de Romina Acosta (id 218) ofreció `13:30` a `15:30` para `2026-11-10`; se eligió `13:30`.
- La sesión restaurada en el emulador pertenecía a una cuenta local de prueba (id 33), no a la cuenta documentada de CU-04. No se cambió de cuenta, no se crearon usuarios y no se introdujeron credenciales; la titularidad de esa cuenta no está confirmada.
- Un único `POST /api/reservation-processes` con `professionalId=218`, `date=2026-11-10` y `startTime=13:30:00`; los datos coinciden con la fila creada (consulta de solo lectura). El código HTTP exacto del `POST` no fue capturado y no se observó el `202`. La creación de una sola fila descarta una repetición equivalente. Se creó un solo proceso en toda la prueba.
- El proceso `09111d89-…` quedó con `hold_id` `hold-eb4fa650-…`, fase `WAITING_FOR_PHONE` y vencimiento `2026-11-09T03:28:39Z` (10 minutos desde el alta). La pantalla mostró “Falta completar información”, el vencimiento UTC y “La reserva sigue en curso…”; no afirmó confirmación.
- La respuesta inmediata del `POST` no incluyó el requisito de teléfono y la pantalla no lo inventó. Un `GET` de actualización falló sin código HTTP registrado y conservó el proceso con “Reintentar”; el siguiente `GET` devolvió el estado y habilitó el aviso de teléfono. No se envió ningún teléfono.
- Estado final: pendiente `WAITING_FOR_PHONE` con hold vigente y sin `reservation_id`; no confirmado, rechazado ni vencido durante la prueba. No se esperó el vencimiento del hold.
- Aislamiento entre cuentas: un `GET` del mismo `processId` con otra cuenta respondió `404 RESERVATION_PROCESS_NOT_FOUND`; no se expuso el proceso ajeno.
- No se detectaron defectos funcionales en el cliente: la UI reflejó el estado recibido, conservó el proceso ante el fallo de lectura y permitió reintentar sin repetir el `POST`.

### Límites

- No se repitió el `POST`, no se envió teléfono, no se confirmó ni canceló la reserva y no se modificaron datos de otros usuarios.
- No se ejecutó CU-07 (envío de teléfono) ni CU-10 (cancelación); no hay reserva confirmada.
- El código HTTP exacto del `POST` y el del primer `GET` fallido no quedaron registrados. No se afirma haber observado `202`.

### Desviaciones del PLAN

- `ReservationError` (12 valores) pasó a `ReservationFailure` (3 clasificaciones) con mensaje en la excepción. Se eliminaron `StartedReservation.replayed`, `ReservationCancellation`, `warningCode`, `phoneRequestId`, las etapas `Started/Idle/...` y los campos `visible`, `notice`, `error`, `retryable`, `isRefreshing`, `processId`, `discoveryFailure` y `discoveryRetryable`.
- La visibilidad, el banner y la acción “Ver estado” viven en `App`; `ReservationViewModel.dismiss()` solo decide qué estado se conserva. No cambian RF, CA ni contratos.
