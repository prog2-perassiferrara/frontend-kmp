# PLAN: Consultar mis reservas

**SPEC:** [SPEC.md](SPEC.md) · **CU:** [CU-09](../../use-cases/CU-09-consultar-reservas.md) · **Issue:** #12
**Estado:** Implementado el 2026-11-09.

## Contexto verificado

| Contrato o componente | Uso |
| --- | --- |
| `GET /api/reservations?page&size` (`ReservationController.list`) | Devuelve `pending` y `history {content,page,size,totalElements,totalPages}`; `size` entre 1 y 100. |
| `ReservationDtoMapper` | Mismos campos de proceso, nombres históricos opcionales, `cancellation` y `warningCode`. |
| `KtorReservationsApi` / `AuthenticatedApiClient` | Bearer y mapeo de errores existentes; hoy solo lee `pending`. |
| `ReservationViewModel` / `ProcessScreen` | CU-08 ya muestra un proceso recibido. |
| `SearchScreen` | Pantalla autenticada desde la que se abre la consulta. |

El historial excluye el proceso activo y puede contener `CONFIRMED`, `CANCELLED`, `FAILED` y `EXPIRED`. Los nombres históricos son opcionales: la reserva confirmada de la prueba real no los tenía.

## Solución

1. `ReservationsApi` agrega `OwnReservationsPage` y `reservations(page, size)`. `KtorReservationsApi` reutiliza el GET autenticado y deserializa `pending` e `history`; `pending()` se conserva para CU-08 sin cambiar su mensaje.
2. `ReservationProcess` incorpora `cancellation` del contrato. Se derivan `canRequestCancellation` (`CONFIRMED` sin cancelación `PENDING`) y avisos de cancelación; no se replica la máquina del backend.
3. Un `MyReservationsViewModel` nuevo concentra la responsabilidad real de la pantalla: cargar al abrir, paginar, actualizar y refrescar tras cancelar. No hay repositorio, coordinador ni persistencia.
4. `MyReservationsScreen` presenta encabezado, pendiente separado con “Ver estado”, historial, vacío, error y paginación. Los textos se derivan en funciones pequeñas y testeables.
5. `App` agrega `reservationsVisible` junto a `processVisible`; desde el pendiente se reutiliza CU-08 con `ReservationViewModel.present(process)` —sin otra lectura ni POST—. El estado se descarta al cerrar sesión.
6. `SearchScreen` incorpora “Mis reservas” en su encabezado; no se agrega Navigation Compose ni BottomBar.

## Componentes afectados

| Componente | Acción | Requisito |
| --- | --- | --- |
| `reservation/data/ReservationsApi.kt` | Modificar | RF-01, RF-03 |
| `reservation/data/KtorReservationsApi.kt` | Modificar | RF-01, RF-02 |
| `ui/reservation/MyReservationsUiState.kt` | Crear | RF-02–RF-05 |
| `ui/reservation/MyReservationsViewModel.kt` | Crear | RF-01, RF-04, RF-05 |
| `ui/reservation/MyReservationsPresentation.kt` | Crear | RF-02, RF-03 |
| `ui/reservation/MyReservationsScreen.kt` | Crear | RF-02–RF-05 |
| `ui/reservation/ReservationViewModel.kt` | Modificar | RF-03 (abrir CU-08) |
| `ui/search/SearchScreen.kt`, `App.kt`, `MainActivity.kt` | Modificar | Navegación mínima |

## Validación

| Criterio | Método |
| --- | --- |
| CA-01 | `KtorReservationsApiTest`: método, ruta, bearer, `page` y `size`. |
| CA-02 | `MyReservationsPresentationTest` y `KtorReservationsApiTest`: campos y nombre de reserva fallback. |
| CA-03 | `MyReservationsViewModelTest` y `ReservationViewModelTest.present`: pendiente separado y apertura sin POST. |
| CA-04 | `MyReservationsViewModelTest`: carga, vacío, error, reintento y relectura al reabrir. |
| CA-05 | `MyReservationsViewModelTest`: páginas y totales. |

**Comandos:** `./gradlew :shared:testAndroidHostTest`, `./gradlew :androidApp:assembleDebug :androidApp:lintDebug`.
**Límites:** la verificación real es de solo lectura; no se crean ni cancelan reservas sin autorización. Dependencias nuevas: ninguna.
