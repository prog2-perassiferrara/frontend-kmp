# PLAN: Consultar y retomar el proceso propio

**SPEC de referencia:** [SPEC.md](SPEC.md) (implementada y verificada con pruebas; prueba de integración real el 2026-11-09).
**CU e issue:** [CU-08](../../use-cases/CU-08-consultar-proceso.md); issue #11.
**Estado:** Implementado y verificado el 2026-11-08; simplificado el 2026-11-08; prueba real el 2026-11-09.
**Alcance:** consulta del progreso, actualización manual y acceso al pendiente desde Cuenta; sin teléfono ni cancelación.

## Contexto técnico verificado

| Componente o contrato | Ruta verificada | Uso previsto |
| --- | --- | --- |
| `ReservationController.get` | `backend-turnos/.../reservation/infrastructure/web/controller` | `GET /api/reservation-processes/{processId}`; `200` o `404`. |
| `GetReservationProcessUseCaseImpl` | `.../application/usecases` | Progreso durable propio; `404` también para un proceso ajeno. |
| `ReservationController.list` | `.../controller` | `GET /api/reservations?page=0&size=1`; `pending` separado del historial. |
| `ListOwnReservationsUseCaseImpl` | `.../application/usecases` | Un pendiente propio y totales locales. |
| `ReservationProcessResponse` | `.../web/dto` | `state`, `expiresAt`, `reasonCode`, `requiredField`, `canSubmitPhone`. |
| `ReservationPhase` | `.../domain/enums` | Fases informadas; no se replica la máquina de estados. |

**Contrato de lectura:** la fase es durable y local; `expiresAt` es un instante UTC y puede ser `null`; `requiredField` solo indica una acción pendiente. Un `503` de lectura no demuestra ausencia de proceso.

## Solución

1. `ReservationsApi.process(processId)` consume el `GET` por proceso. Solo `404` o `RESERVATION_PROCESS_NOT_FOUND` se traducen a `NOT_FOUND`; el resto es `UNCERTAIN`.
2. Cuenta usa `ReservationsApi.reservations(...)` y muestra el pendiente separado del historial.
3. Al tocar el pendiente en Cuenta, `MainScreen` abre el proceso con `ReservationViewModel.present(...)`.
4. La pantalla presenta la fase agrupada en textos, el vencimiento convertido a la zona de atención y un aviso de teléfono. No calcula vencimientos ni estados.
5. `MainScreen` usa el destino anterior de Navigation Compose y el resultado del inicio para determinar el regreso desde Reserva. La pantalla y el sistema invocan la misma acción.
5. `refresh()` repite el `GET` del proceso conocido. Si falla, conserva el último proceso y muestra el mensaje con “Reintentar”.
6. Buscar ofrece verificar por UUID un inicio incierto. `dismiss()` conserva procesos abiertos e intentos que requieren seguimiento.
7. Un fallo al listar en Cuenta muestra el error y permite actualizar.

**Sin polling:** la actualización es manual y al abrir el proceso. No hay temporizador ni observación de segundo plano.

**Sin persistencia nueva:** el proceso y su `requestId` viven en el ViewModel. Al reabrir la app, Cuenta puede volver a consultar Turnos con la sesión cifrada existente.

**Respuestas tardías:** cada operación cancela el job anterior; `reset()` cancela el job y descarta el estado. No se agrega un token adicional. Las pruebas cubren inicio y actualización tardíos.

## Componentes afectados

| Componente o ruta | Acción | Responsabilidad | Requisito |
| --- | --- | --- | --- |
| `reservation/data/ReservationsApi.kt` | Crear | Contrato de consulta y pendiente. | RF-01, RF-06 |
| `reservation/data/KtorReservationsApi.kt` | Crear | `GET` por proceso, `GET` de reservas y mapeo de errores. | RF-01, RF-05, RF-06 |
| `ui/reservation/ReservationUiState.kt` | Crear | Proceso, estado del inicio, mensaje y banner. | RF-02–RF-08 |
| `ui/reservation/ReservationViewModel.kt` | Crear | Descubrimiento, actualización y descarte. | RF-05–RF-09 |
| `ui/reservation/ProcessScreen.kt` | Crear | Fase, vencimiento, teléfono y acciones. | RF-02–RF-05 |
| `ui/reservation/ReservationBanner.kt` | Crear | Aviso no bloqueante en búsqueda. | RF-07, RF-08 |
| `App.kt` | Modificar | Visibilidad del proceso, banner y descubrimiento al login. | RF-06–RF-09 |
| `AppContainer.kt`, `MainActivity.kt` | Modificar | Cableado de la API y las pantallas. | RF-06 |

## Datos y contratos

- **Entrada:** `processId` UUID propio; sin fechas ni filtros adicionales.
- **Salida:** el proceso durable descrito en `ReservationProcessResponse`; `pending` puede ser `null`.
- **Productor:** Turnos; KMP solo presenta y actualiza. La reconciliación REST/Kafka sigue siendo de Turnos.
- **Persistencia:** ninguna nueva. La sesión cifrada existente es la única continuidad entre procesos Android.

## Estado, operaciones y errores

- La consulta de pendientes se realiza al abrir Cuenta.
- `refresh()` conserva el proceso anterior ante un fallo; no lo reemplaza por un estado vacío.
- `refresh()` usa el mismo `GET` para el proceso conocido y el inicio incierto. Solo el `404` del UUID original del inicio permite repetirlo.
- `dismiss()` descarta un proceso final o un rechazo; conserva un proceso pendiente o incierto. `reset()` descarta todo al cerrar sesión.
- `expiresAt` se presenta como `DD/MM/AAAA · HH:mm` mediante `formatInstantInZone` con `ATTENTION_TIME_ZONE` (`America/Argentina/Buenos_Aires`), la misma zona de las horas locales de atención, sin compararlo con el reloj del dispositivo ni modificar el valor recibido.
- El teléfono solo se anuncia; `canSubmitPhone` no habilita un envío en esta funcionalidad.
- Solo un `404` o el código `RESERVATION_PROCESS_NOT_FOUND` acredita inexistencia; `400`, `403` y `5xx` no habilitan repetir el inicio.

## Dependencias y configuración

No se agregan dependencias ni configuración. Se reutilizan Ktor, kotlinx.serialization, Compose Material 1, la sesión cifrada y `TURNOS_BASE_URL`.

## Estrategia de validación

| Criterio | Método | Evidencia |
| --- | --- | --- |
| CA-01 | `KtorReservationsApiTest` | Ruta, bearer y ausencia de POST. |
| CA-02 | `KtorReservationsApiTest`, `ReservationViewModelTest` | Fases y campos presentados sin completar ausentes. |
| CA-03 | `ProcessPresentationTest`, `InstantFormatTest` | `expiresAt` convertido de UTC a la zona de atención, con cambio de día cuando corresponde; sin comparación con el reloj local. |
| CA-04 | `ReservationViewModelTest`; emulador | Aviso de teléfono sin campo de envío. |
| CA-05 | `ReservationViewModelTest` | Actualización exitosa y fallo que conserva el proceso. |
| CA-06 | `MyReservationsViewModelTest`; emulador | Cuenta recibe el pendiente y permite abrirlo. |
| CA-07 | `MyReservationsViewModelTest`; emulador | Un error de listado permite actualizar. |
| CA-08 | `ReservationViewModelTest`; emulador | Se conserva el pendiente al salir y Buscar ofrece verificar un inicio incierto. |
| CA-09 | `ReservationViewModelTest` | Respuesta tardía tras `reset` no publica. |
| CA-10 | Revisión de dependencias y código | Sin almacenamiento nuevo, polling ni reconciliación propia. |

**Comandos:** `./gradlew :shared:testAndroidHostTest`, `./gradlew :androidApp:assembleDebug :androidApp:lintDebug`, `./gradlew :androidApp:installDebug`.

**Limitaciones previstas:** la prueba real del 2026-11-09 observó `pending`, `WAITING_FOR_PHONE`, el aviso de teléfono y la recuperación al reiniciar. El código HTTP del `POST` y el del primer `GET` fallido no se registraron. No se ejecutó CU-07 ni CU-10.

## Orden de implementación

1. Contrato de consulta y pendiente con pruebas.
2. Estado, apertura desde Cuenta y actualización con pruebas.
3. Pantalla y cableado.
4. Pruebas, build, lint y recorrido de solo lectura.

## Riesgos y decisiones pendientes

- **Riesgos:** un fallo de listado se informa sin inferir ausencia del pendiente. El cliente protegido conserva el tratamiento de sesión vencida.
- **Decisiones pendientes:** ninguna de comportamiento. El teléfono y la cancelación pertenecen a CU-07 y CU-10.
