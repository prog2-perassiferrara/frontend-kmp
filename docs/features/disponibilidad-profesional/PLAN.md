# PLAN: Consultar disponibilidad de un profesional

**SPEC de referencia:** [SPEC.md](SPEC.md) (implementada y verificada el 2026-11-08).
**CU e issue:** [CU-05](../../use-cases/CU-05-consultar-disponibilidad.md); issue #8.
**Estado:** Implementado y verificado el 2026-11-08.
**Alcance:** selección desde búsqueda, consulta de disponibilidad, marca visual de un horario y vuelta; sin reserva ni holds.

## Contexto técnico verificado

| Componente o contrato | Ruta verificada | Uso previsto |
| --- | --- | --- |
| `AvailabilityController` | `backend-turnos/.../availability/infrastructure/web/controller` | `GET /api/appointment-availability?professionalId&date`, `Cache-Control: no-store`. |
| `AvailabilityResponse` | `.../web/dto/AvailabilityResponse.java` | `professionalId`, `date`, `timeZone`, `availableStartTimes` (`HH:mm:ss`). |
| `GetAvailabilityUseCaseImpl` | `.../application/usecases` | Fecha pasada → `INVALID_AVAILABILITY_QUERY`; sin agenda → lista vacía; agenda deshabilitada → `PROFESSIONAL_DISABLED`. |
| `AvailabilityProblemHandler` | `.../web/AvailabilityProblemHandler.java` | 400 (`INVALID_AVAILABILITY_QUERY`, `PROFESSIONAL_DISABLED`), 404 `PROFESSIONAL_NOT_FOUND`, 503 (`CATALOG_NOT_CURRENT`, `CATALOG_UNAVAILABLE`, `OCCUPANCIES_UNAVAILABLE`, `TECHNICAL_INTEGRATION_UNAVAILABLE`), 500. |
| `AuthenticatedApiClient` | `shared/.../network` | Inyecta el JWT, descarta respuestas tardías y cierra la sesión ante 401. |
| `SearchViewModel`/`SearchScreen` | `shared/.../ui/search` | Resultados de CU-04, estado conservado y punto de partida de la selección. |
| `App`/`AppContainer` | `shared/.../App.kt`, `shared/src/androidMain/.../config` | Destino autenticado y cableado por proceso. |

**Verificación en vivo del contrato:** `201 2026-11-09` devolvió cinco horarios; `999999` respondió 404 `PROFESSIONAL_NOT_FOUND`; `214` (deshabilitado) respondió 400 `PROFESSIONAL_DISABLED`; fecha pasada y formato inválido respondieron 400 `INVALID_AVAILABILITY_QUERY`; sin bearer, 401. Con el catálogo en sincronización respondió 503 `CATALOG_NOT_CURRENT`.

## Solución propuesta

1. `AvailabilityApi`/`KtorAvailabilityApi` consumen la consulta sobre `AuthenticatedApiClient` y traducen estados a `AvailabilityException`, con `AvailabilityError` presentable.
2. `SearchUiState` agrega `selected: Professional?`; `SearchViewModel` expone `select(professional)` y `backToSearch()`. La selección vive con el estado ya existente, por lo que sobrevive a la rotación y se descarta al cerrar sesión.
3. `AvailabilityViewModel` conserva profesional, fecha, horarios, horario marcado, carga y error; valida la forma de la fecha, consulta, reintenta y descarta la respuesta que no corresponde a la selección vigente. `selectTime` solo guarda una marca visual.
4. `AvailabilityScreen` presenta identificación del profesional, fecha, estados y horarios. Tocar un horario lo marca con un selector; no reserva ni crea holds, porque eso pertenece a CU-06.
5. `App` muestra una pantalla u otra según `searchViewModel.state.selected`; `AppContainer` y `MainActivity` cablean la nueva API y pantalla. Al cerrar sesión se descartan búsqueda y disponibilidad.

**Navegación:** dos destinos autenticados resueltos con el estado existente y una acción “Volver”; no se agrega Navigation Compose ni BottomBar. Una biblioteca de navegación y una barra de secciones no resuelven un problema del CU. El retroceso del sistema en la pantalla de disponibilidad sigue el comportamiento actual de la app (cierra la actividad): la vuelta explícita es el botón, y así queda registrado como límite.

**Respuestas fuera de orden:** la consulta cancela la anterior y solo publica si la fecha del estado sigue siendo la consultada. Es una condición del CA-07, no un mecanismo general de concurrencia.

**Fecha:** campo de texto `AAAA-MM-DD`, coherente con el filtro de CU-04 y sin dependencias nuevas; se precarga con el filtro de búsqueda cuando ya tiene esa forma. El rango (desde hoy, zona de atención) lo valida Turnos.

## Componentes afectados

| Componente o ruta | Acción | Responsabilidad | Requisito |
| --- | --- | --- | --- |
| `availability/AvailabilityError.kt` | Crear | Errores presentables y excepción. | RF-05, RF-06 |
| `availability/data/AvailabilityApi.kt` | Crear | Modelo de disponibilidad y contrato de consulta. | RF-03 |
| `availability/data/KtorAvailabilityApi.kt` | Crear | HTTP, parámetros y mapeo de estados. | RF-03–RF-06 |
| `ui/availability/AvailabilityUiState.kt` | Crear | Estado de presentación, incluido el horario marcado. | RF-02–RF-10 |
| `ui/availability/AvailabilityViewModel.kt` | Crear | Validación, consulta, reintento, marca de horario y descarte. | RF-02–RF-07, RF-10 |
| `ui/availability/AvailabilityScreen.kt` | Crear | Pantalla, horarios seleccionables visualmente y acción de vuelta. | RF-01, RF-04, RF-05, RF-09, RF-10 |
| `ui/search/SearchUiState.kt`, `SearchViewModel.kt`, `SearchScreen.kt` | Modificar | Resultado seleccionable y selección conservada. | RF-01, RF-08 |
| `App.kt`, `MainActivity.kt`, `AppContainer.kt` | Modificar | Destino, cableado y descarte al cerrar sesión. | RF-08, RF-09 |

## Datos y contratos

- **Entrada:** `professionalId` positivo, `date` ISO `yyyy-MM-dd` desde hoy en la zona de atención.
- **Salida:** `professionalId`, `date`, `timeZone`, `availableStartTimes` ordenados y sin duplicados. Lista vacía válida; el campo es obligatorio y un 200 sin él se trata como error, no como ausencia.
- **Productor:** Turnos, que a su vez usa Catálogo vigente y ocupaciones centrales. KMP no consulta agendas ni ocupaciones.
- **Persistencia:** ninguna. El profesional, la fecha, los horarios y la marca viven en memoria mientras dure el proceso.
- **Errores leídos por HTTP y `code`**, nunca por `detail`.

## Estado, operaciones y errores

- `AvailabilityUiState`: profesional seleccionado, fecha, error de fecha, horarios, horario marcado, carga, resultado obtenido y error.
- `open(professional, initialDate)` fija la selección y descarta resultados anteriores; `consult()` valida y consulta; `selectTime(time)` marca un horario ofrecido y `retry()` repite la fecha vigente.
- `changeDate` y cada nueva consulta descartan el horario marcado, porque pertenecía a otra lista.
- `400`/`404` se informan sin reintento; `503`, `5xx`, transporte o cuerpo ilegible permiten “Reintentar”. El `401` lo resuelve el cliente protegido.
- `reset()` cancela la consulta y descarta el estado al cerrar sesión.

## Dependencias y configuración

No se agregan dependencias ni configuración. Se reutilizan Ktor, kotlinx.serialization, Compose Material 1, `TURNOS_BASE_URL` y el cliente autenticado.

## Estrategia de validación

| Criterio | Método | Evidencia |
| --- | --- | --- |
| CA-01 | `SearchViewModelTest` (selección y vuelta); emulador | Selección de un resultado y regreso con filtros intactos. |
| CA-02 | `KtorAvailabilityApiTest` | Parámetros exactos, host de Turnos y bearer. |
| CA-03 | `KtorAvailabilityApiTest`, `AvailabilityViewModelTest`; emulador | Horarios en orden y formato `HH:mm`. |
| CA-04 | `AvailabilityViewModelTest`; emulador | Lista vacía informada sin error. |
| CA-05 | `AvailabilityViewModelTest`; emulador | Error 503 y reintento exitoso. |
| CA-06 | `KtorAvailabilityApiTest`, `AvailabilityViewModelTest` | 400/404 no reintentables; 401 cierra sesión. |
| CA-07 | `AvailabilityViewModelTest` | Respuesta de otra fecha descartada. |
| CA-08 | `SearchViewModelTest`, `AvailabilityViewModelTest`; emulador | Vuelta, rotación —incluido el horario marcado— y logout. |
| CA-09 | Recorrido en emulador | Aviso de que consultar no reserva. |
| CA-10 | `KtorAvailabilityApiTest` (campo obligatorio), `AvailabilityViewModelTest`; emulador | Marca de un horario ofrecido, rechazo de uno ajeno y descarte al cambiar fecha. No se crea ningún hold ni proceso. |

**Comandos:** `./gradlew :shared:testAndroidHostTest`, `./gradlew :androidApp:assembleDebug :androidApp:lintDebug`, `./gradlew :androidApp:installDebug`.

**Limitaciones previstas:** el backend real no permite forzar un 503 a demanda; los estados de error se cubren con pruebas de contrato. La vuelta con el gesto del sistema no se intercepta.

## Orden de implementación

1. Contrato y cliente HTTP con pruebas.
2. Selección en búsqueda y pruebas de navegación.
3. ViewModel, pantalla y cableado.
4. Pruebas, build, lint y recorrido real.

## Riesgos y decisiones pendientes

- **Riesgos:** el backend valida el rango de fechas y la zona de atención; la app solo verifica la forma. Los horarios no se persisten ni se consideran vigentes entre consultas.
- **Decisiones pendientes:** ninguna de comportamiento. La reserva y la creación de holds pertenecen a CU-06; aquí solo se marca el horario.
