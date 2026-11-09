# SPEC: Cancelar reserva propia

**Estado:** Implementada y verificada con pruebas; cancelación real no ejecutada (2026-11-09).
**CU e issue de referencia:** [CU-10](../../use-cases/CU-10-cancelar-reserva.md); issue #12.
**Fuentes:** [CU-10 de KMP](../../use-cases/CU-10-cancelar-reserva.md), [CU-08 de Turnos](../../../../backend-turnos/docs/use-cases/CU-08-cancelar-reserva.md), [contrato de Turnos](../../../../backend-turnos/docs/CONTRATOS.md#api-propia-de-turnos) e [instrucciones KMP](../../../AGENTS.md).

## Objetivo

Solicitar la cancelación de una reserva propia confirmada, mostrando el resultado que Turnos acredita y sin declarar cancelada una reserva antes de recibirlo.

## Alcance

- **RF-01 · Habilitación:** la acción se ofrece solo para `state=CONFIRMED` sin cancelación `PENDING`. Una cancelación `RETRY_REQUIRED` permite reintentar; `CANCELLED` no ofrece la acción.
- **RF-02 · Confirmación:** antes del POST se muestra un diálogo con profesional, fecha y hora, sin motivo ni campos adicionales. “Volver” no envía nada.
- **RF-03 · Operación:** `POST /api/reservation-processes/{processId}/cancel` sin cuerpo y con el bearer existente. Mientras está en curso se impide otra solicitud.
- **RF-04 · Resultado:** `200` y `202` actualizan la fila con el estado y la cancelación informados. Solo `state=CANCELLED` se presenta como cancelada; `202` se muestra como cancelación en curso.
- **RF-05 · Errores:** `404`, `409 CANCELLATION_NOT_ALLOWED`, `403` y demás rechazos conocidos se informan sin declarar cancelación. `503`, `5xx` o fallo de transporte se presentan como resultado no verificado.
- **RF-06 · Respuesta perdida:** ante un resultado incierto se conserva la reserva, se consulta el listado y se bloquea otra cancelación de esa reserva hasta que una lectura exitosa permita conocer su estado.
- **RF-07 · Actualización:** después de la operación se actualiza la fila y se puede refrescar el listado; no se reimplementan las reglas de cancelación de Turnos.

## Fuera de alcance

- Motivo de cancelación, cancelación offline, cola de solicitudes, polling o reintentos automáticos.
- Abandonar un proceso pendiente (CU-11) o cancelar una reserva ajena.
- Cambios en `backend-turnos` o en CU-01/CU-08.

## Flujo de usuario

1. En “Mis reservas”, una reserva confirmada ofrece “Cancelar reserva”.
2. La app pide confirmación con los datos de la reserva.
3. Al confirmar, envía una sola solicitud y muestra la operación en curso.
4. Turnos responde la reserva actualizada, un rechazo o un resultado incierto.
5. La app muestra el estado informado: cancelada, cancelación en curso, pendiente de reintento o rechazo.
6. Ante incertidumbre, actualiza el listado y exige una lectura exitosa antes de habilitar otro intento.

## Reglas y casos alternativos

- La elegibilidad surge del estado recibido; la app no deduce ni fuerza transiciones.
- Un `202` con `cancellation.status=PENDING` no es una cancelación confirmada.
- Un rechazo o un `404` se contrastan con el listado para mostrar el estado actual.
- La reserva no se elimina de la lista por una intención local.

## Criterios de aceptación

- **CA-01 · RF-01:** solo `CONFIRMED` sin `PENDING` habilita la acción; `RETRY_REQUIRED` permite reintentar.
- **CA-02 · RF-02:** el diálogo muestra los datos y “Volver” no produce POST.
- **CA-03 · RF-03:** dos confirmaciones simultáneas producen una sola llamada, sin cuerpo.
- **CA-04 · RF-04:** `200 CANCELLED` actualiza la fila; `202 PENDING` no la declara cancelada.
- **CA-05 · RF-05:** `404`, `409`, `403`, `503` y transporte se informan correctamente.
- **CA-06 · RF-06:** un resultado incierto consulta el listado y bloquea otro intento hasta una lectura exitosa.
- **CA-07 · RF-07:** la fila y el listado reflejan el estado posterior sin reimplementar reglas.

## Verificación

La evidencia, los comandos y los límites están en [TASKS.md](TASKS.md); las decisiones técnicas en [PLAN.md](PLAN.md).
