# CU-08. Consultar y retomar proceso

**Estado:** Revisado y aprobado por el usuario el 2026-10-07. Apertura del proceso pendiente al entrar confirmada el 2026-10-07.

## Identificación

| Campo | Descripción |
| --- | --- |
| Sistema | Aplicación KMP (Android). |
| Objetivo | Conocer el progreso propio y retomar solo las acciones todavía permitidas. |
| Actor principal | Usuario propietario del proceso. |
| Actor de apoyo | Backend Turnos. |
| Disparador | Consulta explícita, seguimiento del inicio o entrada/reapertura con un proceso pendiente. |

## Precondiciones

- Hay sesión válida. El usuario identifica un proceso propio o la app consulta el pendiente de su cuenta.

## Flujo principal

1. La app consulta Turnos para el proceso propio, sin iniciar un nuevo hold.
2. Turnos verifica propiedad y devuelve progreso, información requerida, vencimiento y grado de verificación disponibles.
3. La app muestra el estado de ese proceso, distinguiendo espera, información requerida, resultado conocido e incertidumbre.
4. Si permite continuar, ofrece completar teléfono con CU-07 o abandonar explícitamente con CU-11.
5. Al recibir el resultado final desde Turnos, lo presenta y deja de ofrecer acciones incompatibles.

## Flujos alternativos y de excepción

- **1a/2d. Vencimiento detectado o JWT de usuario rechazado como inválido:** aplica la limpieza local y requiere [CU-02](CU-02-iniciar-sesion.md); después vuelve a consultar el proceso, sin reiniciarlo ni repetir operaciones de resultado desconocido. El proceso ya enviado sigue su curso en Turnos.
- **2a. Proceso inexistente o no autorizado:** informa el rechazo sin exponer datos ajenos; termina la consulta.
- **2b. Turnos inaccesible:** informa que no pudo actualizar el progreso; no deduce éxito, fallo o ausencia de proceso. Termina el intento de consulta.
- **2c. Turnos entrega estado local sin verificación central:** muestra el aviso y las acciones permitidas por el backend; continúa en el paso 3.
- **3a. Plazo mostrado agotado con resultado incierto:** impide nuevos envíos de teléfono; consulta el resultado en Turnos sin declarar vencimiento central verificado ni habilitar otro proceso.
- **3b. Proceso terminado:** muestra resultado conocido, sin reabrirlo; continúa en el paso 5.
- **4a. Volver atrás o cerrar la app:** deja la consulta y conserva el proceso en Turnos; no ejecuta CU-11. Al regresar, vuelve al paso 1.

## Postcondiciones

- **Éxito:** el usuario conoce el progreso y las acciones permitidas según el estado recibido.
- **Fallo:** la falta de actualización queda explícita y no produce acciones con efectos nuevas.

## Reglas y relaciones

- Tras login o reapertura con sesión válida, se abre automáticamente el pendiente propio solo si se verifica que existe. Si no existe o la consulta inicial falla, se abre Búsqueda; ante fallo, se ofrece aviso no bloqueante y reintento, sin deducir ausencia ni omitir el control de Turnos al iniciar otra reserva. La consulta inicial no bloquea el acceso al resto de la app. Decisión confirmada en esta revisión.
- Consultar procesos anteriores no los sustituye por el pendiente ni mezcla sus resultados.
- Turnos recupera y reconcilia REST/Kafka. KMP no reproduce su máquina de estados central ni usa el reloj del dispositivo como prueba de cancelación o expiración.
- Aplicar el control de sesión de CU-02 al abrir/reabrir, volver del segundo plano y antes de consultar o enviar una acción protegida, además del rechazo de autenticación del backend. No interrumpir la pantalla por un temporizador de sesión ni confundir falta de permiso o indisponibilidad con JWT de usuario inválido.
- Sin consulta offline del progreso ni cola de acciones. Tras terminar el proceso Android se descarta el estado temporal y se consulta Turnos con la sesión conservada o después de login, según [la política acordada](README.md).

## Fuentes y pendientes

**Fuentes:** [CU-06 de Turnos](../../../backend-turnos/docs/use-cases/CU-06-consultar-proceso.md); [CU-09 de Turnos](../../../backend-turnos/docs/use-cases/CU-09-recuperar-procesos.md); [decisión de navegación](README.md).

**Pendiente para la SPEC:** vocabulario visible y navegación durante incertidumbre y al finalizar. Aplicar la política de conservación temporal ya acordada.

**Pendiente para el PLAN:** contrato de consulta y obtención del pendiente por identidad autenticada, actualización del progreso y manejo de respuestas tardías. No se presupone un identificador central conocido tras una respuesta de inicio perdida.
