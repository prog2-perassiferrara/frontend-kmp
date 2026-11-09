# TASKS: Consultar y retomar el proceso propio

**SPEC:** [SPEC.md](SPEC.md) · **PLAN:** [PLAN.md](PLAN.md) · **CU:** [CU-08](../../use-cases/CU-08-consultar-proceso.md) · **Issue:** #11
**Estado:** Implementada y verificada con un pendiente real el 2026-11-09; teléfono y cancelación fuera de alcance.

## Coherencia previa

Cada RF tiene CA y validación en el PLAN; cada tarea referencia sus RF/CA y no agrega alcance. Sin decisiones bloqueantes.

## Tareas

- [x] **T1 · Contrato de consulta y pendiente** (RF-01, RF-06)
  - `ReservationsApi.process`, `ReservationsApi.pending` y su consumo en `KtorReservationsApi`.
  - Validación: `KtorReservationsApiTest` (11 pruebas, todas en verde).
- [x] **T2 · Descubrimiento y actualización** (RF-05–RF-09)
  - `ReservationViewModel.discover`, `retryDiscovery`, `refresh`, `verify`, `dismiss`, `reset` y cancelación de operaciones anteriores.
  - Validación: `ReservationViewModelTest` (21 pruebas, todas en verde).
- [x] **T3 · Pantalla y cableado** (RF-02–RF-04, RF-07, RF-08)
  - `ProcessScreen`, `ReservationBanner`, `App`, `MainActivity` y `AppContainer`.
  - Validación: build, lint y recorrido de solo lectura.
- [x] **T4 · Verificación integral** (CA-01–CA-10)
  - Suite host, build, lint y recorrido real con un pendiente creado al ejecutar CU-06.
  - Se observaron `WAITING_FOR_PHONE`, el vencimiento, el aviso de teléfono, la actualización y el descubrimiento tras reiniciar.

## Cierre

### CA y evidencia

| CA | Estado | Evidencia |
| --- | --- | --- |
| CA-01 | Verificado | `KtorReservationsApiTest.processReadsTheDurableProgressWithTheUserBearer`: ruta y bearer propios, sin POST. |
| CA-02 | Verificado | `ReservationViewModelTest.discoveryOpensTheOwnPendingProcessOnlyOnce` y `KtorReservationsApiTest.processReadsTheDurableProgressWithTheUserBearer`: campos presentados sin completar ausentes. |
| CA-03 | Verificado | `ProcessPresentationTest.theExpirationIsShownInTheSameTimeZoneAsTheAppointmentsWithoutTheDeviceClock` e `InstantFormatTest`: `expiresAt` UTC convertido a la zona de atención (`09/11/2026 · 12:30`), incluido el cambio de día (`2026-11-10T02:30:00Z` → `09/11/2026 · 23:30`); sin comparación con el reloj local. |
| CA-04 | Verificado | En CU-08 se informa el requisito sin enviar; el formulario y el envío pertenecen a CU-07 y se validan en `docs/features/completar-telefono/`. |
| CA-05 | Verificado | `ReservationViewModelTest.aFailedRefreshKeepsTheLastReportedProcess`: conserva el último proceso y muestra el mensaje. `App` vuelve a consultar al pulsar “Ver estado”. |
| CA-06 | Verificado | `KtorReservationsApiTest.pendingReadsTheOwnProcessAndAnAbsentPendingIsNull` y `ReservationViewModelTest.discoveryOpensTheOwnPendingProcessOnlyOnce`; en emulador, al reiniciar la app el pendiente real se descubrió y el proceso se abrió sin un `POST` nuevo. |
| CA-07 | Verificado | `ReservationViewModelTest.aDiscoveryFailureDoesNotBlockTheSearchAndCanBeRetried`: la búsqueda sigue disponible y “Reintentar” repite la consulta. |
| CA-08 | Verificado | `ReservationViewModelTest.aPendingProcessHiddenByTheUserRemainsAvailableThroughTheBanner` y `dismissDiscardsFinalRejectedAndUnregisteredProcesses`: el aviso “Ver estado” conserva el proceso sin cancelarlo. |
| CA-09 | Verificado | `ReservationViewModelTest.aLateStartResponseAfterResetDoesNotPublishState`, `aLateRefreshResponseAfterResetDoesNotPublishState` y `aResponseFromAPreviousOperationDoesNotReplaceTheNewerOne`. |
| CA-10 | Verificado | Revisión de dependencias y del árbol: sin almacenamiento, polling ni reconciliación nuevos (solo Ktor, sesión y Compose existentes). |

### Regresión

- `./gradlew :shared:testAndroidHostTest`: 114 pruebas, 0 fallos (11 de contrato, 21 del ViewModel y 3 de presentación).
- `./gradlew :androidApp:assembleDebug :androidApp:lintDebug`: BUILD SUCCESSFUL; 5 avisos de lint preexistentes, ninguno en el código nuevo.
- Simplificación: `visible`, `leave()` y `show()` se reemplazaron por `processVisible` en `App` y por `dismiss()` para descartar un proceso final. “Ver estado” consulta Turnos (`refresh`/`verify`).
- Invalidación por cancelación: `aLateStartResponseAfterResetDoesNotPublishState`, `aLateRefreshResponseAfterResetDoesNotPublishState` y `aResponseFromAPreviousOperationDoesNotReplaceTheNewerOne`.
- Solo `404`/`RESERVATION_PROCESS_NOT_FOUND` acredita inexistencia; `400`/`403` no habilitan otro POST (`KtorReservationsApiTest.onlyAnAuthenticNotFoundEnablesRetryingTheStart`, `ReservationViewModelTest.aFailedVerificationKeepsTheUncertaintyAndDoesNotEnableAnotherStart`).

### Prueba real de CU-08 (2026-11-09)

- Proceso real `09111d89-…`, fase `WAITING_FOR_PHONE`, `expiresAt` `2026-11-09T03:28:39Z` y `hold_id` `hold-eb4fa650-…`. Turnos lo informó como pendiente del usuario autenticado; no se creó otro proceso.
- “Actualizar” ejecutó `GET /api/reservation-processes/{processId}`. Un primer intento falló sin código HTTP registrado y la pantalla conservó el proceso con “Reintentar”; el siguiente devolvió el estado y mostró el aviso de teléfono. El cliente solo publica el proceso con `200`, por lo que el resultado es consistente con esa respuesta, aunque no se capturó el código con un proxy.
- “Volver” dejó el banner “Tenés un proceso de reserva en curso”; “Ver estado” reabrió el proceso y repitió el `GET` con la misma fase y vencimiento.
- Reinicio de la app: `GET /api/reservations?page=0&size=1` devolvió el pendiente y la app abrió el proceso automáticamente, sin iniciar otro. El descubrimiento solo publica con `200`; no se capturó el código con un proxy.
- Aislamiento observado con una cuenta distinta: `GET` del mismo `processId` respondió `404 RESERVATION_PROCESS_NOT_FOUND`; no se expuso el proceso ajeno.
- Estado final: pendiente con hold vigente y sin `reservation_id`; no confirmado. No se envió teléfono, no se canceló y no se esperó el vencimiento.
- No se detectaron defectos funcionales en el cliente.

### Límites

- La sesión correspondía a una cuenta local de prueba (id 33) cuya titularidad no está confirmada; no se cambió de cuenta.
- El código HTTP exacto del `POST` no fue capturado y no se observó el `202`. El del primer `GET` fallido tampoco quedó registrado.
- No se ejecutó CU-07 (envío de teléfono) ni CU-10 (cancelación); no se confirmó ni canceló una reserva.

### Desviaciones del PLAN

- El estado único reduce las etapas a `StartStatus` y el proceso conocido; `message` reemplaza `notice`, `error` y `discoveryFailure`. El banner se deriva del estado y `App` decide su visibilidad.
- `dismiss()` descarta solo procesos finales o rechazos; un proceso pendiente o incierto se conserva. No cambian RF, CA ni contratos.
