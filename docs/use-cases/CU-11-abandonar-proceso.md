# CU-11. Abandonar proceso propio

**Estado:** Revisado y aprobado por el usuario el 2026-10-07. Diálogo previo sin motivo confirmado el 2026-10-07.

## Identificación

| Campo | Descripción |
| --- | --- |
| Sistema | Aplicación KMP (Android). |
| Objetivo | Solicitar abandonar explícitamente un proceso propio y conocer si se canceló. |
| Actor principal | Usuario propietario del proceso. |
| Actor de apoyo | Backend Turnos. |
| Disparador | El usuario pulsa la acción explícita de abandonar el proceso. |

## Precondiciones

- Hay sesión válida y un proceso propio que Turnos permite abandonar según su estado e identificadores disponibles.

## Flujo principal

1. El usuario solicita abandonar; la app muestra un diálogo con profesional, fecha y hora, sin campo de motivo, y advierte que también se cancelará si acaba de confirmarse.
2. El usuario confirma; la app comunica la solicitud en curso y la envía a Turnos evitando envíos simultáneos repetidos.
3. Turnos conserva la intención y gestiona la cancelación central conforme a la aclaración docente.
4. La app muestra el abandono/cancelación verificado por Turnos y deja de ofrecer continuar el intercambio de teléfono.

## Flujos alternativos y de excepción

- **Antes del paso 1, volver atrás, cerrar la app o cerrar sesión:** no dispara este caso; conserva el proceso en Turnos para retomarlo si continúa vigente.
- **2b. Volver o cerrar el diálogo sin confirmar:** no envía abandono; conserva el proceso y termina sin solicitar cancelación.
- **3a. Se confirmó mientras el usuario esperaba:** la misma intención autoriza a Turnos a cancelar la reserva confirmada; no pide otra acción al usuario. Continúa en el paso 4 al verificar el resultado.
- **3b. Proceso ya terminado o vencimiento verificado:** muestra el resultado conocido sin reabrirlo; no llama cancelación exitosa a un vencimiento.
- **2a/3c. Timeout, desconexión o resultado incierto:** muestra que el abandono no se verificó y consulta el progreso; no habilita otro proceso mientras Turnos mantenga pendiente el cierre.
- **3d. Rechazo del backend:** informa que la cancelación no se completó; conserva el acceso al progreso consultable.

## Postcondiciones

- **Éxito:** cancelación verificada visible y sin nuevas acciones incompatibles sobre el proceso.
- **Alternativa:** intención por verificar, rechazo o resultado final distinto correctamente identificados.

## Reglas y relaciones

- La intención también autoriza cancelación de una confirmación concurrente, según lo aprobado en Turnos. La UI no declara ese resultado antes de verificarlo.
- Conservar el proceso no extiende `expiresAt`. Cancelar no garantiza que el horario siga libre para otra reserva.
- La autorización docente para cancelar holds está resuelta en Turnos; no se reabre esa decisión ni se inventan respuestas centrales para KMP.
- Sin abandono offline ni cola de solicitudes; terminar el proceso Android descarta el estado temporal del diálogo, no el proceso de reserva conservado en Turnos.

## Fuentes y pendientes

**Fuentes:** [CU-10 de Turnos](../../../backend-turnos/docs/use-cases/CU-10-abandonar-proceso.md); [aclaración docente registrada](../../../backend-turnos/docs/use-cases/README.md).

**Pendiente para la SPEC/PLAN:** textos del diálogo, presentación del resultado incierto, contrato y seguimiento coordinados con Turnos. Su validación del backend mantiene pendientes las respuestas y eventos reales de cancelación de holds.
