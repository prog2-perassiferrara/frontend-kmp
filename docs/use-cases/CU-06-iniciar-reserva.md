# CU-06. Iniciar reserva

**Estado:** Revisado y aprobado por el usuario el 2026-10-07.

## Identificación

| Campo | Descripción |
| --- | --- |
| Sistema | Aplicación KMP (Android). |
| Objetivo | Iniciar el proceso propio del horario seleccionado con una sola acción. |
| Actor principal | Usuario final. |
| Actor de apoyo | Backend Turnos. |
| Disparador | El usuario solicita reservar el horario seleccionado. |

## Precondiciones

- Hay sesión válida y una selección de profesional, fecha y horario.
- El recorrido normal requiere no tener otro proceso pendiente; Turnos verifica ese límite, incluso ante concurrencia.

## Flujo principal

1. La app muestra la selección y el usuario solicita reservarla.
2. La app comunica el envío en curso y evita que pulsaciones repetidas generen solicitudes simultáneas de inicio.
3. Turnos valida información vigente y propietario, registra el proceso y crea e inicia la confirmación REST del hold.
4. La app recibe el estado conocido, identifica el proceso e informa que la reserva sigue en curso.
5. Continúa mediante [CU-08](CU-08-consultar-proceso.md); cuando se solicite teléfono, el usuario puede ejecutar [CU-07](CU-07-completar-informacion.md).

## Flujos alternativos y de excepción

- **3a. Horario ocupado o selección invalidada:** informa el rechazo conocido y permite volver a CU-05; no muestra reserva creada.
- **3b. Ya existe proceso pendiente o cierre incierto:** informa la condición y ofrece consultar el existente en CU-08, sin iniciar otro.
- **3c. Falta información vigente:** informa la indisponibilidad; termina sin habilitar un hold sobre datos no verificables.
- **3d/4a. Timeout, respuesta perdida o interrupción:** informa resultado sin verificar y consulta Turnos antes de repetir acciones con efectos. No presupone que el hold falló ni pide iniciar otro a ciegas.
- **4b. Ya se conoce información requerida o resultado final:** presenta el progreso más reciente recibido de Turnos; no impone una espera anterior.

## Postcondiciones

- **Éxito:** proceso propio identificado y seguimiento disponible; la aceptación inicial no garantiza reserva confirmada.
- **Alternativa:** rechazo conocido o incertidumbre explícita, sin duplicar el inicio.

## Reglas y relaciones

- Nombres del paciente y propiedad provienen de la cuenta autenticada; la app no los sustituye por datos de otra persona.
- Un proceso pendiente por usuario, también mientras se verifica su cierre. La UI no sustituye ese control de Turnos.
- Salir de la pantalla no abandona. Se retoma sin extender vencimiento ni crear otro hold.
- No iniciar reservas offline ni conservar solicitudes en una cola para enviarlas después. Selección y formularios sin enviar siguen la [política temporal acordada](README.md); el proceso enviado se consulta en Turnos.

## Fuentes y pendientes

**Fuentes:** [CU-04 de Turnos](../../../backend-turnos/docs/use-cases/CU-04-iniciar-reserva.md); [anexo, secciones 9, 10 y 17](../../../INTEGRATION_REFERENCE-v2.md).

**Pendiente para la SPEC:** presentación exacta de selección, conflicto y resultado incierto.

**Pendiente contractual/PLAN:** acordar con Turnos identificación/consulta del inicio cuya respuesta se perdió. El backend también mantiene una limitación contractual para creación central sin identificadores; no prometer recuperación total ni inventar una API central.
