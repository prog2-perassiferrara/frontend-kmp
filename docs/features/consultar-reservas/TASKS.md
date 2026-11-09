# TASKS: Consultar mis reservas

**SPEC:** [SPEC.md](SPEC.md) · **PLAN:** [PLAN.md](PLAN.md) · **CU:** [CU-09](../../use-cases/CU-09-consultar-reservas.md) · **Issue:** #12
**Estado:** Implementada y verificada con pruebas y con una lectura real en emulador (2026-11-09).

## Coherencia previa

Cada RF tiene CA y validación en el PLAN; las tareas no agregan alcance. Sin decisiones bloqueantes.

## Tareas

- [x] **T1 · Contrato y modelos** (RF-01, RF-02)
  - `OwnReservationsPage`, `CancellationProgress` y `reservations(page, size)`; lectura de `pending` e `history` con la paginación de Turnos. `pending()` se conserva para CU-08.
  - Validación: `KtorReservationsApiTest.ownReservationsReadPendingHistoryAndPagination`, `ownReservationsIncompleteHistoryIsAReadFailure`, `ownReservationsWithoutPendingAndWithACompleteEmptyHistoryIsEmptyButValid` y `ownReservationsFailureAndTransportAreUncertainReads`.
- [x] **T2 · Estado de la pantalla** (RF-04, RF-05)
  - `MyReservationsUiState` y `MyReservationsViewModel`: carga, vacío, error, actualización, anterior/siguiente y relectura al reabrir. Tamaño fijo 10.
  - Validación: `MyReservationsViewModelTest` (11 pruebas).
- [x] **T3 · Presentación y navegación** (RF-02, RF-03)
  - `MyReservationsScreen`, textos en `MyReservationsPresentation`, “Mis reservas” en `SearchScreen` y `reservationsVisible` en `App`. El pendiente ofrece “Ver estado” mediante `ReservationViewModel.present(process)`, sin otra lectura.
  - Validación: `MyReservationsPresentationTest` (4 pruebas); `ReservationViewModelTest.presentingAReportedProcessShowsItWithoutReadingItAgain`.
- [x] **T4 · Verificación integral** (CA-01–CA-05)
  - Suite host, build, lint y lectura real en emulador.

## Criterios de aceptación

| CA | Estado | Evidencia |
| --- | --- | --- |
| CA-01 | Verificado | `ownReservationsReadPendingHistoryAndPagination`: método GET, ruta `/api/reservations`, bearer, `page=1` y `size=10`. `ownReservationsIncompleteHistoryIsAReadFailure` exige el historial completo y `pending=null` sigue siendo válido. |
| CA-02 | Verificado | `KtorReservationsApiTest` (nombres históricos y campos), `MyReservationsPresentationTest` (estados, fallback `Profesional {id}`, fecha/hora) y lectura real en emulador. |
| CA-03 | Verificado | `MyReservationsViewModelTest` cubre pendiente separado y elegibilidad; `..presentingAReportedProcessShowsItWithoutReadingItAgain` prueba la apertura sin GET. El emulador de esta cuenta no tenía pendiente. |
| CA-04 | Verificado | `loadsTheFirstPageAndShowsAnEmptyList` (carga y vacío), `aFailedLoadShowsAnErrorAndRetryReadsAgain` (error y reintento), `reopeningTheScreenReadsTheFirstPageAgain` y “Actualizar” real. |
| CA-05 | Verificado | `paginatesWithinTheReportedTotals`: página 0→1→2→1 y límites según `totalPages`; en emulador `Página 1 de 1` con “Anterior” y “Siguiente” deshabilitados. |

### Pruebas ejecutadas (2026-11-09)

- `./gradlew :shared:testAndroidHostTest`: 154 pruebas, 0 fallos (22 de contrato de reservas, 12 del ViewModel nuevo, 32 del ViewModel previo, 4 de presentación nueva).
- `./gradlew :androidApp:assembleDebug :androidApp:lintDebug`: `BUILD SUCCESSFUL`; avisos de lint preexistentes.

### Lectura real en emulador (2026-11-09)

- Sesión ficticia `cu04patient` (id 35). `GET /api/reservations?page=0&size=10` respondió tres registros reales.
- La app mostró: dos `EXPIRED` de Luciana Paredes (2026-11-11 20:00 y 18:00, motivo del hold vencido) y una `CONFIRMED` del profesional 218 (2026-11-10 14:00) sin nombre histórico; se presentó como “Profesional 218”.
- `Página 1 de 1 · 3 reserva(s)`; ambos botones de paginación quedaron deshabilitados. “Volver” regresó a la búsqueda y una nueva apertura volvió a mostrar el listado sin errores.

### Límites

- No se crearon reservas ni se modificó la base. La cuenta no tenía un pendiente real en esta etapa: la separación se verificó con pruebas y con los datos de CU-07.
- La paginación múltiple se verificó con dobles; la cuenta real solo tiene una página.
- En la cancelación real de CU-10, “Mis reservas” releyó 6 registros, mostró la reserva 1677 como `CANCELLED` y conservó el estado al salir y reabrir.
- Sin cambios en `backend-turnos`, `backend-catalogo` ni en CU-01/CU-08.
