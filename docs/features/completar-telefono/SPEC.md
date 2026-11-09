# SPEC: Completar el teléfono solicitado

**Estado:** Implementada y verificada con pruebas; envío real no ejecutado.
**CU e issue de referencia:** [CU-07](../../use-cases/CU-07-completar-informacion.md); issue #10.
**Fuentes:** [CU-07 de KMP](../../use-cases/CU-07-completar-informacion.md), [CU-05 de Turnos](../../../../backend-turnos/docs/use-cases/CU-05-completar-informacion.md), [contrato de Turnos](../../../../backend-turnos/docs/CONTRATOS.md#api-propia-de-turnos), [anexo, secciones 15.4 a 15.9](../../../../INTEGRATION_REFERENCE-v2.md) e [instrucciones KMP](../../../AGENTS.md).

## Objetivo

Enviar el teléfono que Turnos solicita para un proceso propio y presentar la espera del resultado, sin declarar una confirmación que Turnos todavía no informó.

## Alcance

- **RF-01 · Habilitación real:** la entrada de teléfono solo se muestra cuando el proceso recibido tiene `canSubmitPhone=true` y no está finalizado. `requiredField=PHONE_NUMBER` por sí solo no habilita el envío.
- **RF-02 · Validación:** se admiten los separadores del contrato (espacios, guiones, paréntesis y puntos); se quitan antes de validar `^\+?[0-9]{7,15}$` y antes de enviar. Un valor inválido no se envía y permite corregirlo.
- **RF-03 · Envío:** `POST /api/reservation-processes/{processId}/phone` con `{requestId, phoneNumber}` y el JWT de usuario existente. Cada intento usa un `requestId` nuevo.
- **RF-04 · Envío único:** mientras el POST está en curso se impide otro envío por pulsaciones repetidas; el resultado se muestra como en curso.
- **RF-05 · Errores:** `400 INVALID_PHONE_NUMBER` y los `409 PHONE_NOT_REQUESTED`, `PHONE_SUBMISSION_ACTIVE`, `PHONE_WINDOW_CLOSED` y `REQUEST_ID_CONFLICT` se informan con un mensaje comprensible. `404 RESERVATION_PROCESS_NOT_FOUND` informa que el proceso no está disponible. `503`, `5xx` o fallo de transporte se presentan como resultado no verificado, nunca como éxito ni como fallo definitivo.
- **RF-06 · Actualización:** una respuesta `202` o `200` actualiza el proceso con el estado informado. Un rechazo con `processId` se contrasta con un `GET` del proceso. Tras un resultado incierto, la app ofrece verificar el estado con `GET` antes de volver a enviar.
- **RF-07 · Sin confirmación prematura:** `PHONE_QUEUED`, `WAITING_FOR_RESULT` y `RECONCILIATION_REQUIRED` se muestran como teléfono enviado y reserva aún no confirmada. Solo `state=CONFIRMED` se presenta como reserva confirmada.
- **RF-08 · Vencimiento o cierre:** si Turnos deja de habilitar el envío (`canSubmitPhone=false`) por vencimiento, cierre o envío activo, no se ofrece un nuevo teléfono.
- **RF-09 · Navegación:** salir y retomar el proceso usa CU-08; no se abandona ni se amplía el plazo. El texto del teléfono se conserva solo mientras viva el ViewModel y se descarta al cerrar sesión o terminar el proceso.

## Fuera de alcance

- Cancelar procesos o reservas (CU-10), listar historial o reservas confirmadas (CU-09).
- Persistir el teléfono, enviarlo offline o encolarlo.
- Reintentos automáticos, polling, un nuevo ViewModel, coordinador, repositorio o máquina de estados.
- Cambios en `backend-turnos` o refactorizaciones de CU-06/CU-08.

## Flujo de usuario

1. Turnos informa un proceso pendiente con `canSubmitPhone=true`.
2. La app muestra la solicitud, el proceso y el vencimiento; el formulario de teléfono queda disponible.
3. La persona escribe el teléfono y toca “Enviar teléfono”.
4. La app comunica el envío y no permite repetirlo mientras está en curso.
5. Turnos responde el proceso actualizado, un rechazo o un resultado incierto.
6. Con el proceso actualizado, la app muestra que el teléfono ya fue enviado y que la reserva sigue en curso.
7. Ante un rechazo corregible, muestra el motivo y permite editar y enviar de nuevo si Turnos sigue habilitándolo.
8. Ante incertidumbre, ofrece “Verificar estado”; no repite el envío hasta conocer el proceso.

## Reglas y casos alternativos

- El teléfono se normaliza localmente con la misma regla del contrato; la validación local no sustituye la de Turnos.
- Un `200` es una repetición equivalente de Turnos; no crea otro envío ni confirma la reserva.
- Un error de transporte no demuestra que el envío no llegó: no se repite sin verificar.
- La fase y el vencimiento mostrados provienen de Turnos; no se deduce un resultado con el reloj del dispositivo.
- El teléfono no se conserva como atributo de la cuenta.

## Criterios de aceptación

- **CA-01 · RF-01:** con `canSubmitPhone=false`, aun con `requiredField=PHONE_NUMBER`, el formulario no se muestra ni se puede enviar.
- **CA-02 · RF-02:** los separadores permitidos se eliminan; un valor inválido no produce POST y muestra un mensaje corregible.
- **CA-03 · RF-03:** el POST usa la ruta, el cuerpo, el bearer y el teléfono normalizado del contrato.
- **CA-04 · RF-04:** dos pulsaciones durante el envío producen una sola llamada.
- **CA-05 · RF-05:** los `400`, `409`, `404`, `503`, `5xx` y el transporte se clasifican y presentan sin falso éxito.
- **CA-06 · RF-06:** la respuesta actualiza el proceso; un rechazo se contrasta con `GET`; la incertidumbre se verifica antes de repetir.
- **CA-07 · RF-07:** `PHONE_QUEUED` y `WAITING_FOR_RESULT` no muestran “reserva confirmada”.
- **CA-08 · RF-08:** un proceso vencido, final o sin envío habilitado no ofrece el formulario.
- **CA-09 · RF-09:** salir no cancela; el proceso se retoma por CU-08 y el estado se conserva solo durante el ViewModel.

## Verificación

La evidencia por criterio, los comandos y los límites están en [TASKS.md](TASKS.md). Las decisiones técnicas y el contrato consumido están en [PLAN.md](PLAN.md).
