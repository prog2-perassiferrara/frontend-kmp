# TASKS: Inicio, conservación y cierre de sesión

**Referencias:** [SPEC](SPEC.md), [PLAN](PLAN.md), [CU-02](../../use-cases/CU-02-iniciar-sesion.md), [CU-03](../../use-cases/CU-03-cerrar-sesion.md), issues #5 y #6.
**Estado:** Completado y verificado el 2026-11-08.

**Chequeo previo:** coherencia RF → CA → PLAN → tareas comprobada; todos los CA tienen validación y ninguna tarea agrega alcance. Sin decisiones bloqueantes.

- [x] **T-01 · Coordinar estado, restauración y vigencia.**
  - **Objetivo:** implementar `SessionManager`, `SessionState`, `SessionError`, `JwtExpiry`, el epoch y la limpieza local.
  - **Dependencias:** ninguna.
  - **Referencias:** RF-02 a RF-08; CA-01, CA-04 a CA-11.
  - **Validación:** `SessionManagerTest` (13 pruebas) cubre restauración, vencimiento, login, logout, limpieza fallida y respuestas tardías.

- [x] **T-02 · Persistir el JWT cifrado.**
  - **Objetivo:** implementar `AndroidEncryptedSessionStore` con AES-GCM, clave en Keystore, `AtomicFile` y `noBackupFilesDir`.
  - **Dependencias:** T-01.
  - **Referencias:** RF-02, RF-03, RF-05, RF-08; CA-01, CA-04, CA-05, CA-07, CA-08, CA-11.
  - **Validación:** `AndroidEncryptedSessionStoreTest` (3 pruebas) en dispositivo: lectura desde otra instancia, ciphertext alterado y clave eliminada.

- [x] **T-03 · Consumir autenticación y APIs protegidas.**
  - **Objetivo:** implementar `AuthApi`, `KtorAuthApi` y `AuthenticatedApiClient`, con mapeo de errores y tratamiento del 401.
  - **Dependencias:** T-01.
  - **Referencias:** RF-01, RF-06, RF-07, RF-08; CA-01, CA-02, CA-09, CA-10.
  - **Validación:** `KtorAuthApiTest` (5 pruebas) y `AuthenticatedApiClientTest` (7 pruebas).

- [x] **T-04 · Presentar login, errores y cierre.**
  - **Objetivo:** implementar `SessionViewModel`, `LoginScreen` y `AuthenticatedScreen`, con validación, un intento simultáneo y descarte del formulario.
  - **Dependencias:** T-01, T-03.
  - **Referencias:** RF-01, RF-05, RF-08; CA-01, CA-02, CA-03, CA-07, CA-11.
  - **Validación:** `SessionViewModelTest` (5 pruebas).

- [x] **T-05 · Verificar el flujo integrado.**
  - **Objetivo:** ejecutar pruebas host y de dispositivo, compilar e instalar, y validar login, restauración, logout, vigencia y 401 con los backends reales.
  - **Dependencias:** T-01 a T-04; backends y emulador disponibles.
  - **Referencias:** RF-01 a RF-08; CA-01 a CA-11.
  - **Validación:** evidencia del [PLAN](PLAN.md), sección «Validación ejecutada».

## Cierre

**CA y evidencia:**

- **CA-01:** login real en el emulador y `user-session.bin` cifrado de 643 bytes sin `eyJ`; `SessionManagerTest` y `SessionViewModelTest`.
- **CA-02:** `doesNotPersistRejectedOrUnusableAuthentication`, `mapsStatusAndCodeWithoutShowingServerDetailsOrRetrying` y mensaje de error del login.
- **CA-03:** `validatesPasswordLengthWithoutChangingTheValue`, `validatesIdentifierLengthAfterNormalizationWithoutTrimming` y `repeatedSubmitKeepsOneAttemptAndSuccessfulLoginDiscardsTheForm`.
- **CA-04:** restauración observada tras force-stop y `restoresOnlyOnceWithoutCallingAuthentication`.
- **CA-05:** `discardsExpiredAndMalformedStoredTokens`, `readFailureDoesNotGrantAccessAndCleansTheUnusableStore` y `knownExpiryPreventsAnyHttpRequest`.
- **CA-06:** `expiryIsCheckedAtControlPointsWithoutAnIdleTimer` y `successfulInFlightResponseIsNotRejectedOnlyBecauseTimePassed`.
- **CA-07:** logout observado en el emulador, también sin red; `logoutClearsLocallyWithoutAuthenticatingAgain` y `logoutDiscardsTheTemporaryForm`.
- **CA-08:** `cleanupFailureWithdrawsAccessAndBlocksLoginUntilAStoreClearSucceeds` y `failedCleanupCannotRestoreTheClosedSessionInANewProcess`.
- **CA-09:** `onlyUserAuthenticationRejectionClearsTheSession`, `transportFailurePreservesTheCurrentSession`, `requestTimeoutFailsOnceWithoutGrantingAccess` y 401 real con token alterado.
- **CA-10:** `lateLoginCannotReplaceANewSession`, `oldResponsesCannotPublishDataOrCloseANewSession` y sonda temporal con el cliente real.
- **CA-11:** almacenamiento cifrado, `SessionSnapshot`/`SessionUiState`/`ApiResponse` con `toString` redactado y ausencia de datos sensibles en los mensajes.

**Regresión:** 30 pruebas host, 3 pruebas de dispositivo, `assembleDebug`, `installDebug` y `lintDebug` ejecutados sobre el árbol actual el 2026-11-08. La única modificación de código durante la verificación fue el fallback de limpieza (`store.save("")`), cubierto por CA-08. No se crearon reservas ni se modificaron datos ajenos.

**Desviaciones del PLAN:** ninguna de alcance o comportamiento. El 401 no se puede provocar desde la UI porque todavía no hay una pantalla protegida; se verificó con backend real y una sonda temporal eliminada.
