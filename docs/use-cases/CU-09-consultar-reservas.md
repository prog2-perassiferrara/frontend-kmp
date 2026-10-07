# CU-09. Consultar reservas propias

**Estado:** Revisado y aprobado por el usuario el 2026-10-07.

## Identificación

| Campo | Descripción |
| --- | --- |
| Sistema | Aplicación KMP (Android). |
| Objetivo | Ver reservas y procesos propios con estados diferenciados. |
| Actor principal | Usuario final. |
| Actor de apoyo | Backend Turnos. |
| Disparador | El usuario abre o actualiza Mis reservas. |

## Precondiciones

- Hay sesión válida del usuario.

## Flujo principal

1. El usuario abre la consulta de reservas propias.
2. La app muestra carga y solicita los registros a Turnos con su identidad autenticada.
3. Turnos devuelve registros propios y estado de actualización.
4. La app presenta reservas confirmadas, canceladas y fallidas, y diferencia los procesos pendientes.
5. El usuario puede consultar un proceso mediante CU-08 o solicitar la cancelación de una reserva confirmada con CU-10.

## Flujos alternativos y de excepción

- **3a. Sin registros propios:** muestra lista vacía; si Turnos solo pudo consultar sus datos locales, conserva ese aviso. Termina correctamente.
- **3b. Cátedra inaccesible pero Turnos responde con registros locales:** presenta los datos propios con aviso de posible desactualización; continúa en el paso 4.
- **2a/3c. Sin conexión con Turnos:** informa fallo de consulta y permite volver a consultar, sin confundirlo con lista vacía ni resolver la vista offline desde datos guardados del dispositivo.
- **3d. Registros con resultado incierto:** conserva la indicación recibida; permite consultar el proceso, sin inventar un estado definitivo.

## Postcondiciones

- **Éxito:** registros propios o lista vacía con estados y actualización diferenciados.
- **Fallo:** queda explícito que no pudo realizarse la consulta.

## Reglas y relaciones

- No requiere Catálogo para consultar datos propios en Turnos; la caída de Catálogo no bloquea esta vista.
- La app no elige un `externalPatientId` arbitrario ni muestra el listado de toda la cuenta técnica.
- Datos históricos del profesional permiten describir la reserva, pero no validar una nueva disponibilidad.
- Un proceso pendiente no se presenta como reserva confirmada. Se preserva la identidad e historial de cada registro.
- Sin consultas offline ni cola de operaciones. Las selecciones y filtros siguen la [política temporal confirmada](README.md).

## Fuentes y pendientes

**Fuentes:** [CU-07 de Turnos](../../../backend-turnos/docs/use-cases/CU-07-consultar-reservas.md); [anexo, sección 11](../../../INTEGRATION_REFERENCE-v2.md).

**Pendiente para la SPEC:** campos visibles, filtros, orden, paginación y disposición del proceso pendiente e historial. El orden central no impone el orden de la app.

**Pendiente para el PLAN:** contrato de listado propio, actualización y relación con consulta de procesos.
