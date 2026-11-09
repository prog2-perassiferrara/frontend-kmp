# SPEC: Inicio, conservación y cierre de sesión

**Estado:** Implementada y verificada el 2026-11-08.
**CU e issue de referencia:** [CU-02](../../use-cases/CU-02-iniciar-sesion.md), [CU-03](../../use-cases/CU-03-cerrar-sesion.md); issues #5 y #6.
**Fuentes:** [seguridad del enunciado](../../../../PROJECT_STATEMENT-v1.md), [CU-02](../../use-cases/CU-02-iniciar-sesion.md), [CU-03](../../use-cases/CU-03-cerrar-sesion.md), [contrato de integración](../../../../INTEGRATION_REFERENCE-v2.md) e [instrucciones KMP](../../../AGENTS.md).

## Objetivo

Permitir que una persona inicie sesión con su cuenta, use las APIs protegidas con su JWT, conserve la sesión mientras sea válida y la cierre explícitamente. El registro (CU-01) y la navegación de negocio quedan fuera de esta funcionalidad.

## Alcance

- **RF-01 · Login:** enviar usuario/correo y contraseña a Turnos, con un único intento simultáneo. El identificador se convierte a minúsculas sin recortar y admite hasta 254 caracteres; la contraseña debe tener entre 6 y 100 puntos de código Unicode.
- **RF-02 · Sesión:** guardar únicamente el JWT cifrado; nunca la contraseña ni el formulario. Publicar el estado de sesión sin exponer el token.
- **RF-03 · Restauración:** al iniciar el proceso, restaurar la sesión solo si el JWT guardado sigue vigente y es legible. Un token vencido, ilegible o un fallo de lectura no habilita acceso.
- **RF-04 · Vigencia:** comprobar el vencimiento al restaurar, al volver a primer plano y antes de cada petición protegida; no usar temporizador. No enviar un token que ya se sabe vencido.
- **RF-05 · Cierre:** cerrar sesión de forma local, sin depender de la red; eliminar el JWT, descartar el formulario y las vistas privadas, y volver al login.
- **RF-06 · Rechazo 401:** un 401 de una API protegida cierra la sesión local con la misma limpieza. Un 403, timeout o error de transporte no la cierra.
- **RF-07 · Aislamiento:** una respuesta, un login o un 401 tardío de una apertura anterior no publica datos ni cierra la sesión vigente, aunque sea de la misma cuenta.
- **RF-08 · Errores y secretos:** presentar errores de credenciales, entrada, servicio, respuesta o almacenamiento sin cuerpos HTTP ni detalles internos. El JWT, la contraseña y los cuerpos privados no aparecen en logs, mensajes ni `toString`.

## Fuera de alcance

- Registro (CU-01), consulta inicial del proceso pendiente, búsqueda, disponibilidad, reservas y navegación final.
- Renovación automática, temporizador de expulsión, login offline, cola de credenciales y revocación remota del JWT.
- Validación local de firma o permisos: corresponde a los backends.

## Flujo de usuario

1. Al abrir la app, se muestra el login si no hay una sesión restaurable; si la hay, se muestra el área autenticada transitoria.
2. La persona ingresa usuario/correo, contraseña y, si lo desea, Recordarme.
3. La app valida el formulario, muestra el progreso y evita envíos simultáneos.
4. Turnos responde. Si acepta, la app guarda el JWT cifrado y muestra `Sesión iniciada` con `Cerrar sesión`; si rechaza o falla, muestra el error y permite corregir.
5. Al volver a primer plano o antes de una petición protegida, si la sesión venció, se limpia y se pide login.
6. `Cerrar sesión` limpia la sesión y vuelve al login; no cancela procesos ni reservas en Turnos.

## Reglas y casos alternativos

- Recordarme se envía a Turnos; la vigencia efectiva la determina el `exp` del JWT. La app no renueva ni extiende la sesión por sí sola.
- Un fallo al limpiar el almacenamiento retira el acceso e intenta invalidar el contenido residual; un login nuevo queda bloqueado hasta que la limpieza se reintente con éxito.
- Una respuesta de una sesión anterior no restablece la sesión ni los datos descartados.
- El cierre por vencimiento o 401 usa la misma limpieza que el cierre manual, incluso si después se autentica la misma persona.

## Criterios de aceptación

- **CA-01 · RF-01, RF-02:** credenciales válidas inician sesión; el JWT queda cifrado y la contraseña no se persiste.
- **CA-02 · RF-01, RF-08:** credenciales rechazadas o respuesta no utilizable no crean sesión y muestran un error presentable.
- **CA-03 · RF-01:** identificador vacío o de más de 254 caracteres, o contraseña fuera de 6–100 puntos de código, impiden el envío; una segunda pulsación no inicia otro intento.
- **CA-04 · RF-03:** un JWT guardado y vigente restaura la sesión sin pedir credenciales.
- **CA-05 · RF-03, RF-04:** un JWT vencido, ilegible o un fallo de lectura no restaura la sesión, no se envía y se limpia.
- **CA-06 · RF-04:** al volver a primer plano o antes de una petición protegida, un JWT vencido cierra la sesión sin temporizador.
- **CA-07 · RF-05:** cerrar sesión elimina el JWT, descarta el formulario y vuelve al login; funciona sin red y una reapertura no restaura la sesión.
- **CA-08 · RF-05:** si la limpieza falla, se retira el acceso, se intenta invalidar el contenido residual, se bloquea un login nuevo hasta limpiar y se evita restaurar la sesión cerrada cuando la invalidación se completa.
- **CA-09 · RF-06:** un 401 de una API protegida cierra la sesión; un 403, timeout o error de transporte la conserva.
- **CA-10 · RF-07:** un login, una respuesta o un 401 tardío de una apertura anterior no publica datos ni cierra la sesión vigente.
- **CA-11 · RF-02, RF-08:** el JWT, la contraseña y los cuerpos privados no aparecen en logs, mensajes ni `toString`.

## Verificación

La evidencia por criterio y sus límites están en [TASKS.md](TASKS.md). Las decisiones técnicas y los comandos están en [PLAN.md](PLAN.md).
