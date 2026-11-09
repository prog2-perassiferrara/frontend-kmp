# PLAN: Cancelar reserva propia

**SPEC:** [SPEC.md](SPEC.md) · **CU:** [CU-10](../../use-cases/CU-10-cancelar-reserva.md) · **Issue:** #12
**Estado:** Implementado el 2026-11-09.

## Contexto verificado

| Contrato o regla | Uso |
| --- | --- |
| `POST /api/reservation-processes/{processId}/cancel` (`ReservationController.cancel`) | Sin body; `202` si la intención queda `PENDING`, `200` en otro caso. |
| `ReservationProcess.requestCancellation` | Solo `CONFIRMED` con hold; `CANCELLED` y `PENDING` devuelven el mismo estado. |
| `RecoverReservationCancellationsUseCaseImpl` | Reintenta centralmente; puede devolver `RETRY_REQUIRED` con `canRetry=true`. |
| `ReservationDtoMapper.cancellation` | `cancellation {status, canRetry}`; solo `CANCELLED` acredita el cierre. |
| Errores del contrato | `404 RESERVATION_PROCESS_NOT_FOUND`, `409 CANCELLATION_NOT_ALLOWED`, `403`, `503 CANCELLATION_REJECTED/UNCERTAIN/STORAGE_UNAVAILABLE`. |
| `AuthenticatedApiClient.post` | Hoy exige cuerpo; se admite `null` para el POST sin body. |

## Solución

1. `ReservationsApi.cancel(processId)` usa POST sin cuerpo y acepta `200/202`. `KtorReservationsApi` mapea `404`, `409`, `403`, `5xx`/transporte y respuestas ilegibles a `NOT_FOUND`, `REJECTED` o `UNCERTAIN`.
2. `MyReservationsViewModel` conserva una única operación de cancelación (`cancellingId`), evita duplicados y actualiza la fila con la respuesta. Un `RETRY_REQUIRED` habilita reintentar; `CANCELLED` deshabilita la acción.
3. Ante `UNCERTAIN`, el ViewModel marca `cancelBlockedId`, conserva la reserva y ejecuta un GET del listado. Solo una lectura exitosa limpia el bloqueo; si la lectura falla, el botón sigue bloqueado y “Actualizar” permite reintentar la consulta.
4. Un rechazo conocido conserva la reserva, muestra el mensaje y consulta el listado para reflejar el estado actual (GET sin efectos); si esa lectura falla, la reserva queda bloqueada hasta verificarla.
5. `MyReservationsScreen` muestra el diálogo de confirmación sin motivo y la acción en curso; el POST solo se dispara desde “Confirmar”.
6. No se crean reglas de elegibilidad propias: se deriva `state=CONFIRMED` y `cancellation` del contrato.

## Componentes afectados

| Componente | Acción | Requisito |
| --- | --- | --- |
| `network/AuthenticatedApiClient.kt` | Modificar | POST sin cuerpo |
| `reservation/data/ReservationsApi.kt` | Modificar | RF-03, RF-04 |
| `reservation/data/KtorReservationsApi.kt` | Modificar | RF-03, RF-05 |
| `ui/reservation/MyReservationsUiState.kt` | Crear | RF-01, RF-06 |
| `ui/reservation/MyReservationsViewModel.kt` | Crear | RF-03–RF-07 |
| `ui/reservation/MyReservationsScreen.kt` | Crear | RF-02, RF-04–RF-06 |
| `ui/reservation/MyReservationsPresentation.kt` | Crear | RF-01, RF-04 |

## Validación

| Criterio | Método |
| --- | --- |
| CA-01 | `MyReservationsViewModelTest`: estados y `cancellation` del contrato. |
| CA-02 | `MyReservationsViewModelTest`: abrir/volver no llama a la API. |
| CA-03 | `KtorReservationsApiTest` y `MyReservationsViewModelTest`: POST sin body y doble confirmación. |
| CA-04 | `KtorReservationsApiTest`, `MyReservationsViewModelTest`: `200 CANCELLED` y `202 PENDING`. |
| CA-05 | `KtorReservationsApiTest`, `MyReservationsViewModelTest`: códigos y transporte. |
| CA-06 | `MyReservationsViewModelTest`: incertidumbre, GET posterior y desbloqueo solo tras lectura exitosa. |
| CA-07 | `MyReservationsViewModelTest`: fila y listado actualizados. |

**Comandos:** `./gradlew :shared:testAndroidHostTest`, `./gradlew :androidApp:assembleDebug :androidApp:lintDebug`.
**Límites:** no se ejecutan cancelaciones reales sin autorización; los efectos se validan con dobles y el diálogo en emulador. Sin dependencias nuevas.
