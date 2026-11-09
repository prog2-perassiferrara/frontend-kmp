# SPEC: Consultar y retomar el proceso propio

**Estado:** Implementada y verificada con pruebas; prueba de integración real el 2026-11-09.
**CU e issue de referencia:** [CU-08](../../use-cases/CU-08-consultar-proceso.md); issue #11.
**Fuentes:** [CU-08 de KMP](../../use-cases/CU-08-consultar-proceso.md), [CU-06 de Turnos](../../../../backend-turnos/docs/use-cases/CU-06-consultar-proceso.md), [CU-09 de Turnos](../../../../backend-turnos/docs/use-cases/CU-09-recuperar-procesos.md), [contrato de Turnos](../../../../backend-turnos/docs/CONTRATOS.md#api-propia-de-turnos) e [instrucciones KMP](../../../AGENTS.md).

## Objetivo

Mostrar el progreso que Turnos informa para el proceso propio, permitir actualizarlo y recuperar el pendiente al iniciar sesión o reabrir la app, sin repetir operaciones ni replicar la reconciliación del backend.

## Alcance

- **RF-01 · Consulta:** `GET /api/reservation-processes/{processId}` con el JWT de usuario y sin iniciar un hold ni un proceso nuevo.
- **RF-02 · Progreso:** presentar la fase, los datos de la selección y el grado de verificación recibidos. `CONFIRMED`, `FAILED`, `EXPIRED` y `CANCELLED` solo se muestran si Turnos los informa.
- **RF-03 · Vencimiento:** mostrar `expiresAt`, que el contrato informa en UTC, convertido a la zona horaria de atención con la que la aplicación presenta las fechas y horas de los turnos. El reloj del dispositivo no se usa para declarar vencido, cancelado ni confirmado un proceso.
- **RF-04 · Teléfono:** cuando `requiredField=PHONE_NUMBER` o `canSubmitPhone=true`, avisar que falta el teléfono y dejar la acción para CU-07. No se envía ni se solicita el número.
- **RF-05 · Actualización:** permitir actualizar el proceso conocido sin polling permanente. La actualización no crea efectos.
- **RF-06 · Pendiente propio:** al iniciar sesión o reabrir la app, consultar `GET /api/reservations?page=0&size=1` y abrir el `pending` solo si Turnos lo devuelve.
- **RF-07 · Descubrimiento no bloqueante:** si la consulta inicial falla, abrir la búsqueda y mostrar un aviso con “Reintentar”; no deducir ausencia de proceso.
- **RF-08 · Salir y volver:** salir de la pantalla no cancela ni abandona el proceso. Desde la búsqueda, un aviso permite volver a consultarlo.
- **RF-09 · Sesión y respuestas tardías:** una respuesta de una sesión anterior no reabre el proceso ni publica datos de otra cuenta. Un `401` conserva el cierre de sesión existente.
- **RF-10 · Sin estado local nuevo:** el progreso no se persiste; al reabrir se consulta Turnos. No hay cola offline, polling ni reconciliación propia.

## Fuera de alcance

- Enviar teléfono o corregir un rechazo telefónico (CU-07).
- Cancelar procesos o reservas (CU-10), listar el historial (CU-09) y consultar reservas de otros usuarios.
- SSE, notificaciones, polling permanente, almacenamiento local nuevo y máquina de estados paralela.
- Cambios en `backend-turnos` o en los contratos existentes.

## Flujo de usuario

1. Al entrar con sesión válida, la app consulta el pendiente propio sin bloquear la búsqueda.
2. Si Turnos devuelve un pendiente, se abre su progreso con la fase y el vencimiento recibidos.
3. La persona puede actualizar el estado, volver a la búsqueda o salir de la app; el proceso sigue en Turnos.
4. Si Turnos pide teléfono, se muestra el aviso correspondiente sin habilitar el envío.
5. Si un proceso termina, se muestra el resultado informado y no se ofrecen acciones incompatibles.
6. Si la consulta inicial o una actualización falla, se informa y se permite reintentar.

## Reglas y casos alternativos

- Un `404` de consulta informa que el proceso no existe o no es propio, sin exponer datos ajenos.
- Un fallo de red no se interpreta como ausencia del proceso ni como resultado final.
- El pendiente se abre una sola vez por instancia del ViewModel; rotar la pantalla no repite la consulta ni reabre un proceso que la persona dejó.
- El proceso Android puede descartar el estado temporal; la recuperación se apoya en la sesión conservada y en Turnos.
- La app no usa `processId` de otra cuenta ni mezcla procesos anteriores con el pendiente.

## Criterios de aceptación

- **CA-01 · RF-01:** la consulta usa la ruta del proceso y el bearer del usuario, sin crear holds.
- **CA-02 · RF-02:** la fase y los datos se presentan tal como los informa Turnos, sin completar estados ausentes.
- **CA-03 · RF-03:** `expiresAt` se muestra como `DD/MM/AAAA · HH:mm` en la zona de atención, con el mismo criterio que las fechas de los turnos, y no se usa el reloj local para declarar vencimiento.
- **CA-04 · RF-04:** `requiredField` o `canSubmitPhone` producen un aviso, sin campo ni envío de teléfono.
- **CA-05 · RF-05:** “Actualizar” repite el `GET` y conserva el último proceso si la actualización falla.
- **CA-06 · RF-06:** al iniciar sesión o reabrir, un `pending` de Turnos abre el proceso; sin `pending` se muestra la búsqueda.
- **CA-07 · RF-07:** un fallo de descubrimiento no bloquea la búsqueda y ofrece “Reintentar”.
- **CA-08 · RF-08:** salir del proceso deja un aviso “Ver estado” y no cancela el proceso.
- **CA-09 · RF-09:** una respuesta tardía tras cerrar sesión no publica el proceso.
- **CA-10 · RF-10:** no se agrega almacenamiento local, cola, polling ni reconciliación propia.

## Verificación

La evidencia por criterio, los comandos y los límites están en [TASKS.md](TASKS.md). Las decisiones técnicas y el contrato consumido están en [PLAN.md](PLAN.md).
