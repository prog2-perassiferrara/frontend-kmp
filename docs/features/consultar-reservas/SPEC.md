# SPEC: Consultar mis reservas

**Estado:** Implementada y verificada con pruebas y emulador (2026-11-09).
**CU e issue de referencia:** [CU-09](../../use-cases/CU-09-consultar-reservas.md); issue #12.
**Fuentes:** [CU-09 de KMP](../../use-cases/CU-09-consultar-reservas.md), CU-07 de Turnos, contrato de Turnos e [instrucciones KMP](../../../AGENTS.md).

## Objetivo

Mostrar las reservas propias informadas por Turnos, diferenciando el historial de un proceso todavía pendiente y sin presentar una confirmación que Turnos no acreditó.

## Alcance

- **RF-01 · Consulta:** `GET /api/reservations?page={n}&size={m}` con el JWT de usuario existente. Cada entrada a la pestaña Cuenta relee la primera página; no se consulta Catálogo ni se guardan datos en el dispositivo.
- **RF-02 · Contenido:** se muestran el nombre histórico del profesional (o una referencia por identificador si Turnos no lo conserva), la fecha, la hora local de atención, el estado y el motivo informado. No se inventan datos ausentes.
- **RF-03 · Pendiente separado:** el `pending` se presenta como proceso en curso, nunca como reserva confirmada, y permite abrir CU-08 con el estado ya recibido.
- **RF-04 · Estados de la consulta:** carga inicial, lista vacía, error con reintento y actualización manual de la página actual.
- **RF-05 · Paginación:** se usa la página y los totales devueltos por Turnos (tamaño 10), con “Anterior” y “Siguiente”. No hay filtros ni orden propio.

## Fuera de alcance

- Cancelar reservas (CU-10), buscar o filtrar reservas, consultas offline y persistencia.
- Enriquecer nombres con Catálogo o repetir consultas que Turnos no ofrece.

## Flujo de usuario

1. Desde la búsqueda autenticada, la persona abre “Mis reservas”.
2. La app muestra la carga y consulta la primera página.
3. Turnos responde el pendiente y el historial con sus totales.
4. La app lista las reservas y destaca el proceso pendiente por separado.
5. La persona puede actualizar, cambiar de página o abrir el proceso pendiente.

## Reglas y casos alternativos

- Un fallo de consulta no se muestra como lista vacía; conserva los datos previos si existían.
- `CONFIRMED`, `CANCELLED`, `FAILED` y `EXPIRED` se presentan como estados distintos.
- Un `state` no reconocido se muestra tal cual, sin deducir su significado.
- Un aviso de cancelación se conserva solo durante la lectura que reconcilia un rechazo o un resultado incierto; una lectura posterior lo retira porque la fila ya muestra el estado vigente.
- La sesión vencida la resuelve el cliente HTTP existente; la pantalla no conserva datos ajenos.

## Criterios de aceptación

- **CA-01 · RF-01:** la consulta usa la ruta, el bearer, la página y el tamaño del contrato.
- **CA-02 · RF-02:** se representan profesional, fecha, hora, estado y motivo disponibles.
- **CA-03 · RF-03:** el pendiente no aparece como reserva confirmada y se puede abrir en CU-08.
- **CA-04 · RF-04:** carga, vacío, error con reintento y actualización funcionan sin confundirse.
- **CA-05 · RF-05:** “Anterior” y “Siguiente” respetan página y totales; el tamaño enviado es 10.

## Verificación

La evidencia, los comandos y los límites están en [TASKS.md](TASKS.md); las decisiones técnicas en [PLAN.md](PLAN.md).
