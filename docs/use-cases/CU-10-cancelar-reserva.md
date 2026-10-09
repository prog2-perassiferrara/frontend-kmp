# CU-10. Cancelar reserva propia

**Estado:** Revisado y aprobado por el usuario el 2026-10-07. Diálogo previo sin motivo confirmado el 2026-10-07.

## Identificación

| Campo | Descripción |
| --- | --- |
| Sistema | Aplicación KMP (Android). |
| Objetivo | Solicitar cancelar una reserva propia confirmada y conocer el resultado verificado. |
| Actor principal | Usuario propietario de la reserva. |
| Actor de apoyo | Backend Turnos. |
| Disparador | El usuario solicita explícitamente cancelar la reserva seleccionada. |

## Precondiciones

- Hay sesión válida y una reserva propia conocida como confirmada. Una repetición puede encontrarla ya cancelada.

## Flujo principal

1. El usuario solicita cancelar; la app muestra un diálogo con profesional, fecha y hora, sin campo de motivo.
2. El usuario confirma; la app presenta la acción en curso y envía la solicitud a Turnos evitando pulsaciones repetidas simultáneas.
3. Turnos verifica propiedad y gestiona la cancelación central y su reconciliación.
4. La app recibe y muestra la cancelación verificada; actualiza la reserva propia.

## Flujos alternativos y de excepción

- **2b. Volver o cerrar el diálogo sin confirmar:** no envía la cancelación; conserva la reserva y termina sin cambios.
- **3a. Ya cancelada:** muestra el estado actual sin duplicar efectos; continúa en el paso 4.
- **3b. No autorizada, inexistente o no cancelable:** informa el rechazo sin declarar cancelación; termina el intento.
- **2a/3c. Timeout, desconexión o resultado pendiente:** indica que la cancelación no pudo verificarse y consulta el progreso en Turnos; no borra la reserva ni la muestra cancelada por intención local.
- **4a. Resultado tardío o consulta posterior:** presenta el estado reconciliado de la misma reserva; no lo aplica a otra selección ni revierte una cancelación verificada con información anterior.

## Postcondiciones

- **Éxito:** cancelación verificada visible en la reserva.
- **Alternativa:** rechazo conocido o resultado por verificar claramente diferenciados.

## Reglas y relaciones

- Esta acción no depende de Catálogo. Turnos controla propiedad y permisos.
- El diálogo previo y la omisión de motivo fueron confirmados por el usuario; la API central permite cancelación sin body.
- Se diferencia del abandono de un proceso pendiente (CU-11), aunque Turnos use la operación central de cancelación para ambos.
- Un timeout no prueba fallo; no habilita reenvíos indiscriminados.
- Sin cancelación offline ni cola de solicitudes; el diálogo no confirmado es estado temporal, según [la política acordada](README.md).

## Fuentes y pendientes

**Fuentes:** [CU-08 de Turnos](../../../backend-turnos/docs/use-cases/CU-08-cancelar-reserva.md); [anexo, secciones 12 y 15.10](../../../INTEGRATION_REFERENCE-v2.md).

**Pendiente para la SPEC/PLAN:** textos del diálogo y presentación del resultado pendiente; acordar contrato y recuperación de respuesta perdida. No enviar un motivo elegido libremente desde la app.
