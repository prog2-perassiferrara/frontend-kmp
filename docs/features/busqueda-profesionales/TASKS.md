# TASKS: Búsqueda de profesionales

**Referencias:** [SPEC](SPEC.md), [PLAN](PLAN.md), [CU-04](../../use-cases/CU-04-buscar-profesionales.md), issue #7.
**Estado:** Completado y verificado el 2026-11-08.

**Chequeo previo:** RF → CA → PLAN → tareas comprobado; ningún CA agrega alcance fuera de CU-04. Las decisiones pendientes de la SPEC (predeterminados, combinación, orden, paginación y aviso) se resolvieron con el contrato real de Catálogo. Sin decisiones bloqueantes.

- [x] **T-01 · Consumir el contrato de Catálogo.**
  - **Objetivo:** implementar `CatalogApi`, `KtorCatalogApi`, `CatalogError` y los modelos de consulta, página, profesional, categoría y estado.
  - **Dependencias:** ninguna.
  - **Referencias:** RF-01, RF-03, RF-07, RF-08; CA-01, CA-05.
  - **Validación:** `KtorCatalogApiTest` (6 pruebas): filtros y bearer, parámetros omitidos, mapeo de 400/503/5xx/JSON ilegible, categorías y 401.

- [x] **T-02 · Coordinar filtros, estados y paginación.**
  - **Objetivo:** implementar `SearchUiState` y `SearchViewModel` con carga inicial, validación local, consulta, paginación, aviso de copia, reintento y descarte al cerrar sesión.
  - **Dependencias:** T-01.
  - **Referencias:** RF-01–RF-11; CA-01–CA-09.
  - **Validación:** `SearchViewModelTest` (12 pruebas): carga sin filtros, combinación y reinicio de página, entrada inválida, vacío, copia indisponible, 400 no reintentable, categorías, paginación, consulta única, descarte y recarga, invalidación de paginación al editar filtros y cancelación de la carga de categorías al cerrar sesión.

- [x] **T-03 · Presentar la búsqueda y cablearla.**
  - **Objetivo:** implementar `SearchScreen`, sustituir `AuthenticatedScreen`, actualizar `App`, `MainActivity` y `AppContainer`.
  - **Dependencias:** T-01, T-02.
  - **Referencias:** RF-02, RF-04–RF-10; CA-02, CA-04, CA-08–CA-10.
  - **Validación:** `assembleDebug`, `installDebug` y recorrido real.

- [x] **T-04 · Verificar y cerrar.**
  - **Objetivo:** ejecutar pruebas, build, lint y recorrido en emulador; registrar evidencia y límites.
  - **Dependencias:** T-03.
  - **Referencias:** todos los CA.
  - **Validación:** sección siguiente.

## Evidencia de cierre

**CA-01 · Filtros y consulta:** `sendsTheFourFiltersWithTheUserBearerAndParsesThePage`, `omitsUnsetFiltersAndSendsTheDefaultPagination` y `combinesTheFourFiltersAndRestartsAtTheFirstPage`. En el emulador, nombre “an” + Clínica médica + Habilitados devolvió “Andres Quiroga” (1 resultado), coincidente con la consulta directa al backend.

**CA-02 · Resultados y paginación:** `paginationRequestsAdjacentPagesOnly`. En el emulador, 24 resultados: “Página 1 de 2 · 24 resultado(s)”; “Siguiente” mostró los cuatro restantes en “Página 2 de 2” y “Anterior” volvió a la primera.

**CA-03 · Aviso de copia:** `initialLoadSearchesWithoutFiltersAndFlagsOutdatedCopy`. En el emulador apareció “Aviso: el catálogo puede estar desactualizado…” durante un ciclo real de `SYNCHRONIZING`; la misma ruta devolvió `SYNCHRONIZING` a las 17:12:43 y 17:13:49 y `CURRENT` en las consultas intermedias.

**CA-04 · Vacío válido:** `anEmptyPageIsAValidResultWithoutError`. En el emulador, fecha 2026-11-10 mostró “No se encontraron profesionales con esos filtros.”, sin error ni reintento.

**CA-05 · Indisponibilidad distinta de vacío:** `unavailableCopyIsAnErrorAndRetryRepeatsTheQuery` y `categoriesReportUnavailableCopyWithTheContractCode`. En el emulador, fecha 2026-02-31 fue rechazada por Catálogo y se mostró “Revisá los filtros: hay un valor con formato inválido.”, con resultados descartados y sin botón de reintento.

**CA-06 · Error de transporte y reintento:** `mapsSearchErrorsWithoutRetrying` y `repeatedSearchWhileLoadingSendsOneRequest`. En el emulador se observó “No se pudo conectar con el servicio…” y “Reintentar” recuperó la búsqueda y las categorías.

**CA-07 · Sesión:** `a401ClosesTheSessionThroughTheExistingClient` y `resetDiscardsFiltersAndResultsAndAcceptsANewInitialLoad`. En el emulador, “Salir” volvió al login; al autenticarse de nuevo, los filtros y resultados anteriores no reaparecieron.

**CA-08 · Categorías:** `readsCategoriesIncludingDisabledOnes` y `aCategoriesFailureDoesNotBlockSearchAndTheNextSearchReloadsThem`. En el emulador se ofrecieron las ocho categorías reales; un fallo transitorio mostró el aviso sin bloquear la búsqueda y la siguiente búsqueda las recuperó.

**CA-09 · Navegación y conservación:** en el emulador, la rotación a horizontal conservó el filtro “an” y sus resultados; `force-stop` y reapertura restauraron la sesión y recargaron la búsqueda con filtros por defecto.

**CA-10 · Agenda semanal:** el texto “Filtra por agenda habilitada para ese día de la semana; no garantiza horarios libres.” permanece visible bajo la fecha.

**Regresión:** `./gradlew :shared:testAndroidHostTest` — BUILD SUCCESSFUL, 62 pruebas, 0 fallos: 44 previas y 18 de CU-04 (6 de `KtorCatalogApiTest`, 12 de `SearchViewModelTest`). `:androidApp:assembleDebug` y `:androidApp:lintDebug` — BUILD SUCCESSFUL. `:androidApp:installDebug` — instalado en Pixel_10_Pro (API 37).

**Recorrido real:** Catálogo y Turnos se consultaron con el usuario ficticio `cu04patient` y JWT de usuario, sin credenciales técnicas ni acceso a Redis/Kafka. Catálogo respondió 200 con ocho categorías y 24 profesionales en dos páginas; Turnos respondió 200 al autenticar. Las únicas escrituras fueron el alta de esa cuenta ficticia y su sesión; CU-04 no escribió datos. No se modificaron los backends.

**Selector de fecha (2026-11-09):** «Seleccionar fecha» abrió el calendario con días pasados habilitados; elegir `05/11/2026` y «Buscar» devolvió 3 resultados, «Cancelar» no modificó el valor y «Quitar fecha» volvió a «Seleccionar fecha» conservando los resultados hasta la siguiente búsqueda. `theOptionalDateIsSentInIsoAndClearedWithoutLosingTheOtherFilters` cubre el envío ISO, la limpieza y la conservación de filtros y paginación. Suite host: 163 pruebas, 0 fallos.

**Límites no verificados:** no se forzó una copia inexistente (`CATALOG_COPY_UNAVAILABLE`) ni un fallo permanente de lectura; se cubrieron por pruebas de contrato. El aviso de desactualización se observó con `SYNCHRONIZING`, no con `STALE`, porque el Catálogo real volvió a `CURRENT`. La selección de profesionales y la navegación a CU-05 no se implementaron por estar fuera de alcance. En un arranque, una de las dos consultas iniciales falló una vez por transporte; no fue determinista y el reintento funcionó con los servicios respondiendo 200.

**Desviaciones del PLAN:** no se incorporó Navigation Compose porque el CU tiene un único destino autenticado; se mantiene el `when` por estado de sesión de `App`. La fecha se elige con el calendario nativo de `DatePickerModal` (reutilizado de CU-05) y se muestra como `DD/MM/AAAA`, conservando `AAAA-MM-DD` interno; se permite quitarla porque el filtro es opcional y el CU no restringe fechas pasadas. El botón “Reintentar” se oculta ante un 400, porque repetir el mismo filtro inválido no puede resolverlo. Ninguna cambia el alcance ni los contratos externos.
