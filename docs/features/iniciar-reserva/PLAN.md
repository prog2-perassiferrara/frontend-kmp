# PLAN: Iniciar una reserva

**SPEC de referencia:** [SPEC.md](SPEC.md) (implementada y verificada con pruebas; prueba de integración real el 2026-11-09).
**CU e issue:** [CU-06](../../use-cases/CU-06-iniciar-reserva.md); issue #9.
**Estado:** Implementado y verificado el 2026-11-08; simplificado el 2026-11-08; prueba real el 2026-11-09.
**Alcance:** envío idempotente del inicio, interpretación de rechazos e incertidumbre y presentación del proceso; sin teléfono ni cancelación.

## Contexto técnico verificado

| Componente o contrato | Ruta verificada | Uso previsto |
| --- | --- | --- |
| `ReservationController.start` | `backend-turnos/.../reservation/infrastructure/web/controller` | `POST /api/reservation-processes`; `202` nuevo, `200` repetición propia. |
| `StartReservationUseCaseImpl` | `.../application/usecases` | `requestId` idempotente, un pendiente por usuario, replay sin repetir efectos, cierre incierto con `processId`. |
| `ReservationProblemHandler` | `.../infrastructure/web` | `code`, `processId` y `recheckAt`; `400/409/503/500` sin cuerpos internos. |
| `ReservationProcessResponse` | `.../web/dto` | `processId`, selección, `state`, `expiresAt`, `reasonCode`, `requiredField`, `canSubmitPhone`. |
| `AuthenticatedApiClient` | `shared/.../network` | Bearer de usuario, descarte de respuestas tardías y cierre ante `401`. Se agrega `post`. |
| `AvailabilityUiState` | `shared/.../ui/availability` | Profesional, fecha y horario marcado que alimentan el inicio. |

**Contrato de inicio:** cuerpo `{requestId, professionalId, date, startTime}`; el `requestId` es el UUID local y coincide con el `processId`. `ACTIVE_RESERVATION_PROCESS`, `SLOT_NOT_AVAILABLE`, `HOLD_EXPIRED`, `CONFIRMATION_REJECTED`, `HOLD_CREATION_UNCERTAIN` y `CONFIRMATION_UNCERTAIN` pueden incluir `processId`. Un `503` no afirma rollback.

## Solución

1. `AuthenticatedApiClient.post` replica las garantías del `get`: bearer, `ApiResponse` sin cuerpo imprimible y `401` que cierra la sesión. Un fallo de transporte se traduce en `SessionException(UNAVAILABLE)`.
2. `ReservationsApi`/`KtorReservationsApi` encapsulan inicio, consulta y pendiente. `start` devuelve directamente el `ReservationProcess`; KMP no crea holds ni reproduce la máquina de estados de Turnos.
3. `ReservationException` clasifica en `REJECTED`, `NOT_FOUND` o `UNCERTAIN` y conserva un mensaje presentable y el `processId` opcional. El mapeo de `code` queda en el cliente HTTP, que conoce el contrato.
4. Un único `ReservationUiState` contiene el proceso, el estado del inicio (`StartStatus`), la selección, el `requestId`, la carga y un mensaje. La fase mostrada siempre proviene de Turnos.
5. `ReservationViewModel.start` genera el `requestId` una vez y solo se acepta desde `IDLE` y sin proceso. `retryStart()` reutiliza la clave solo desde `NOT_REGISTERED`.
6. Un error con `processId` se resuelve con `GET /api/reservation-processes/{processId}` y se presenta como proceso, con el rechazo como mensaje. Si ese `GET` falla, el estado queda `UNCERTAIN` y verificable: `ACTIVE_RESERVATION_PROCESS` no se convierte en rechazo definitivo.
7. Ante incertidumbre, “Verificar estado” consulta por `requestId`. Solo `404` o `RESERVATION_PROCESS_NOT_FOUND` habilitan repetir la misma solicitud.
8. `ProcessScreen` informa el envío, el rechazo, la incertidumbre y la fase recibida. No envía teléfono: solo avisa cuando Turnos lo requiere.

**Navegación:** `App` conserva `processVisible` con `rememberSaveable` y abre el proceso cuando Turnos informa uno. Salir no cancela; `dismiss()` descarta solo un proceso final o un rechazo/no registrado, y conserva uno pendiente o incierto para el banner.

**Invalidación:** cada operación cancela el job anterior; `reset()` cancela el job y descarta el estado. Las pruebas cubren una respuesta tardía tras `reset` y una respuesta de una operación anterior. No se mantiene un token adicional.

**Concurrencia de UI:** el botón se reemplaza por el proceso al iniciar y el ViewModel ignora otra llamada mientras `start != IDLE` o hay proceso. Es una guarda de presentación, no un reemplazo del control de Turnos.

## Componentes afectados

| Componente o ruta | Acción | Responsabilidad | Requisito |
| --- | --- | --- | --- |
| `network/AuthenticatedApiClient.kt` | Modificar | `post` autenticado con las garantías existentes. | RF-01 |
| `reservation/ReservationFailure.kt` | Crear | Clasificación del fallo y excepción presentable. | RF-05–RF-08 |
| `reservation/data/ReservationsApi.kt` | Crear | Modelos del proceso y contratos de inicio, consulta y pendiente. | RF-01, RF-03, RF-04 |
| `reservation/data/KtorReservationsApi.kt` | Crear | HTTP, cuerpo, estados y lectura de `code`/`processId`. | RF-01–RF-08 |
| `ui/reservation/ReservationUiState.kt` | Crear | Proceso, estado del inicio, selección, mensaje y banner. | RF-02–RF-09 |
| `ui/reservation/ReservationViewModel.kt` | Crear | Inicio, verificación, reintento, descubrimiento y descarte. | RF-02–RF-09 |
| `ui/reservation/ProcessScreen.kt` | Crear | Presentación del proceso y acciones permitidas. | RF-03, RF-05–RF-10 |
| `ui/availability/AvailabilityScreen.kt` | Modificar | Acción “Solicitar reserva” y aviso de bloqueo. | RF-01, RF-02 |
| `App.kt`, `MainActivity.kt`, `AppContainer.kt` | Modificar | Visibilidad del proceso, banner y cableado. | RF-09 |

## Datos y contratos

- **Entrada:** `requestId` UUID, `professionalId` positivo, `date` ISO y `startTime` `HH:mm[:ss]`; el horario proviene de la lista de CU-05.
- **Salida:** proceso con `processId`, selección, `state`, `expiresAt`, `reasonCode`, `requiredField`, `canSubmitPhone` y nombres históricos opcionales.
- **Persistencia:** ninguna. El `requestId` y el proceso viven en el ViewModel; reabrir el proceso Android se resuelve con `GET /api/reservations`.
- **Errores leídos por HTTP y `code`**, nunca por `detail`.

## Estado, operaciones y errores

- `StartStatus`: `IDLE`, `SENDING`, `UNCERTAIN`, `NOT_REGISTERED` y `REJECTED`. Con un proceso conocido, `process` tiene prioridad sobre el estado del inicio.
- `start(selection)` crea un estado nuevo con `requestId` y envía; `retryStart()` solo desde `NOT_REGISTERED`.
- `verify()` consulta una incertidumbre; `refresh()` actualiza un proceso conocido; ambos usan `GET` y comparten la guarda `loading`.
- `404` de verificación → `NOT_REGISTERED`; cualquier otro fallo de verificación mantiene `UNCERTAIN`.
- Un fallo de `refresh` conserva el último proceso y muestra el mensaje.
- Una respuesta tardía se descarta porque `reset()`/una operación nueva cancela el job anterior.
- `dismiss()` descarta un proceso final o un rechazo/no registrado; no cancela ni abandona el proceso en Turnos.

## Dependencias y configuración

No se agregan dependencias ni configuración. Se reutilizan Ktor, kotlinx.serialization, Compose Material 1, `TURNOS_BASE_URL` y el cliente autenticado.

## Estrategia de validación

| Criterio | Método | Evidencia |
| --- | --- | --- |
| CA-01 | `KtorReservationsApiTest` | Método, ruta, bearer y cuerpo exactos. |
| CA-02 | `ReservationViewModelTest` | Dos `start` durante el envío producen una sola llamada. |
| CA-03 | `KtorReservationsApiTest`, `ReservationViewModelTest` | `202/200` y fases presentadas sin confirmación implícita. |
| CA-04 | `KtorReservationsApiTest`, `ReservationViewModelTest` | `requestId` igual al `processId`; reintento con la misma clave. |
| CA-05 | `KtorReservationsApiTest`, `ReservationViewModelTest` | `400/409` mapeados y sin segundo POST. |
| CA-06 | `ReservationViewModelTest` | Error con `processId` dispara `GET` y muestra el proceso; si el `GET` falla, queda verificable. |
| CA-07 | `KtorReservationsApiTest`, `ReservationViewModelTest` | `503`, `5xx` y transporte producen `UNCERTAIN`. |
| CA-08 | `ReservationViewModelTest` | `404` de verificación → `NOT_REGISTERED`; “Reintentar” conserva el `requestId`. |
| CA-09 | `ReservationViewModelTest`; emulador | Salir conserva el proceso y el banner ofrece “Ver estado”. |
| CA-10 | `ReservationViewModelTest`; emulador | `requiredField`/`canSubmitPhone` solo se anuncian. |

**Comandos:** `./gradlew :shared:testAndroidHostTest`, `./gradlew :androidApp:assembleDebug :androidApp:lintDebug`, `./gradlew :androidApp:installDebug`.

**Limitaciones previstas:** la prueba real del 2026-11-09 creó un proceso con hold y fase `WAITING_FOR_PHONE`, sin confirmar. No se capturó el código HTTP del `POST`; no se envió teléfono ni se canceló. Los rechazos y la incertidumbre se cubren con pruebas de contrato y proveedores ficticios.

## Orden de implementación

1. `post`, contrato HTTP, modelos y errores con pruebas.
2. ViewModel de inicio, verificación, reintento y descubrimiento con pruebas.
3. Pantalla, botón de disponibilidad, banner y cableado.
4. Pruebas, build, lint y recorrido de solo lectura.

## Riesgos y decisiones pendientes

- **Riesgos:** un `503` no distingue por sí solo si Turnos alcanzó a registrar el proceso; por eso no se reintenta sin verificar. Un `processId` en un error ajeno se consulta solo con el JWT del usuario y un `404` no expone datos.
- **Decisiones pendientes:** ninguna de comportamiento. El envío de teléfono y la cancelación pertenecen a CU-07 y CU-10.
