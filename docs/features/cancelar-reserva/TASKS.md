# TASKS: Cancelar reserva propia

**SPEC:** [SPEC.md](SPEC.md) · **PLAN:** [PLAN.md](PLAN.md) · **CU:** [CU-10](../../use-cases/CU-10-cancelar-reserva.md) · **Issue:** #12
**Estado:** Implementada y verificada con pruebas y una cancelación real controlada (2026-11-09).

## Coherencia previa

Cada RF tiene CA y validación en el PLAN; las tareas no agregan alcance. Sin decisiones bloqueantes.

## Tareas

- [x] **T1 · Contrato y errores** (RF-03, RF-05)
  - `ReservationsApi.cancel(processId)`, POST sin cuerpo en `AuthenticatedApiClient` y mapeo de `404`, `409`, `403`, `5xx` y transporte en `KtorReservationsApi`.
  - Validación: `KtorReservationsApiTest.cancelSendsNoBodyAndAcceptsBothSuccessCodes`, `cancelMapsKnownRejectionsAndUncertainOutcomes` y `cancelTransportFailureIsUncertainAndANonUuidIsNotSent`.
- [x] **T2 · Estado, confirmación y bloqueo** (RF-01, RF-02, RF-04, RF-06, RF-07)
  - `MyReservationsViewModel`: diálogo, una sola operación, actualización de la fila, bloqueo ante incertidumbre y reconciliación con lectura.
  - Validación: `MyReservationsViewModelTest` (13 pruebas al cierre, ocho de cancelación; incluye la regresión de I-1 `aRejectedCancellationWhoseReadLosesTheSessionKeepsTheBlockWithoutEscaping`).
- [x] **T3 · Diálogo y presentación** (RF-02, RF-04)
  - `MyReservationsScreen` muestra la confirmación sin motivo y el progreso informado; `reservationCancellationNotice` distingue `PENDING`, `RETRY_REQUIRED` y `CANCELLED`.
  - Validación: `MyReservationsPresentationTest.onlyTheReportedCancellationProgressProducesANotice`.
- [x] **T4 · Verificación integral** (CA-01–CA-07)
  - Suite host, build, lint y apertura/cierre del diálogo en emulador sin confirmar. La cancelación real se agregó después, en la prueba controlada documentada abajo.

## Criterios de aceptación

| CA | Estado | Evidencia |
| --- | --- | --- |
| CA-01 | Verificado | `cancellationIsOnlyOfferedForConfirmedReservationsWithoutPendingCancellation`: `CONFIRMED` sí; `PENDING` y `CANCELLED` no; `RETRY_REQUIRED` sí. |
| CA-02 | Verificado | `confirmingCancellationSendsOneCallAndUpdatesTheRow` confirma que el diálogo y “Volver” no envían; en emulador se abrió y se cerró con “Volver” sin POST. |
| CA-03 | Verificado | `KtorReservationsApiTest` comprueba cuerpo vacío y ruta; `confirmingCancellationSendsOneCallAndUpdatesTheRow` pulsa dos veces y registra una sola llamada. |
| CA-04 | Verificado | `confirmingCancellationSendsOneCallAndUpdatesTheRow` (`200 CANCELLED`) y `aPendingCancellationIsShownWithoutDeclaringItCancelled` (`202 PENDING`); cancelación real de 1677 con `CANCELLED` informado en la misma respuesta. |
| CA-05 | Verificado | `KtorReservationsApiTest.cancelMapsKnownRejectionsAndUncertainOutcomes` y `cancelTransportFailureIsUncertainAndANonUuidIsNotSent`; `MyReservationsViewModelTest.aRejectedCancellationKeepsTheReservationAndRefreshesTheList` y `aRejectedCancellationWhoseReadFailsBlocksUntilVerified`. |
| CA-06 | Verificado | `anUncertainCancellationBlocksUntilAReadVerifiesTheReservation` y `anUncertainCancellationThatReadsAPendingProgressIsNotOfferedAgain`. |
| CA-07 | Verificado | `aPendingCancellationIsShownWithoutDeclaringItCancelled`, el rechazo reconciliado y la cancelación real de 1677 seguida de “Actualizar”, “Volver” y reapertura. |

### Pruebas ejecutadas (2026-11-09)

- `./gradlew :shared:testAndroidHostTest`: 155 pruebas, 0 fallos (incluye la regresión de I-1 sobre la reconciliación de una cancelación rechazada).
- `./gradlew :androidApp:assembleDebug :androidApp:lintDebug`: `BUILD SUCCESSFUL`; avisos de lint preexistentes.

### Verificación en emulador (2026-11-09)

- La reserva real `CONFIRMED` (proceso `0fe8784f-beea-45c5-a886-29b17023f806`, profesional 218) mostró “Cancelar reserva”.
- El diálogo mostró “Profesional 218”, `2026-11-10 · 14:00` y “Turnos confirmará si la cancelación se concretó”.
- Se cerró con “Volver”. Un `GET /api/reservations` posterior devolvió la misma reserva `CONFIRMED` con `cancellation=null`: **no se envió ningún POST**.
- No se crearon ni cancelaron reservas reales.

### Intento previo con la reserva 1675 (2026-11-09)

- Cuenta `cu04patient` (owner 35) confirmada desde el listado real del emulador; Turnos y MySQL respondían y la fila de `0fe8784f-…` pertenece a esa cuenta.
- La reserva ficticia **1675** (proceso `0fe8784f-beea-45c5-a886-29b17023f806`, profesional 218, `2026-11-10 14:00`) **ya estaba cancelada**: el `GET /api/reservations` devolvió `state=CANCELLED` y `cancellation=null`, y una consulta de solo lectura confirmó `reservation_id=1675`, `owner_id=35` y `closed_at=2026-11-09 04:18:23`.
- No se cumplía la precondición de la prueba (seguir `CONFIRMED` y sin cancelación completada), por lo que se detuvo sin elegir otra reserva.
- **No se envió ningún POST de cancelación.** En “Mis reservas” la fila 1675 se mostró como “Cancelada” con “Turnos confirmó la cancelación.” y sin “Cancelar reserva”.

### Cancelación real controlada de la reserva 1677 (2026-11-09)

- Cuenta `cu04patient` (owner 35). Antes del POST, `GET /api/reservations` devolvió para el proceso `514462fa-72c1-430e-be24-678cc1a41200` `state=CONFIRMED` y `cancellation=null`; la consulta de solo lectura confirmó `reservation_id=1677`, `owner_id=35` y `phase=CONFIRMED`.
- “Mis reservas” mostró “Profesional 218 · 2026-11-10 · 15:30” con “Cancelar reserva”. El diálogo identificó la misma reserva y se confirmó **una sola vez** a las `04:36:16 UTC`.
- La app mostró de inmediato “Turnos confirmó la cancelación.” y la fila pasó a “Cancelada” sin ofrecer otra cancelación. Por ese mensaje y por la transición registrada (`closed_at=04:36:17.525`), la respuesta se interpreta como `200 CANCELLED`; el código HTTP no se capturó porque la app no registra tráfico.
- “Actualizar” releyó el listado: `Página 1 de 1 · 6 reserva(s)`, sin pendiente, con 1677 `CANCELLED` y sin botones de cancelación. “Volver” regresó a la búsqueda y la reapertura de “Mis reservas” conservó el estado.
- **Un solo POST** desde la app; no hubo reintento automático ni manual. No se crearon reservas, holds ni procesos durante la prueba.
- El proceso de Pablo Suarez (`21857e19-…`) no fue tocado: pasó a `FAILED` (`CENTRAL_PROCESS_FAILED`, `reservation_id=1678`, `closed_at=04:35:56.718`) **antes** del POST, por el vencimiento de su hold (`expires_at=04:35:44.788`). Tras “Actualizar” apareció en el historial como “Fallida”.

### Límites

- La cancelación real se ejecutó una sola vez sobre 1677 y Turnos la completó. El caso `202 PENDING`, el rechazo y la incertidumbre siguen cubiertos con dobles y pruebas de contrato, no con tráfico real.
- El código HTTP del POST no se capturó; se infiere de la rama de presentación y del estado persistido. Los `GET` de consulta sí respondieron `200`.
- El bloqueo tras un resultado incierto y su desbloqueo solo con lectura exitosa no se reprodujeron contra la red real.
- El banner de proceso pendiente de CU-08 conservó el texto anterior al volver a la búsqueda, aunque Turnos ya había marcado ese proceso como fallido; es un comportamiento preexistente fuera de CU-09/10.
- Sin cambios en `backend-turnos` ni en CU-01/CU-08.
