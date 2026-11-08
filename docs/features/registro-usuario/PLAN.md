# PLAN: Registro de usuario

**Referencias:** [SPEC](SPEC.md), [CU-01](../../use-cases/CU-01-registrar-usuario.md), [CU-02](../../use-cases/CU-02-iniciar-sesion.md), issue #4.
**Estado:** Implementado y verificado el 2026-11-08.
**Alcance:** formulario, validación local, alta contra Turnos y login automático. No incluye navegación general, imágenes, idioma alternativo ni cambios en los backends.

## Decisiones

- `RegistrationApi` aísla `POST /api/register`; `KtorRegistrationApi` usa el mismo `HttpClient` y `ApiConfiguration` que la sesión, sin bearer, cookies ni reintentos.
- `201` es la única confirmación válida y no requiere cuerpo. `400` con `code = USER_IDENTIFIER_ALREADY_USED` informa duplicado; `400` con `VALIDATION_ERROR` o sin código se presenta como entrada inválida y conserva los `fieldErrors` (`field`, `code`) para ubicarlos.
- Cualquier otro estado, una redirección, un timeout o un fallo de transporte se trata como resultado incierto: no se afirma éxito ni fallo definitivo y no se repite la petición automáticamente. Un `5xx` puede ocurrir después de crear la cuenta.
- La validación local replica el contrato de `RegistrationRequest`: login normalizado de 1 a 50 con el patrón JHipster; contraseña de 6 a 100 puntos de código; nombre y apellido no blancos de 2 a 50; correo de 5 a 254 con dominio con punto. El backend sigue siendo la autoridad final.
- Login y correo se normalizan a minúsculas antes de enviar y de autenticar; no se recortan nombres, login ni contraseña. Las contraseñas con espacios siguen siendo válidas si cumplen la longitud.
- `RegistrationViewModel` valida, normaliza y ejecuta un único intento mediante un `Job`; las pulsaciones repetidas no crean otra petición. La red se espera fuera de cualquier mutex de sesión.
- Tras el `201`, el ViewModel reutiliza `SessionManager.signIn(login, password, rememberMe = false)`. No duplica almacenamiento ni estado de sesión.
- Si el alta confirma la cuenta y el login automático falla, el estado queda en `accountCreated`: se limpia la contraseña, se deshabilitan los campos y solo se ofrece volver a Login. No se repite el registro.
- Un resultado incierto limpia la contraseña, conserva los demás datos para corregir y permite un intento manual; el aviso indica verificar mediante Login.
- La contraseña vive solo en el ViewModel mientras se edita; no se persiste ni aparece en `toString`. Se limpia al autenticar, al quedar incierta, al confirmar la cuenta sin sesión y al volver a Login.
- La navegación mínima es un booleano `rememberSaveable` en `App.Content`: Login ↔ Registro. Al cambiar la sesión a `SIGNED_IN`, el árbol existente muestra el área autenticada. No se agrega Navigation Compose ni BottomBar.
- El cableado se mantiene en `AppContainer` y `MainActivity`, con dependencias por constructor.

## Archivos principales

| Ruta | Responsabilidad |
| --- | --- |
| `shared/src/commonMain/.../registration/RegistrationError.kt` | Errores presentables y excepción con `fieldErrors`. |
| `shared/src/commonMain/.../registration/data/RegistrationApi.kt`, `KtorRegistrationApi.kt` | Contrato de alta, `NewAccount` y consumo Ktor. |
| `shared/src/commonMain/.../ui/registration/RegistrationUiState.kt`, `RegistrationViewModel.kt`, `RegistrationScreen.kt` | Estado, validación, intento único, login automático y formulario Compose. |
| `shared/src/commonMain/.../App.kt` | Navegación mínima Login ↔ Registro. |
| `shared/src/commonMain/.../ui/session/LoginScreen.kt` | Acceso “Crear cuenta”. |
| `shared/src/androidMain/.../config/AppContainer.kt` | Cableado de `KtorRegistrationApi`. |
| `androidApp/src/main/.../MainActivity.kt` | Composición de la pantalla nueva. |
| `shared/src/commonTest/.../network/KtorRegistrationApiTest.kt` | Contrato HTTP y mapeo de resultados. |
| `shared/src/commonTest/.../ui/registration/RegistrationViewModelTest.kt` | Validación, intento único, errores y login automático. |

## Validación ejecutada (2026-11-08)

- `./gradlew :shared:testAndroidHostTest` — BUILD SUCCESSFUL. 43 pruebas, 0 fallos: 30 previas de sesión y 13 nuevas (4 de `KtorRegistrationApi`, 9 de `RegistrationViewModel`).
- `./gradlew :androidApp:assembleDebug`, `:androidApp:installDebug` y `:androidApp:lintDebug` — BUILD SUCCESSFUL, sin errores.
- Emulador Pixel_10_Pro (Android 17) con Turnos en `localhost:8081`: formulario vacío validado localmente; alta real con `CU01PATIENT` normalizado a `cu01patient`, `lang_key = es` y cuenta activa; login automático; archivo `no_backup/user-session.bin` de 643 bytes sin `eyJ` en claro; duplicado informado sin sesión ni borrado de datos; vuelta a Login con contraseña descartada.
- Un intento con el permiso Android `ACCESS_LOCAL_NETWORK` revocado produjo el aviso de resultado incierto y limpió la contraseña; tras concederlo, el alta y el login automático funcionaron.
- Regresión CU-02/03: login real, restauración tras force-stop y logout con eliminación del archivo de sesión. No se modificaron los backends ni se usaron datos reales.

## Límites y pendientes

- No se probó hardware físico ni otra plataforma; la evidencia Android es del AVD Pixel_10_Pro.
- El alta confirmada con login automático fallido no se forzó en el emulador porque requiere inyectar una falla entre ambas llamadas; está cubierta por `failedAutomaticLoginReportsCreatedAccountAndNeverRepeatsRegistration`.
- La navegación general y el área autenticada definitiva pertenecen a los CU de búsqueda y reservas.
- El formulario no ofrece carga de imágenes ni selección de idioma; se envía `langKey = "es"` y se omite `imageUrl`.
