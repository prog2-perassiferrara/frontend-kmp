# SPEC: Registro de usuario

**Estado:** Implementada y verificada el 2026-11-08.
**CU e issue de referencia:** [CU-01](../../use-cases/CU-01-registrar-usuario.md); issue #4.
**Fuentes:** [CU-01](../../use-cases/CU-01-registrar-usuario.md), [contrato de Turnos](../../../../backend-turnos/docs/CONTRATOS.md#api-propia-de-turnos), [CU-02](../../use-cases/CU-02-iniciar-sesion.md) e [instrucciones KMP](../../../AGENTS.md).

## Objetivo

Crear una cuenta activa de usuario final desde Android y dejarla autenticada reutilizando el flujo de sesión de CU-02, sin duplicar la cuenta cuando el login automático falla.

## Alcance

- **RF-01 · Formulario:** pedir `login`, `password`, `firstName`, `lastName` y `email`, los cinco campos obligatorios del contrato. Enviar `langKey` fijo `es` (valor por defecto del backend) y omitir `imageUrl`; no se ofrece carga de fotos.
- **RF-02 · Validación local:** replicar los límites del backend antes de enviar: login de 1 a 50 caracteres con el patrón JHipster; contraseña de 6 a 100 puntos de código Unicode; nombre y apellido no blancos de 2 a 50; correo válido de 5 a 254. No recortar nombres, login ni contraseña.
- **RF-03 · Alta:** consumir `POST /api/register` sin bearer y con JSON. Un `201` vacío confirma la creación; no devuelve JWT.
- **RF-04 · Errores del backend:** un `400 VALIDATION_ERROR` se presenta por campo cuando `fieldErrors` lo permite; un `400 USER_IDENTIFIER_ALREADY_USED` informa que el usuario o el correo ya existen. Ninguno crea sesión.
- **RF-05 · Login automático:** tras un `201`, autenticar con las mismas credenciales mediante `SessionManager.signIn(..., rememberMe = false)`. Si responde, la sesión queda iniciada y se accede al área autenticada.
- **RF-06 · Alta sin sesión:** si el registro confirma la cuenta pero el login automático falla, informar que la cuenta fue creada, limpiar la contraseña y ofrecer iniciar sesión por separado. No repetir el registro.
- **RF-07 · Resultado incierto:** un timeout, error de transporte, `5xx` o redirección no se presenta como éxito. Se informa que el resultado no pudo confirmarse, no se reintenta automáticamente y se ofrece iniciar sesión para verificarlo.
- **RF-08 · Envíos repetidos:** un único intento simultáneo. Pulsaciones repetidas no generan otra petición; los campos y el botón se deshabilitan mientras se envía.
- **RF-09 · Navegación mínima:** permitir ir de Login a Registro y volver, sin Navigation Compose ni BottomBar. Al autenticarse, la app muestra el área autenticada existente.
- **RF-10 · Datos sensibles:** la contraseña no se persiste ni aparece en logs, mensajes o `toString`. Se descarta al completar el registro, al quedar el resultado incierto y al volver al login.

## Fuera de alcance

- Carga o presentación de imágenes (`imageUrl`), selección de idioma distinta de `es`, verificación por correo, recuperación de cuenta y edición de perfil.
- Navegación general, BottomBar, registro de cuentas técnicas y cambios en los backends.
- Renovación automática del JWT, revocación remota o consultas offline.

## Flujo de usuario

1. Desde Login, la persona elige “Crear cuenta”.
2. Completa login, contraseña, nombre, apellido y correo; la app valida y muestra el progreso.
3. Turnos responde `201`; la app autentica sin pedir otra acción.
4. Con login correcto se muestra el área autenticada. Si el login falla, se informa “Cuenta creada” y se ofrece ir a Login.
5. Ante datos inválidos o duplicados, la persona corrige y reenvía. Ante un resultado incierto, puede ir a Login para comprobar si la cuenta existe.

## Reglas y casos alternativos

- El backend es la autoridad final de validación y unicidad; la validación local evita viajes innecesarios.
- Login y correo se normalizan a minúsculas antes de enviar y de autenticar, igual que el backend.
- Un duplicado no autentica ni demuestra que la solicitud anterior fuera del mismo cliente.
- Un `5xx` puede ocurrir después de confirmar la escritura; por eso no se afirma fallo definitivo ni se repite el alta automáticamente.
- La cuenta creada permanece activa aunque falle el login automático; no se solicita un nuevo registro.
- No se conserva la contraseña para reintentar el login: se pide nuevamente en Login.

## Criterios de aceptación

- **CA-01 · RF-01, RF-03, RF-05:** datos válidos crean la cuenta y dejan la sesión iniciada con el JWT emitido por Turnos.
- **CA-02 · RF-02:** login, contraseña, nombre, apellido o correo inválidos impiden la petición y muestran el error de campo.
- **CA-03 · RF-04:** `VALIDATION_ERROR` se presenta por campo cuando el backend lo informa; no se crea sesión.
- **CA-04 · RF-04:** un identificador duplicado se informa como tal, sin sesión y sin borrar los datos para corregir.
- **CA-05 · RF-07:** timeout, error de transporte, `5xx` o redirección se presentan como resultado incierto, sin éxito ni reintento automático.
- **CA-06 · RF-06:** cuenta creada y login fallido muestran “Cuenta creada”, conservan la cuenta y no repiten el registro.
- **CA-07 · RF-08:** pulsaciones repetidas generan una sola petición de registro.
- **CA-08 · RF-09:** Login y Registro se alternan; al autenticarse se muestra el área autenticada y al cerrar sesión se vuelve a Login.
- **CA-09 · RF-10:** la contraseña no se persiste ni aparece en `toString`; se limpia al completar, al quedar incierta y al volver a Login.
- **CA-10 · CU-02/03:** login, restauración, logout y rechazo 401 conservan su comportamiento.

## Verificación

La evidencia por criterio, los comandos y los límites están en [TASKS.md](TASKS.md). Las decisiones técnicas están en [PLAN.md](PLAN.md).
