# PLAN: Consultar y retomar el proceso propio

**SPEC de referencia:** [SPEC.md](SPEC.md) (implementada y verificada con pruebas; prueba de integración real el 2026-11-09).
**CU e issue:** [CU-08](../../use-cases/CU-08-consultar-proceso.md); issue #11.
**Estado:** Implementado y verificado el 2026-11-08; simplificado el 2026-11-08; prueba real el 2026-11-09.
**Alcance:** consulta del progreso, actualización manual, descubrimiento del pendiente y aviso no bloqueante; sin teléfono ni cancelación.

## Contexto técnico verificado

| Componente o contrato | Ruta verificada | Uso previsto |
| --- | --- | --- |
| `ReservationController.get` | `backend-turnos/.../reservation/infrastructure/web/controller` | `GET /api/reservation-processes/{processId}`; `200` o `404`. |
| `GetReservationProcessUseCaseImpl` | `.../application/usecases` | Progreso durable propio; `404` también para un proceso ajeno. |
| `ReservationController.list` | `.../controller` | `GET /api/reservations?page=0&size=1`; `pending` separado del historial. |
| `ListOwnReservationsUseCaseImpl` | `.../application/usecases` | Un pendiente propio y totales locales. |
| `ReservationProcessResponse` | `.../web/dto` | `state`, `expiresAt`, `reasonCode`, `requiredField`, `canSubmitPhone`. |
| `ReservationPhase` | `.../domain/enums` | Fases informadas; no se replica la máquina de estados. |

**Contrato de lectura:** la fase es durable y local; `expiresAt` es UTC y puede ser `null`; `requiredField` solo indica una acción pendiente. Un `503` de lectura no demuestra ausencia de proceso.

## Solución

1. `ReservationsApi.process(processId)` consume el `GET` por proceso. Solo `404` o `RESERVATION_PROCESS_NOT_FOUND` se traducen a `NOT_FOUND`; el resto es `UNCERTAIN`.
2. `ReservationsApi.pending()` consume `GET /api/reservations?page=0&size=1` y devuelve solo `pending`. No se persiste ni se completa con el historial.
3. `ReservationViewModel.discover()` corre una vez por instancia y solo si el estado sigue intacto; abre el pendiente y `App` lo muestra. Rotar no repite la consulta; `reset()` reinicia la guarda.
4. La pantalla presenta la fase agrupada en textos, el vencimiento UTC y un aviso de teléfono. No calcula vencimientos ni estados.
5. `refresh()` repite el `GET` del proceso conocido. Si falla, conserva el último proceso y muestra el mensaje con “Reintentar”.
6. `App` conserva la visibilidad con `rememberSaveable`. Al salir de un proceso pendiente, la búsqueda muestra un banner “Ver estado”; al pulsarlo consulta Turnos (`refresh` para un proceso conocido, `verify` para uno incierto). Un proceso final o un rechazo se descartan con `dismiss()`.
7. Un fallo de descubrimiento queda en `message` con `start == IDLE`; el banner ofrece “Reintentar” y la búsqueda sigue disponible.

**Sin polling:** la actualización es manual y al abrir el proceso. No hay temporizador ni observación de segundo plano.

**Sin persistencia nueva:** el proceso y su `requestId` viven en el ViewModel. Reabrir la app crea un ViewModel nuevo y consulta Turnos con la sesión cifrada existente.

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

- `discover()` consulta una vez; un fallo se muestra como banner y no como ausencia.
- `refresh()` conserva el proceso anterior ante un fallo; no lo reemplaza por un estado vacío.
- `verify()` de CU-06 usa el mismo `GET` y distingue `404` de un fallo de transporte.
- `dismiss()` descarta un proceso final o un rechazo; conserva un proceso pendiente o incierto. `reset()` descarta todo al cerrar sesión.
- `expiresAt` se presenta como `AAAA-MM-DD HH:mm:ss UTC`, sin compararlo con el reloj local.
- El teléfono solo se anuncia; `canSubmitPhone` no habilita un envío en esta funcionalidad.
- Solo un `404` o el código `RESERVATION_PROCESS_NOT_FOUND` acredita inexistencia; `400`, `403` y `5xx` no habilitan repetir el inicio.

## Dependencias y configuración

No se agregan dependencias ni configuración. Se reutilizan Ktor, kotlinx.serialization, Compose Material 1, la sesión cifrada y `TURNOS_BASE_URL`.

## Estrategia de validación

| Criterio | Método | Evidencia |
| --- | --- | --- |
| CA-01 | `KtorReservationsApiTest` | Ruta, bearer y ausencia de POST. |
| CA-02 | `KtorReservationsApiTest`, `ReservationViewModelTest` | Fases y campos presentados sin completar ausentes. |
| CA-03 | `ProcessPresentationTest`; emulador | `expiresAt` rotulado UTC; sin comparación local. |
| CA-04 | `ReservationViewModelTest`; emulador | Aviso de teléfono sin campo de envío. |
| CA-05 | `ReservationViewModelTest` | Actualización exitosa y fallo que conserva el proceso. |
| CA-06 | `KtorReservationsApiTest`, `ReservationViewModelTest`; emulador | `pending` abre el proceso; ausencia deja búsqueda. |
| CA-07 | `ReservationViewModelTest`; emulador | Fallo de descubrimiento no bloquea y “Reintentar” repite. |
| CA-08 | `ReservationViewModelTest`; emulador | Salir deja banner “Ver estado”, sin cancelar. |
| CA-09 | `ReservationViewModelTest` | Respuesta tardía tras `reset` no publica. |
| CA-10 | Revisión de dependencias y código | Sin almacenamiento nuevo, polling ni reconciliación propia. |

**Comandos:** `./gradlew :shared:testAndroidHostTest`, `./gradlew :androidApp:assembleDebug :androidApp:lintDebug`, `./gradlew :androidApp:installDebug`.

**Limitaciones previstas:** la prueba real del 2026-11-09 observó `pending`, `WAITING_FOR_PHONE`, el aviso de teléfono y la recuperación al reiniciar. El código HTTP del `POST` y el del primer `GET` fallido no se registraron. No se ejecutó CU-07 ni CU-10.

## Orden de implementación

1. Contrato de consulta y pendiente con pruebas.
2. Estado, descubrimiento y actualización con pruebas.
3. Pantalla, banner y cableado.
4. Pruebas, build, lint y recorrido de solo lectura.

## Riesgos y decisiones pendientes

- **Riesgos:** el descubrimiento depende de que Turnos responda; un fallo se informa y no se interpreta como ausencia. La sesión puede vencer entre la consulta y la actualización; el cliente protegido mantiene el comportamiento existente.
- **Decisiones pendientes:** ninguna de comportamiento. El teléfono y la cancelación pertenecen a CU-07 y CU-10.
