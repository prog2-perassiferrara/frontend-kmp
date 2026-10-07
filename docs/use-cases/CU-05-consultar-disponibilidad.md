# CU-05. Consultar disponibilidad

**Estado:** Revisado y aprobado por el usuario el 2026-10-07.

## Identificación

| Campo | Descripción |
| --- | --- |
| Sistema | Aplicación KMP (Android). |
| Objetivo | Conocer y seleccionar un horario libre para un profesional y fecha. |
| Actor principal | Usuario final. |
| Actor de apoyo | Backend Turnos. |
| Disparador | El usuario selecciona un profesional y solicita disponibilidad para una fecha. |

## Precondiciones

- Hay sesión válida y un profesional identificado.

## Flujo principal

1. El usuario elige la fecha para el profesional seleccionado.
2. La app comunica carga y solicita disponibilidad a Turnos.
3. Turnos calcula horarios libres a partir de agenda vigente y ocupaciones centrales.
4. La app presenta los horarios recibidos para esa selección.
5. El usuario selecciona un horario para continuar en [CU-06](CU-06-iniciar-reserva.md).

## Flujos alternativos y de excepción

- **1a/3a. Fecha inválida o profesional inexistente/deshabilitado:** informa la condición; permite corregir la selección y volver al paso 1.
- **3b. Sin agenda aplicable o todos los horarios ocupados:** muestra que no hay horarios para esa fecha, sin presentarlo como fallo; vuelve al paso 1.
- **2a/3c. Turnos inaccesible, sin catálogo vigente u ocupaciones no disponibles:** informa que no pudo obtener disponibilidad; no representa los horarios como libres. Termina el intento.
- **4a. Cambió profesional o fecha mientras se consultaba:** no muestra la respuesta anterior como correspondiente a la nueva selección; consulta esta desde el paso 2.

## Postcondiciones

- **Éxito:** horarios para la selección consultada o ausencia válida de horarios.
- **Fallo:** indisponibilidad explícita, sin ofrecer huecos calculados a partir de información incompleta.

## Reglas y relaciones

- La app no calcula huecos desde el filtro de Catálogo ni interpreta una consulta fallida de ocupaciones como agenda libre.
- La consulta no crea un hold. Un horario puede ocuparse antes de reservarlo.
- Sin consultas offline. La selección se conserva según la [política temporal](README.md), sin utilizar horarios anteriores como disponibilidad vigente tras una consulta fallida.
- Horas de atención locales y vencimientos UTC tienen significados distintos; aceptar `HH:mm` y `HH:mm:ss` no requiere mostrarlos en ese formato literal.

## Fuentes y pendientes

**Fuentes:** [CU-03 de Turnos](../../../backend-turnos/docs/use-cases/CU-03-consultar-disponibilidad.md); [anexo, secciones 4 y 8](../../../INTEGRATION_REFERENCE-v2.md).

**Pendiente para la SPEC:** fechas permitidas, horarios pasados y formatos visibles. Coordinar restricciones con Turnos y aplicar la conservación temporal acordada.

**Pendiente para el PLAN:** contrato de disponibilidad; protección frente a respuestas fuera de orden.
