# PLAN: Búsqueda de profesionales

**SPEC de referencia:** [SPEC.md](SPEC.md) (implementada y verificada el 2026-11-08).
**CU e issue:** [CU-04](../../use-cases/CU-04-buscar-profesionales.md); issue #7.
**Estado:** Implementado y verificado el 2026-11-08.
**Alcance:** filtros, resultados paginados, aviso de copia y estados básicos. No incluye selección ni CU-05.

## Contexto técnico verificado

| Componente o archivo existente | Ruta verificada | Responsabilidad y uso previsto |
| --- | --- | --- |
| `CatalogSearchController` | `backend-catalogo/.../web/controller/CatalogSearchController.java` | Dos GET reales: `/api/catalog/professionals` y `/api/catalog/categories`. |
| `ProfessionalSearchRequest`/`Criteria` | `backend-catalogo/.../web/dto`, `.../domain/model` | Filtros opcionales, `page=0`, `size=20` (1–100), nombre máximo 100. |
| `ProfessionalSearchResponse` | `backend-catalogo/.../web/dto` | `content`, `page`, `size`, `totalElements`, `totalPages`, `catalog`. |
| `CatalogQueryExceptionHandler` | `backend-catalogo/.../web/exception` | 400 `INVALID_SEARCH_CRITERIA`, 503 `CATALOG_COPY_UNAVAILABLE`/`CATALOG_UNAVAILABLE`. |
| `AuthenticatedApiClient` | `shared/.../network/AuthenticatedApiClient.kt` | Inyecta bearer, conserva cuerpo y cierra la sesión ante 401. |
| `AppContainer` | `shared/src/androidMain/.../config/AppContainer.kt` | Cableado Android por proceso. |
| `App`/`MainActivity` | `shared/.../App.kt`, `androidApp/.../MainActivity.kt` | Composición y navegación por estado de sesión. |
| `SessionManager`/`SessionState` | `shared/.../session` | Fuente de sesión y `epoch`. |

**Convenciones:** MVVM ligero con `State<mutableStateOf>`, pantallas como clases con `Content(...)`, dependencias por constructor, Compose Material 1 y Ktor con `AuthenticatedApiClient`. `AuthenticatedScreen` (área provisoria) se retiró.

## Solución propuesta

1. `CatalogApi` declara las dos consultas y modelos del contrato (`ProfessionalQuery`, `ProfessionalPage`, `Professional`, `CatalogCategory`, `CatalogState`). `KtorCatalogApi` las implementa sobre `AuthenticatedApiClient`, construye los parámetros omitiendo nulos/vacíos y traduce estados a `CatalogException`.
2. `SearchViewModel` conserva filtros, página y resultados; ejecuta la carga inicial una vez, valida nombre/fecha antes de enviar y expone `SearchUiState`.
3. `SearchScreen` presenta filtros, estados, resultados y paginación. `App` la muestra en `SIGNED_IN` y llama `reset()` al pasar a `SIGNED_OUT`.
4. `AppContainer` cablea `KtorCatalogApi(authenticatedApi)`; `MainActivity` compone `App` con `SearchScreen`.

**Navegación:** no se agrega Navigation Compose ni BottomBar. La única pantalla autenticada de este CU se resuelve con el `when` por estado de sesión que ya existe en `App`. Una biblioteca de navegación con un solo destino no resolvería un problema concreto; CU-05 podrá introducirla cuando exista un segundo destino. Es una decisión interna, no un requisito externo.
**Actualización al cierre (2026-11-09):** al existir dos destinos, se incorporó Navigation Compose con BottomBar en `MainScreen`; esta nota conserva la decisión histórica de CU-04.

**Concurrencia:** `isSearching` evita consultas solapadas y el `Job` se cancela al cerrar sesión. No se agregan épocas, reintentos automáticos ni recuperación.

## Módulos y componentes afectados

| Módulo | Existe / nuevo | Responsabilidad y cambios |
| --- | --- | --- |
| `shared/commonMain` | Existente | Nuevos `catalog/` y `ui/search/`; `App.kt` actualizado. |
| `shared/androidMain` | Existente | `AppContainer` cablea la API de Catálogo. |
| `androidApp` | Existente | `MainActivity` sustituye la pantalla provisoria por `SearchScreen`. |

| Componente o ruta | Acción | Cambio y responsabilidad | Requisito |
| --- | --- | --- | --- |
| `catalog/CatalogError.kt` | Crear | Errores presentables y excepción. | RF-07 |
| `catalog/data/CatalogApi.kt` | Crear | Modelos y contrato de consulta. | RF-01, RF-03 |
| `catalog/data/KtorCatalogApi.kt` | Crear | HTTP, parámetros y mapeo de errores. | RF-01–RF-08 |
| `ui/search/SearchUiState.kt` | Crear | Estado de presentación. | RF-04–RF-11 |
| `ui/search/SearchViewModel.kt` | Crear | Validación, consulta, paginación y descarte. | RF-01–RF-11 |
| `ui/search/SearchScreen.kt` | Crear | Filtros, estados, resultados y aviso. | RF-02, RF-04–RF-10 |
| `ui/session/AuthenticatedScreen.kt` | Eliminar | Área provisoria reemplazada. | RF-10 |
| `App.kt`, `MainActivity.kt`, `AppContainer.kt` | Modificar | Composición y cableado. | RF-09, RF-10 |

## Datos y contratos

- **Entrada:** `categoryId` (positivo), `name` (≤100, recortado), `enabled` (booleano), `availableOn` (`AAAA-MM-DD`), `page≥0`, `size=20`. Los nulos y textos vacíos se omiten.
- **Salida:** `content[]` con `id`, `firstName`, `lastName`, `enabled` y `category {id,name,enabled}`; totales; `catalog {state,copyAvailable}`.
- **Productor:** Catálogo, contrato documentado en `backend-catalogo/docs/CONTRATOS.md` y verificado en el controlador y DTO reales.
- **Persistencia:** ninguna en el dispositivo. La copia local pertenece a Catálogo; la app solo conserva el estado en memoria del ViewModel.
- **Orden y paginación:** los define el backend. La app no reordena ni pide páginas fuera de `0..totalPages-1`.
- **Aviso:** se activa con `state != "CURRENT"` o `copyAvailable == false`; una respuesta sin `catalog` se trata como desconocida y muestra el aviso.

## Estado, operaciones y errores

- **Estado:** `SearchUiState` con filtros, categorías, página, totales, resultados, carga, error y aviso. `hasResult` distingue “sin búsqueda todavía” de vacío válido.
- **Carga inicial:** `ensureLoaded()` una vez por ViewModel; se vuelve a habilitar tras `reset()`.
- **Validación local:** nombre recortado >100 y fecha que no cumple `AAAA-MM-DD` bloquean la consulta; el selector entrega siempre ese formato. La validez real de la fecha y los límites restantes quedan en Catálogo.
- **Conservación:** el ViewModel vive en el owner Android de la Activity, por lo que sobrevive a la rotación. Al cerrar sesión se cancela la consulta y se descarta el estado.
- **Errores:** 400 → “Revisá los filtros…”; 503 con código `CATALOG_COPY_UNAVAILABLE` → copia inexistente; resto de 5xx/403/transporte/JSON ilegible → indisponibilidad con reintento. El 401 lo resuelve `AuthenticatedApiClient`.
- **Categorías:** se piden al cargar y al reintentar si fallaron. Su fallo se informa y no impide buscar.

## Dependencias y configuración

No se agregan dependencias. Se reutilizan Ktor, kotlinx.serialization, Compose Material 1 y el cliente autenticado. `CATALOG_BASE_URL` ya existe en `androidApp`. El permiso `ACCESS_LOCAL_NETWORK` del manifest de debug debe concederse tras cada instalación en el emulador (`adb shell pm grant com.prog2.frontend android.permission.ACCESS_LOCAL_NETWORK`).

## Estrategia de validación

| Criterio | Método | Evidencia |
| --- | --- | --- |
| CA-01 | `KtorCatalogApiTest.sendsTheFourFilters…` y `omitsUnsetFilters…`; `SearchViewModelTest.combinesTheFourFilters…` | Parámetros exactos y página 0. |
| CA-02 | `SearchViewModelTest.paginationRequestsAdjacentPagesOnly`; recorrido en emulador | Página 1→2 con 24 resultados. |
| CA-03 | `SearchViewModelTest.initialLoadSearchesWithoutFiltersAndFlagsOutdatedCopy`; emulador | Aviso mostrado en vivo durante un ciclo real `SYNCHRONIZING` de Catálogo (`/tmp/cu04-warning.png`). |
| CA-04 | `anEmptyPageIsAValidResultWithoutError`; emulador | “No se encontraron profesionales…”. |
| CA-05 | `unavailableCopyIsAnErrorAndRetryRepeatsTheQuery`; `categoriesReportUnavailableCopyWithTheContractCode` | Mensaje diferenciado del vacío. |
| CA-06 | `KtorCatalogApiTest.mapsSearchErrorsWithoutRetrying`; emulador | Error de transporte observado y “Reintentar” exitoso. |
| CA-07 | `a401ClosesTheSessionThroughTheExistingClient`; emulador | Cierre de sesión y descarte de filtros. |
| CA-08 | `readsCategoriesIncludingDisabledOnes`; `aCategoriesFailureDoesNotBlockSearch…`; emulador | Ocho categorías reales en el filtro. |
| CA-09 | `resetDiscardsFilters…`; emulador con rotación | Estado conservado al rotar. |
| CA-10 | Recorrido en emulador | Texto visible de agenda semanal. |

**Comprobaciones de regresión:** 62 pruebas host del módulo, `assembleDebug`, `lintDebug` e `installDebug`.

**Comandos:** `./gradlew :shared:testAndroidHostTest`, `./gradlew :androidApp:assembleDebug :androidApp:lintDebug`, `./gradlew :androidApp:installDebug`.

**Limitaciones del entorno:** el aviso se observó con `SYNCHRONIZING` y no con `STALE`, porque el Catálogo real volvió a `CURRENT`. No se forzó una copia inexistente ni se detuvo Catálogo. La selección de profesionales queda para CU-05.

## Orden de implementación

1. Contrato y cliente HTTP; prueba de parámetros y errores.
2. ViewModel y pruebas de filtros, estados y paginación.
3. Pantalla, composición y cableado; build y lint.
4. Recorrido real en emulador y cierre documental.

## Riesgos y decisiones pendientes

- **Riesgos:** la fecha se elige con `DatePickerModal` (el mismo `expect/actual` de CU-05 sobre `DatePickerDialog`), habilitando fechas pasadas porque el CU no las restringe; el valor interno sigue siendo `AAAA-MM-DD` y el backend valida la fecha real. El permiso de red local debe concederse tras cada instalación.
- **Decisiones pendientes:** ninguna de comportamiento. La selección/navegación pertenece a CU-05.
