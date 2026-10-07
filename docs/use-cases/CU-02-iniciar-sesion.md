# CU-02. Iniciar sesión

**Estado:** Revisado y aprobado por el usuario el 2026-10-07. Política de conservación de sesión confirmada por el usuario el 2026-10-07.

## Identificación

| Campo | Descripción |
| --- | --- |
| Sistema | Aplicación KMP (Android). |
| Objetivo | Acceder a las APIs protegidas con la identidad del usuario final. |
| Actor principal | Usuario final. |
| Actor de apoyo | Backend Turnos. |
| Disparador | El usuario elige iniciar sesión o CU-01 ejecuta el login automático. |

## Precondiciones

- Existe una cuenta activa; se dispone de sus credenciales para autenticarla.

## Flujo principal

1. El usuario envía sus credenciales, o [CU-01](CU-01-registrar-usuario.md) inicia la autenticación con las recién registradas.
2. La app comunica que está autenticando y evita envíos repetidos simultáneos.
3. Turnos verifica credenciales y devuelve el JWT de usuario.
4. La app establece la sesión y usa ese JWT para acceder directamente a Turnos y Catálogo.
5. La app consulta si existe un proceso pendiente propio sin bloquear el acceso al resto de la aplicación: abre [CU-08](CU-08-consultar-proceso.md) automáticamente solo si verifica que existe; si verifica que no existe, abre Búsqueda. Ofrece acceso a reservas propias y cierre de sesión.

## Flujos alternativos y de excepción

- **3a. Credenciales rechazadas:** informa el rechazo y permite corregir, sin establecer sesión; vuelve al paso 1.
- **3b. Turnos inaccesible o respuesta no obtenida:** informa que no pudo completar el login; termina sin acceso autenticado.
- **5a. No se puede consultar el proceso con sesión válida:** abre Búsqueda con un aviso no bloqueante y opción de reintentar. Conserva la sesión y permite usar las demás funciones según la disponibilidad de sus servicios; no interpreta el fallo como ausencia de proceso. El límite de un pendiente sigue siendo verificado por Turnos antes de iniciar otra reserva. Si detecta vencimiento o rechazo del JWT de usuario como inválido, aplica la alternativa de cierre local.
- **Después del paso 4, cierre y reapertura:** conserva sesión mientras el JWT siga válido y consulta el proceso como en el paso 5. Reabrir no equivale a un nuevo login.
- **Después del paso 4, vencimiento detectado o JWT de usuario rechazado como inválido:** termina el acceso local con la misma limpieza que [CU-03](CU-03-cerrar-sesion.md): retira la sesión y las vistas privadas, descarta formularios/filtros/selecciones temporales y solicita login. No abandona ni cancela procesos; tras autenticar, consulta el progreso propio en Turnos, sin crear otro hold, extenderlo ni repetir una operación enviada de resultado desconocido.
- **Después del paso 4, permiso insuficiente (403) o fallo de comunicación/servicio:** informa el error correspondiente; no cierra automáticamente la sesión ni lo trata como JWT de usuario inválido. Un problema de autenticación técnica entre servicios tampoco demuestra invalidez de la sesión final.

## Postcondiciones

- **Éxito:** sesión iniciada, identidad aplicada a las consultas y recorrido inicial definido.
- **Fallo:** no se habilita acceso protegido sin autenticación válida.

## Reglas y relaciones

- Registro incluye este caso; también puede iniciarse por separado.
- Conservar sesión hasta vencimiento y ofrecer cierre explícito fue confirmado en esta revisión. No se presupone renovación automática ni se conserva la contraseña para reautenticar.
- Comprobar vencimiento al abrir/reabrir la app, volver del segundo plano y antes de cada petición protegida. Si se detecta vencido, no enviar esa petición y aplicar el cierre local. Un rechazo de autenticación del JWT de usuario por el backend (401) también exige el cierre local, aunque el control local no detectara vencimiento.
- No cerrar sesión por un temporizador mientras la persona permanece en una pantalla. Detectar el vencimiento en los momentos anteriores o al recibir el rechazo del backend; una operación ya enviada no se cancela ni se presume fallida por vencer la sesión.
- Al detectar vencimiento o invalidez del JWT se aplica la misma limpieza local que cerrar sesión manualmente, también si después se autentica la misma persona. Las respuestas en curso no restablecen la sesión ni restauran los datos descartados; una respuesta de una sesión anterior tampoco cierra una sesión nueva.
- Ambos backends validan el JWT. El control local de sesión no sustituye la autorización por propietario en Turnos.
- Sin login offline ni cola de credenciales. La [política de estado temporal](README.md) no convierte el formulario de login en credenciales conservadas para reautenticar.
- La consulta inicial del pendiente no condiciona el éxito del login ni mantiene al usuario en una espera que impida usar el resto de la app. Solo se abre automáticamente un proceso cuya existencia propia se verificó.

## Fuentes y pendientes

**Fuentes:** [enunciado, secciones 3.2 y 9](../../../PROJECT_STATEMENT-v1.md); [CU-02 de Turnos](../../../backend-turnos/docs/use-cases/CU-02-autenticar-usuario.md); [decisiones de esta revisión](README.md).

**Pendiente para la SPEC:** validaciones del login y presentación del aviso no bloqueante/reintento. La navegación ante fallo de consulta inicial ya está acordada.

**Pendiente para el PLAN:** contrato de autenticación, vigencia acordada con Turnos y mecanismo seguro de conservación del JWT. La duración del JWT técnico de cátedra no determina la sesión final.
