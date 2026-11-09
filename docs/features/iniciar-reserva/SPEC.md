# SPEC: Iniciar una reserva

**Estado:** Implementada y verificada con pruebas; prueba de integración real el 2026-11-09.
**CU e issue de referencia:** [CU-06](../../use-cases/CU-06-iniciar-reserva.md); issue #9.
**Fuentes:** [CU-06 de KMP](../../use-cases/CU-06-iniciar-reserva.md), CU-04 de Turnos, contrato de Turnos, anexo, secciones 9, 10 y 17 e [instrucciones KMP](../../../AGENTS.md).

## Objetivo

Desde el profesional, la fecha y el horario marcados en CU-05, enviar una única solicitud de inicio a Turnos y presentar el resultado como un proceso en curso, sin afirmar que la reserva quedó confirmada.

## Alcance

- **RF-01 · Acción única:** con un horario marcado, “Solicitar reserva” envía `POST /api/reservation-processes` con `requestId`, `professionalId`, `date` y `startTime`, usando el JWT de usuario y el cliente protegido existentes.
- **RF-02 · Envío en curso:** mientras se espera la respuesta se muestra el progreso y se impide una segunda solicitud por pulsaciones repetidas. La pantalla de proceso reemplaza a la disponibilidad y no ofrece acciones de inicio.
- **RF-03 · Resultado:** `202` y `200` se presentan como proceso informado por Turnos, con su fase, vencimiento y datos recibidos. La aceptación inicial no se presenta como reserva confirmada.
- **RF-04 · Idempotencia:** el `requestId` se conserva como identificador de la acción y como `processId` local. Una repetición propia se acepta sin crear otro proceso.
- **RF-05 · Rechazos conocidos:** un `400` de selección o perfil, un `409` de conflicto o un rechazo de disponibilidad se informan con su causa y sin reintento ciego.
- **RF-06 · Proceso existente:** si Turnos rechaza por un proceso pendiente, por disponibilidad o por un cierre incierto e incluye `processId`, se consulta y presenta ese proceso en lugar de iniciar otro.
- **RF-07 · Resultado incierto:** `503`, `5xx`, timeout o fallo de transporte no se presentan como éxito ni como fallo. Se informa que el resultado no está verificado y se permite consultar Turnos antes de repetir la acción.
- **RF-08 · Respuesta perdida:** si la verificación por `requestId` devuelve `404`, Turnos no registró el inicio; recién entonces se habilita repetir la misma solicitud con el mismo `requestId`.
- **RF-09 · Navegación:** salir del proceso no lo cancela ni lo abandona. Al volver a la búsqueda queda un aviso no bloqueante para consultar su estado.
- **RF-10 · Teléfono:** si Turnos informa `requiredField=PHONE_NUMBER` o `canSubmitPhone=true`, solo se avisa que falta el teléfono. El envío pertenece a CU-07 y no se implementa aquí.

## Fuera de alcance

- Enviar el teléfono, corregir rechazos de teléfono y completar CU-07.
- Cancelar procesos o reservas, listar historial y consultar reservas confirmadas (CU-09/CU-10).
- Reintentos automáticos, polling, colas offline, almacenamiento local nuevo y reconciliación propia de REST/Kafka.
- Cambios en `backend-turnos`, en su máquina de estados o en los CU ya implementados.

## Flujo de usuario

1. En disponibilidad, la persona marca un horario y toca “Solicitar reserva”.
2. La app informa que la solicitud está en curso y no permite repetirla.
3. Turnos responde un proceso, un rechazo o un resultado sin verificar.
4. Con un proceso, la app muestra su estado y permite actualizarlo; no declara confirmación por sí sola.
5. Con un rechazo conocido, explica el motivo y ofrece volver a la disponibilidad para reconsultar.
6. Con un resultado incierto, explica que no puede afirmar si Turnos lo registró y ofrece “Verificar estado”.
7. Si la verificación confirma que no se registró, permite “Reintentar” con la misma solicitud.

## Reglas y casos alternativos

- El límite de un proceso pendiente lo controla Turnos, incluso ante concurrencia; la app no lo reemplaza.
- Una respuesta perdida no demuestra que el hold falló. No se repite el POST sin verificar por `requestId` o sin la habilitación explícita de Turnos.
- Un `processId` recibido en un error se usa solo para consultar el proceso propio; no se interpreta como éxito.
- La fase recibida se muestra sin deducir por el reloj del dispositivo que un hold venció o que una reserva se canceló.
- Salir de la pantalla, rotar o reabrir la app no ejecuta cancelaciones ni abandonos.
- La selección enviada se conserva solo mientras viva el ViewModel; no se persiste ni se reenvía tras un reinicio del proceso Android.

## Criterios de aceptación

- **CA-01 · RF-01:** “Solicitar reserva” envía el cuerpo y el bearer exactos del contrato, sin parámetros adicionales.
- **CA-02 · RF-02:** una segunda pulsación mientras el envío está en curso no genera otra petición.
- **CA-03 · RF-03:** `202` y `200` muestran el proceso con la fase informada y nunca la leyenda “reserva confirmada”, salvo `state=CONFIRMED` recibido de Turnos.
- **CA-04 · RF-04:** el `requestId` enviado coincide con el `processId` del proceso y se reutiliza al repetir la misma solicitud.
- **CA-05 · RF-05:** `400` y `409` conocidos se informan con su motivo y no repiten la petición automáticamente.
- **CA-06 · RF-06:** un error con `processId` consulta y muestra ese proceso; no inicia otro.
- **CA-07 · RF-07:** `503`, `5xx` o fallo de transporte producen un estado de incertidumbre, sin presentar éxito ni fallo.
- **CA-08 · RF-08:** tras un `404` de verificación solo se habilita “Reintentar” con el mismo `requestId`; la suite confirma que no se duplica el proceso.
- **CA-09 · RF-09:** salir y volver a la búsqueda conserva el proceso y ofrece “Ver estado”, sin cancelarlo.
- **CA-10 · RF-10:** cuando Turnos pide teléfono, la pantalla lo informa y no envía ni solicita el número.

## Verificación

La evidencia por criterio, los comandos y los límites están en [TASKS.md](TASKS.md). Las decisiones técnicas y el contrato consumido están en [PLAN.md](PLAN.md).
