# CU-07. Completar información adicional

**Estado:** Revisado y aprobado por el usuario el 2026-10-07.

## Identificación

| Campo | Descripción |
| --- | --- |
| Sistema | Aplicación KMP (Android). |
| Objetivo | Proporcionar el teléfono requerido y conocer el resultado de la reserva. |
| Actor principal | Usuario propietario del proceso. |
| Actor de apoyo | Backend Turnos. |
| Disparador | Turnos informa que el proceso propio requiere teléfono. |

## Precondiciones

- Hay sesión válida y un proceso propio identificado cuya información requerida permite continuar antes del vencimiento.

## Flujo principal

1. La app presenta la solicitud de teléfono, el proceso al que corresponde y el vencimiento informado por Turnos.
2. El usuario completa el teléfono y solicita enviarlo.
3. La app comunica envío en curso y lo remite a Turnos evitando envíos simultáneos por pulsaciones repetidas.
4. Turnos valida y gestiona el intercambio Kafka; la app muestra que espera un resultado.
5. Mediante [CU-08](CU-08-consultar-proceso.md), la app obtiene y presenta la confirmación definitiva comunicada por Turnos.

## Flujos alternativos y de excepción

- **3a/4a. Entrada inválida o teléfono rechazado:** informa el problema y permite corregir y volver al paso 2 mientras siga vigente; no lo presenta como fallo terminal.
- **3b/4b. Proceso vencido, inválido o terminado:** informa el estado recibido e impide nuevos envíos incompatibles; termina el flujo de información.
- **3c/4c/5a. Interrupción o resultado incierto:** muestra espera o falta de verificación; consulta el progreso con CU-08 antes de repetir el envío, sin declarar confirmación.
- **2a/4d. Volver atrás, pasar a segundo plano o cerrar la app:** no solicita abandono. Puede retomar el proceso desde CU-08 si sigue vigente, sin ampliar el plazo. El teléfono sin enviar se conserva mientras viva el proceso Android; se descarta al terminarlo, cerrar sesión, detectar vencimiento/invalidez de la sesión o cambiar cuenta.

## Postcondiciones

- **Éxito:** el propietario conoce la reserva confirmada por Turnos.
- **Alternativa:** error corregible, espera, incertidumbre o resultado terminal distinguibles.

## Reglas y relaciones

- KMP envía información a Turnos, no publica en Kafka ni maneja la cuenta técnica.
- La regla central elimina espacios, guiones, paréntesis y puntos y valida `^\+?[0-9]{7,15}$`; la validación local no sustituye la de los backends.
- Enviar un teléfono no confirma por sí solo una reserva ni autoriza conservarlo como atributo permanente de la cuenta.
- No enviar teléfono offline ni ponerlo en cola; el estado temporal sigue la [política confirmada](README.md).

## Fuentes y pendientes

**Fuentes:** [CU-05 de Turnos](../../../backend-turnos/docs/use-cases/CU-05-completar-informacion.md); [anexo, secciones 15.4 a 15.9](../../../INTEGRATION_REFERENCE-v2.md).

**Pendiente para la SPEC:** mensajes de validación y presentación del vencimiento. Conservación temporal del formulario ya acordada.

**Pendiente para el PLAN:** contrato de envío, seguimiento y recuperación de respuesta perdida coordinado con Turnos; no elegir transporte de actualizaciones todavía.
