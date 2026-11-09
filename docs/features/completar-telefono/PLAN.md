# PLAN: Completar el teléfono solicitado

**SPEC de referencia:** [SPEC.md](SPEC.md) (implementada y verificada con pruebas; envío real no ejecutado).
**CU e issue:** [CU-07](../../use-cases/CU-07-completar-informacion.md); issue #10.
**Estado:** Implementado el 2026-11-09; pendiente de prueba real autorizada.
**Alcance:** envío del teléfono, corrección de rechazos y verificación de resultados inciertos; sin cancelación ni historial.

## Contexto técnico verificado

| Componente o contrato | Ruta verificada | Uso previsto |
| --- | --- | --- |
| `ReservationController.phone` | `backend-turnos/.../reservation/infrastructure/web/controller` | `POST /api/reservation-processes/{processId}/phone`; `202` tras aceptar, `200` por repetición. |
| `SubmitReservationPhoneUseCaseImpl` | `.../application/usecases` | Propiedad, normalización, idempotencia y errores del envío. |
| `TransactionalReservationStore.submitPhone` | `.../infrastructure/persistence/service` | Acepta proceso y respuesta juntos; replay equivalente y conflicto de clave. |
| `PhoneSubmission.normalize` | `.../domain/model` | Quita espacios, guiones, paréntesis y puntos; valida `^\+?[0-9]{7,15}$`. |
| `ReservationDtoMapper` | `.../infrastructure/web/mapper` | `requiredField`, `canSubmitPhone` y respuesta del proceso. |
| `ReservationsApi`/`KtorReservationsApi` | `shared/.../reservation/data` | Contrato HTTP y mapeo de `code`/`processId` existentes. |
| `ReservationViewModel`, `ReservationUiState`, `ProcessScreen` | `shared/.../ui/reservation` | Estado único, acciones y presentación de CU-06/CU-08. |

**Contrato de envío:** cuerpo `{requestId, phoneNumber}`; `202` nuevo y `200` repetición equivalente. `canSubmitPhone` exige fase `WAITING_FOR_PHONE`, hold vigente, `requestEventId` conocido y ausencia de un envío activo. La respuesta puede informar `state=PHONE_QUEUED` con `canSubmitPhone=false`; no confirma la reserva.

## Solución

1. `ReservationsApi` incorpora `SubmitPhone` y `submitPhone(processId, request)`. `KtorReservationsApi` usa el `post` autenticado existente, acepta `200/202` y mapea los códigos del contrato en `REJECTED`, `NOT_FOUND` o `UNCERTAIN`.
2. El ViewModel normaliza con la misma regla del backend, genera un `requestId` nuevo por envío y rechaza llamadas mientras hay otro envío o lectura en curso. Reutiliza el `job` y la cancelación de respuestas tardías.
3. `ReservationUiState` agrega `phoneNumber`, `PhoneStatus` (`IDLE`, `SENDING`, `UNCERTAIN`) y `phoneMessage`. `canSubmitPhone` se deriva de `process.canSubmitPhone`, el estado local y `loading`, de modo que `requiredField` solo no habilita el formulario.
4. Un POST exitoso actualiza el proceso con `show()` y limpia el texto enviado. Un rechazo con `processId` se contrasta con `GET` y conserva el mensaje para corregir. Un resultado incierto deja `PhoneStatus.UNCERTAIN`; “Verificar estado” ejecuta el `GET` existente y solo una lectura exitosa permite volver a enviar.
5. `ProcessScreen` muestra el formulario solo con `process.canSubmitPhone`, distingue “enviando”, “teléfono enviado, resultado pendiente” e incertidumbre, y mantiene el aviso de vencimiento informado por Turnos.
6. `App` pasa las tres acciones nuevas al `ProcessScreen`; no se agrega navegación ni cableado adicional.

## Componentes afectados

| Componente o ruta | Acción | Responsabilidad | Requisito |
| --- | --- | --- | --- |
| `reservation/data/ReservationsApi.kt` | Modificar | Cuerpo y contrato del teléfono. | RF-03 |
| `reservation/data/KtorReservationsApi.kt` | Modificar | POST, códigos y lectura del proceso actualizado. | RF-03, RF-05, RF-06 |
| `ui/reservation/ReservationUiState.kt` | Modificar | Texto, estado del envío y habilitación real. | RF-01, RF-04, RF-07, RF-09 |
| `ui/reservation/ReservationViewModel.kt` | Modificar | Validación, envío único, reconciliación y verificación. | RF-01–RF-06 |
| `ui/reservation/ProcessScreen.kt` | Modificar | Formulario, envío y mensajes del teléfono. | RF-01, RF-04, RF-05, RF-07, RF-08 |
| `ui/reservation/ProcessPresentation.kt` | Modificar | Avisos de teléfono enviado o no habilitado. | RF-07, RF-08 |
| `App.kt` | Modificar | Cableado de las acciones del formulario. | RF-01, RF-03 |

## Estado, operaciones y errores

- `PhoneStatus`: `IDLE` (formulario disponible si Turnos lo habilita), `SENDING` (POST en curso, botón bloqueado) y `UNCERTAIN` (no se repite sin verificar).
- `submitPhone()` solo actúa con un proceso conocido, `canSubmitPhone=true`, `PhoneStatus.IDLE` y sin otra operación; valida el teléfono antes de generar el `requestId`.
- `verifyPhone()` usa `GET /api/reservation-processes/{processId}`; una lectura exitosa reemplaza el proceso y limpia la incertidumbre local.
- Un rechazo se reconcilia con `GET`; si esa lectura falla, se conserva el proceso y el mensaje del rechazo.
- `reset()` cancela el job y descarta texto, mensajes y estado del proceso.

## Dependencias y configuración

No se agregan dependencias ni configuración. Se reutilizan Ktor, kotlinx.serialization, Compose Material 1, el cliente autenticado y `TURNOS_BASE_URL`.

## Estrategia de validación

| Criterio | Método | Evidencia |
| --- | --- | --- |
| CA-01 | `ReservationViewModelTest` | `canSubmitPhone` falso con `requiredField=PHONE_NUMBER` no habilita el envío. |
| CA-02 | `ReservationViewModelTest` | Normalización de separadores y valor inválido sin POST. |
| CA-03 | `KtorReservationsApiTest` | Método, ruta, bearer y cuerpo exactos. |
| CA-04 | `ReservationViewModelTest` | Dos envíos durante el POST producen una sola llamada. |
| CA-05 | `KtorReservationsApiTest`, `ReservationViewModelTest` | Códigos y transporte clasificados; `503` y rechazo presentados correctamente. |
| CA-06 | `ReservationViewModelTest` | POST exitoso actualiza; rechazo consulta; incertidumbre verifica antes de repetir. |
| CA-07 | `ProcessPresentationTest`, `ReservationViewModelTest` | `PHONE_QUEUED`/`WAITING_FOR_RESULT` no son confirmación. |
| CA-08 | `ReservationViewModelTest`, `ProcessPresentationTest` | Sin `canSubmitPhone` no hay formulario ni aviso de envío posible. |
| CA-09 | `ReservationViewModelTest` | El estado local se descarta con `reset()` y una respuesta tardía no publica. |

**Comandos:** `./gradlew :shared:testAndroidHostTest`, `./gradlew :androidApp:assembleDebug :androidApp:lintDebug`.

**Limitaciones previstas:** no se crean holds ni se envían teléfonos reales sin autorización. El recorrido de emulador solo puede comprobar que un proceso vencido no ofrece el formulario; el envío, los rechazos y la incertidumbre se cubren con dobles.

## Orden de implementación

1. Contrato, DTO y mapeo de errores con pruebas.
2. Estado y ViewModel con validación, envío único y verificación.
3. Formulario, avisos y cableado.
4. Suites, build, lint y recorrido de solo lectura en emulador.

## Riesgos y decisiones pendientes

- **Riesgos:** un `503` no distingue si el envío quedó registrado; por eso no se repite sin `GET`. La respuesta inmediata puede no incluir el requisito de teléfono, y la app solo actúa sobre `canSubmitPhone` recibido.
- **Decisiones pendientes:** ninguna de comportamiento. CU-09 y CU-10 quedan fuera de alcance.
