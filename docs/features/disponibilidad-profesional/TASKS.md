# TASKS: Consultar disponibilidad de un profesional

**SPEC:** [SPEC.md](SPEC.md) · **PLAN:** [PLAN.md](PLAN.md) · **CU:** [CU-05](../../use-cases/CU-05-consultar-disponibilidad.md) · **Issue:** #8
**Estado:** Completada y verificada el 2026-11-08.

## Coherencia previa

Cada RF tiene CA y validación en el PLAN; cada tarea referencia sus RF/CA y no agrega alcance. Sin decisiones bloqueantes.

## Tareas

- [x] **T1 · Contrato de consulta y errores** (RF-03, RF-06)
  - `AvailabilityApi`, `KtorAvailabilityApi`, `AvailabilityError`, `AvailabilityException`.
  - Depende de: contrato verificado de Turnos.
  - Validación: `KtorAvailabilityApiTest` (4 pruebas).
- [x] **T2 · Selección desde búsqueda y vuelta** (RF-01, RF-08, RF-09)
  - `SearchUiState.selected`, `select`, `backToSearch`, fila clickable en `SearchScreen`.
  - Depende de: T1 no; CU-04.
  - Validación: dos pruebas nuevas en `SearchViewModelTest` (preservación de filtros/página/resultados y reset).
- [x] **T3 · Estado y consulta** (RF-02–RF-07, RF-10)
  - `AvailabilityUiState`, `AvailabilityViewModel`: validación de forma, consulta, reintento, marca visual de un horario ofrecido, descarte de respuestas tras cambio de fecha y `reset`.
  - Depende de: T1.
  - Validación: `AvailabilityViewModelTest` (10 pruebas).
- [x] **T4 · Pantalla y cableado** (RF-01, RF-04, RF-05, RF-09, RF-10)
  - `AvailabilityScreen`, selector visual en los horarios, destino condicional en `App`, `AppContainer` y `MainActivity`.
  - Depende de: T2, T3.
  - Validación: build, lint y recorrido real.
- [x] **T5 · Verificación integral** (CA-01–CA-10)
  - Suite host, build, lint y recorrido en emulador contra Catálogo y Turnos.
  - Depende de: T1–T4.
- [x] **T6 · Campo obligatorio en la respuesta** (RF-05)
  - `availableStartTimes` sin valor por defecto; un 200 sin el campo produce error, no una lista vacía.
  - Depende de: T1.
  - Validación: regresión en `KtorAvailabilityApiTest` (5 pruebas).
- [x] **T7 · Selector de fecha nativo** (RF-02, RF-04, RF-06)
  - `DatePickerModal` (`expect`) y su implementación Android con `DatePickerDialog`; botón «Seleccionar fecha», fecha visible `DD/MM/AAAA` e internamente `AAAA-MM-DD`. `minDate` es hoy; cancelar no modifica. Se reutiliza `AvailabilityViewModel.changeDate()`, que descarta horarios y selección. El mismo componente, con `allowPastDates`, sirve al filtro de CU-04 sin agregar otro selector.
  - Depende de: T3.
  - Validación: `DatePickerPresentationTest` (2 pruebas) y emulador: elegir `10/11/2026`, cancelar sin cambios, rechazo de un día pasado y cambio a `11/11/2026` que invalida los horarios y la marca.

## Cierre

### CA y evidencia

| CA | Estado | Evidencia |
| --- | --- | --- |
| CA-01 | Verificado | `SearchViewModelTest`; emulador: «Ana» → selección de Ana Torres abre su disponibilidad y «Volver» regresa con el filtro y los 3 resultados. |
| CA-02 | Verificado | `KtorAvailabilityApiTest`: host de Turnos, ruta, `professionalId`, `date` y bearer de usuario; sin holds ni procesos. |
| CA-03 | Verificado | `KtorAvailabilityApiTest` y `AvailabilityViewModelTest`; emulador: `12:00:00`… se muestran como `12:00`, `12:30`, `13:30`, `14:00`, `14:30` en el orden recibido. |
| CA-04 | Verificado | `AvailabilityViewModelTest`; emulador `2026-11-10`: «No hay horarios disponibles para esa fecha.», sin error. |
| CA-05 | Verificado | `AvailabilityViewModelTest`; emulador con Turnos pausado: error de conexión y «Reintentar» devuelve los horarios al reactivarlo. |
| CA-06 | Verificado | `KtorAvailabilityApiTest` (400/404 no reintentables, 401 cierra sesión); emulador: fecha pasada informa corrección sin reintento; profesional deshabilitado (Mariana Luna) informa «no habilitado» sin reintento. |
| CA-07 | Verificado | `AvailabilityViewModelTest`: cambiar la fecha cancela y descarta la respuesta pendiente; el reintento usa la fecha vigente. |
| CA-08 | Verificado | `SearchViewModelTest`; emulador: rotación conserva profesional, fecha, horarios y horario marcado; «Volver» conserva búsqueda; «Cerrar sesión» (pestaña Cuenta) vuelve al login y un nuevo inicio parte de filtros vacíos. |
| CA-09 | Verificado | Emulador: la pantalla advierte que consultar no reserva y que iniciar la reserva pertenece al paso siguiente. |
| CA-10 | Verificado | `AvailabilityViewModelTest` (marca, rechazo de un horario no ofrecido, conservación al rotar y descarte al cambiar fecha); `KtorAvailabilityApiTest` (campo obligatorio); emulador: «12:30» queda marcado como «Horario elegido… Todavía no está reservado» y la marca desaparece al cambiar a `2026-11-10`. |

### Regresión

- `./gradlew :shared:testAndroidHostTest` → BUILD SUCCESSFUL, 79 pruebas, 0 fallos (2026-11-08).
- `./gradlew :androidApp:assembleDebug :androidApp:lintDebug` → BUILD SUCCESSFUL; lint sin errores y con 5 avisos previos de versiones y `ObsoleteSdkInt`.
- Recorrido real en emulador Pixel 10 Pro (API 37) contra Catálogo y Turnos locales: login, búsqueda, selección, consulta con y sin horarios, marca de un horario, rotación con la marca conservada, cambio de fecha que la descarta, errores 400 y de transporte, reintento, vuelta y cierre de sesión.

### Desviaciones del PLAN

Ninguna. La vuelta desde disponibilidad usa el `NavController` (`popBackStack`): «Volver» y Atrás regresan a Buscar sin cerrar la Activity. Límites observados: el `401` y el `503 CATALOG_NOT_CURRENT` se cubren con pruebas, no se forzaron en el recorrido por no exponer el backend un mecanismo determinista.
