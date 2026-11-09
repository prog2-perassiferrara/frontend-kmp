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
  - `MyReservationsUiState` y `MyReservationsViewModel`: carga, vacío, error, actualización, anterior/siguiente y relectura al reabrir o volver a la pestaña Cuenta. Tamaño fijo 10.
  - Validación: `MyReservationsViewModelTest` (15 pruebas al cierre correctivo: carga, paginación, cancelación, avisos de cancelación y la regresión de I-1).
- [x] **T3 · Presentación y navegación** (RF-02, RF-03)
  - `MyReservationsScreen` dentro de la pestaña “Cuenta” de `MainScreen` (BottomBar), que también contiene «Cerrar sesión»; el pendiente ofrece “Ver estado” mediante `ReservationViewModel.present(process)`, sin otra lectura.
  - Validación: `MyReservationsPresentationTest` (4 pruebas); `ReservationViewModelTest.presentingAReportedProcessShowsItWithoutReadingItAgain`.
- [x] **T4 · Verificación integral** (CA-01–CA-05)
  - Suite host, build, lint y lectura real en emulador.

## Criterios de aceptación

| CA | Estado | Evidencia |
| --- | --- | --- |
| CA-01 | Verificado | `ownReservationsReadPendingHistoryAndPagination`: método GET, ruta `/api/reservations`, bearer, `page=1` y `size=10`. `ownReservationsIncompleteHistoryIsAReadFailure` exige el historial completo y `pending=null` sigue siendo válido. |
| CA-02 | Verificado | `KtorReservationsApiTest` (nombres históricos y campos), `MyReservationsPresentationTest` (estados legibles, nombre ausente sin inventarlo, fecha `DD/MM/AAAA` y hora local) y lectura real en emulador. |
| CA-03 | Verificado | `MyReservationsViewModelTest` cubre pendiente separado y elegibilidad; `..presentingAReportedProcessShowsItWithoutReadingItAgain` prueba la apertura sin GET. El emulador de esta cuenta no tenía pendiente. |
| CA-04 | Verificado | `loadsTheFirstPageAndShowsAnEmptyList` (carga y vacío), `aFailedLoadShowsAnErrorAndRetryReadsAgain` (error y reintento), `reopeningTheScreenReadsTheFirstPageAgain` y “Actualizar” real. |
| CA-05 | Verificado | `paginatesWithinTheReportedTotals`: página 0→1→2→1 y límites según `totalPages`; en emulador `Página 1 de 1` con “Anterior” y “Siguiente” deshabilitados. |

### Pruebas ejecutadas (2026-11-09)

- `./gradlew :shared:testAndroidHostTest`: 158 pruebas, 0 fallos (22 de contrato de reservas, 13 del ViewModel de reservas propias, 32 del ViewModel de proceso, 4 de presentación de reservas propias y 2 del selector de fecha).
- `./gradlew :androidApp:assembleDebug :androidApp:lintDebug`: `BUILD SUCCESSFUL`; avisos de lint preexistentes.

### Lectura real en emulador (2026-11-09)

- Sesión ficticia `cu04patient` (id 35). `GET /api/reservations?page=0&size=10` respondió tres registros reales.
- La app mostró dos solicitudes vencidas de Luciana Paredes (`11/11/2026 · 20:00` y `18:00`) y una confirmada del profesional 218 (`10/11/2026 · 14:00`) sin nombre histórico, presentada como «Profesional sin nombre registrado». Se usaron etiquetas de estado en español y fechas `DD/MM/AAAA`, sin códigos internos.
- `Página 1 de 1 · 3 reserva(s)`; ambos botones de paginación quedaron deshabilitados. “Volver” regresó a la búsqueda y una nueva apertura volvió a mostrar el listado sin errores.

### Límites

- No se crearon reservas ni se modificó la base. La cuenta no tenía un pendiente real en esta etapa: la separación se verificó con pruebas y con los datos de CU-07.
- La paginación múltiple se verificó con dobles; la cuenta real solo tiene una página.
- En la cancelación real de CU-10, “Mis reservas” releyó 6 registros, mostró la reserva 1677 como `CANCELLED` y conservó el estado al salir y reabrir.

### Cierre correctivo (2026-11-09)

- **Relectura al volver a Cuenta:** el destino `Reservations` ejecuta `MyReservationsViewModel.load()` al entrar y `reopeningTheScreenReadsTheFirstPageAgain` cubre la relectura del ViewModel. En el emulador, `Buscar → Cuenta → Buscar → Cuenta` mostró el listado, pero no se capturó la nueva petición HTTP.
- **Avisos de cancelación:** `read()` conserva `cancelMessage` solo en la lectura que reconcilia un rechazo o un resultado incierto; las posteriores lo retiran. `aCancellationNoticeSurvivesTheReconcilingReadAndClearsOnTheNextOne` y `aCompletedCancellationNoticeDoesNotSurviveALaterRead` lo verifican.
- **Regreso desde Cuenta:** si un proceso se abre desde la pestaña Cuenta, «Volver» hace `popBackStack()` y regresa allí; desde el flujo de búsqueda conserva el comportamiento anterior. Se reutiliza el `NavController` existente.
- La navegación desde Cuenta hacia un proceso real no pudo observarse porque la cuenta de prueba no tenía un pendiente; queda cubierta por la lectura del código y por los estados ya verificados.
- Sin cambios en `backend-turnos`, `backend-catalogo` ni en CU-01/CU-08.
