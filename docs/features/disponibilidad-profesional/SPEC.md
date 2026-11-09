# SPEC: Consultar disponibilidad de un profesional

**Estado:** Implementada y verificada el 2026-11-08.
**CU e issue de referencia:** [CU-05](../../use-cases/CU-05-consultar-disponibilidad.md); issue #8.
**Fuentes:** [CU-05 de KMP](../../use-cases/CU-05-consultar-disponibilidad.md), CU-03 de Turnos, contrato de Turnos, anexo, secciones 4 y 8 e [instrucciones KMP](../../../AGENTS.md).

## Objetivo

Completar el recorrido de búsqueda: desde un profesional encontrado, consultar en Turnos los horarios libres de una fecha, presentarlos y permitir marcar uno, sin calcular disponibilidad ni crear reservas.

## Alcance

- **RF-01 · Selección:** tocar un resultado de CU-04 abre la disponibilidad de ese profesional. La pantalla muestra su nombre y categoría y permite volver a la búsqueda.
- **RF-02 · Fecha:** campo `AAAA-MM-DD` con validación local de forma. Se precarga con la fecha del filtro de búsqueda cuando ya respeta esa forma; vacío en caso contrario. El rango real lo valida Turnos.
- **RF-03 · Consulta:** `GET /api/appointment-availability` con `professionalId` y `date`, usando el JWT de usuario y el cliente protegido existentes. No crea holds, procesos ni reservas, y no escribe datos.
- **RF-04 · Resultados:** presentar los horarios devueltos en su orden, sin recalcular ni completar huecos. Se aceptan `HH:mm` y `HH:mm:ss` y se muestran como `HH:mm`.
- **RF-05 · Estados:** mostrar carga, horarios, ausencia válida de horarios y error. La ausencia no es error; el error no se presenta como lista vacía.
- **RF-06 · Errores:** fecha o profesional rechazados invitan a corregir; Catálogo sin vigencia, ocupaciones inaccesibles o Turnos inaccesible permiten reintentar la misma consulta. Un 401 conserva el cierre de sesión existente.
- **RF-07 · Respuestas fuera de orden:** una respuesta pedida para otra fecha o profesional no se publica como resultado de la selección actual.
- **RF-08 · Navegación:** volver a la búsqueda conserva sus filtros, página y resultados. No se agrega BottomBar ni navegación general.
- **RF-09 · Conservación y sesión:** selección, fecha, horarios y horario marcado se conservan al rotar; se descartan al cerrar sesión, terminar el proceso o cambiar de cuenta.
- **RF-10 · Selección visual:** tocar un horario ofrecido lo marca como elegido y conserva ese dato junto con el profesional y la fecha. Marcar no crea holds, procesos ni reservas; iniciar la reserva corresponde a CU-06.

## Fuera de alcance

- Iniciar la reserva, crear holds o procesos y confirmar turnos (CU-06): elegir un horario solo lo marca visualmente.
- Holds, procesos, confirmaciones, consultas offline, caché de horarios y notificaciones.
- Cálculo propio de disponibilidad desde agendas u ocupaciones, y réplica de reglas de Turnos.
- BottomBar, pestañas, navegación general y persistencia de la selección entre procesos.

## Flujo de usuario

1. En los resultados, la persona toca un profesional.
2. La app muestra el profesional y el campo de fecha, precargado si la búsqueda ya tenía una fecha.
3. Al consultar se muestra el progreso y se pide disponibilidad a Turnos.
4. Con horarios se listan; sin horarios se informa que no hay disponibles para esa fecha.
5. La persona puede marcar un horario; la app aclara que aún no está reservado.
6. Ante un error se explica y, si es reintentable, se ofrece “Reintentar”.
7. “Volver” regresa a la búsqueda con sus filtros y resultados intactos.

## Reglas y casos alternativos

- La app no calcula huecos ni interpreta el filtro de agenda de Catálogo como disponibilidad real.
- Una fecha que no cumple `AAAA-MM-DD` no se envía; el resto de los rechazos los decide Turnos.
- Un profesional deshabilitado o inexistente se informa y no se reintenta en bucle: corregir requiere volver a la búsqueda.
- Cambiar la fecha mientras se espera una respuesta no muestra esa respuesta para la fecha nueva; la consulta debe repetirse.
- Consultar o marcar un horario no lo bloquea: otro usuario puede ocuparlo antes de iniciar la reserva (CU-06).

## Criterios de aceptación

- **CA-01 · RF-01:** desde un resultado se abre la disponibilidad del profesional elegido, con su nombre visible, y se puede volver a la búsqueda.
- **CA-02 · RF-02, RF-03:** la consulta envía `professionalId` y `date` en formato ISO, con el JWT de usuario, sin crear holds ni procesos.
- **CA-03 · RF-04:** los horarios se muestran en el orden recibido; `HH:mm:ss` y `HH:mm` se presentan como `HH:mm`.
- **CA-04 · RF-05:** una respuesta con lista vacía se informa como ausencia de horarios, sin error.
- **CA-05 · RF-05, RF-06:** un fallo de Turnos se informa como error y “Reintentar” repite la consulta vigente.
- **CA-06 · RF-06:** `400` y `404` invitan a corregir y no ofrecen reintento ciego; un `401` cierra la sesión con el comportamiento existente.
- **CA-07 · RF-07:** una respuesta de una fecha o profesional anterior no reemplaza los resultados de la selección actual.
- **CA-08 · RF-08, RF-09:** volver y rotar conservan filtros, página, resultados, selección, fecha y horario marcado; cerrar sesión los descarta.
- **CA-09 · RF-04:** la pantalla aclara que los horarios no están reservados y que iniciar la reserva corresponde al CU siguiente.
- **CA-10 · RF-10:** tocar un horario ofrecido lo marca; cambiar la fecha o cerrar sesión descarta la marca y no se crea ningún hold ni proceso.

## Verificación

La evidencia por criterio, los comandos y los límites están en [TASKS.md](TASKS.md). Las decisiones técnicas están en [PLAN.md](PLAN.md).
