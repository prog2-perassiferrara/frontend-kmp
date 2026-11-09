# PLAN: Inicio, conservación y cierre de sesión

**Referencias:** [SPEC](SPEC.md), [CU-02](../../use-cases/CU-02-iniciar-sesion.md), [CU-03](../../use-cases/CU-03-cerrar-sesion.md), issues #5 y #6.
**Estado:** Implementado y verificado el 2026-11-08.
**Alcance:** login, restauración cifrada, vigencia, logout y rechazo 401. No incluye registro, navegación final ni operaciones de negocio.

## Decisiones

- `SessionManager` es la única fuente de sesión. Un `Mutex` serializa estado y almacenamiento; la red se espera fuera del mutex.
- `epoch` identifica cada apertura: se incrementa al iniciar un login y al limpiar. `requireCurrent` y `reject` comparan el epoch de la petición; una respuesta tardía no publica ni cierra una sesión posterior.
- `store.save`/`store.clear` y la publicación del estado se ejecutan bajo `NonCancellable` dentro del mutex; el logout espera una escritura aceptada y luego la elimina.
- Si `store.clear()` falla, se intenta `store.save("")` para invalidar el contenido residual. El estado queda sin sesión y `cleanupRequired` bloquea un login nuevo con `STORAGE_UNAVAILABLE` hasta que la limpieza se reintente.
- `JwtExpiry` lee solo `exp`; firma y permisos pertenecen a los backends. El cliente evita enviar un token que ya sabe vencido.
- `KtorAuthApi` aísla `POST /api/authenticate` y mapea 400/401 por estado y `code`; `AuthenticatedApiClient` inyecta el bearer y solo trata el 401 de una API propia como cierre de sesión.
- `AndroidEncryptedSessionStore` usa AES-GCM con clave en Android Keystore, `AtomicFile` y `noBackupFilesDir`; no guarda la contraseña ni usa `SharedPreferences`.
- La UI mantiene `LoginScreen`/`AuthenticatedScreen` con `Content()`, `SessionViewModel` y estado de Compose. No se agrega navegación ni `BottomBar`.
  - **Actualización al cierre (2026-11-09):** el área autenticada incorporó Navigation Compose con `MainScreen`, `AppDestination` y `AppBottomBar`; login y registro permanecen fuera.

## Archivos principales

| Ruta | Responsabilidad |
| --- | --- |
| `shared/src/commonMain/.../session/SessionManager.kt` | Coordinación de sesión, epoch y limpieza. |
| `shared/src/commonMain/.../session/SessionState.kt`, `SessionError.kt`, `JwtExpiry.kt` | Estado público, errores presentables y lectura de `exp`. |
| `shared/src/commonMain/.../session/data/AuthApi.kt`, `KtorAuthApi.kt`, `SessionStore.kt` | Contrato HTTP, implementación Ktor y almacenamiento. |
| `shared/src/androidMain/.../session/data/AndroidEncryptedSessionStore.kt` | Cifrado y persistencia Android. |
| `shared/src/androidMain/.../session/SessionLifecycleObserver.kt` | Control de vigencia al volver a primer plano. |
| `shared/src/commonMain/.../network/AuthenticatedApiClient.kt` | Bearer y aislamiento de respuestas por epoch. |
| `shared/src/commonMain/.../ui/session/` | ViewModel, estado, login y área autenticada transitoria. |
| `shared/src/androidMain/.../config/AppContainer.kt` | Cableado por proceso. |

## Validación ejecutada (2026-11-08)

- `./gradlew :shared:testAndroidHostTest` — BUILD SUCCESSFUL. 30 pruebas, 0 fallos: 13 de `SessionManager`, 7 de `AuthenticatedApiClient`, 5 de `KtorAuthApi` y 5 de `SessionViewModel`.
- `./gradlew :shared:connectedAndroidDeviceTest` — BUILD SUCCESSFUL. 3 pruebas del almacenamiento cifrado en el AVD Pixel_10_Pro, Android 17: lectura desde otra instancia con IV nuevo, ciphertext alterado y clave eliminada.
- `./gradlew :androidApp:assembleDebug`, `:androidApp:installDebug` y `:androidApp:lintDebug` — BUILD SUCCESSFUL, sin errores de lint.
- Emulador con Catálogo en `localhost:8080` y Turnos en `localhost:8081`: login real con un usuario ficticio, restauración tras force-stop, logout, reinicio posterior sin sesión y operación offline (restauración y logout). El archivo `no_backup/user-session.bin` (643 bytes) no contenía `eyJ` en claro.
- 401: token válido → 200 en `/api/reservations`; firma alterada → 401 `AUTHENTICATION_REQUIRED`. Una sonda temporal con `AuthenticatedApiClient` contra Turnos produjo `SESSION_REQUIRED` y limpió la sesión; la sonda se eliminó.

## Límites y pendientes

- El 401 no es alcanzable desde la UI actual porque no hay una pantalla protegida; se comprobó con el backend real y una sonda temporal, ya eliminada.
- La navegación posterior al login, la consulta inicial del proceso pendiente y las operaciones de negocio pertenecen a otros CU.
- No se crearon reservas ni holds ni se modificaron datos ajenos. La verificación usó solo un usuario ficticio.
- No se probó hardware físico ni otra plataforma; la evidencia Android es del AVD Pixel_10_Pro (Android 17).
