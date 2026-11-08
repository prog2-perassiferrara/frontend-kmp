# TASKS: Registro de usuario

**Referencias:** [SPEC](SPEC.md), [PLAN](PLAN.md), [CU-01](../../use-cases/CU-01-registrar-usuario.md), issue #4.
**Estado:** Completado y verificado el 2026-11-08.

**Chequeo previo:** coherencia RF → CA → PLAN → tareas comprobada; todos los CA tienen validación y ninguna tarea agrega alcance. Sin decisiones bloqueantes.

- [x] **T-01 · Consumir el alta de Turnos.**
  - **Objetivo:** implementar `NewAccount`, `RegistrationApi`, `KtorRegistrationApi`, `RegistrationError` y `RegistrationException`, con `201`, duplicado, validación y resultado incierto.
  - **Dependencias:** ninguna.
  - **Referencias:** RF-03, RF-04, RF-07, RF-10; CA-01, CA-03, CA-04, CA-05, CA-09.
  - **Validación:** `KtorRegistrationApiTest` (4 pruebas).

- [x] **T-02 · Validar, coordinar y autenticar.**
  - **Objetivo:** implementar `RegistrationUiState` y `RegistrationViewModel` con validación local, normalización, intento único, login automático y estados de cuenta creada o resultado incierto.
  - **Dependencias:** T-01.
  - **Referencias:** RF-01, RF-02, RF-05, RF-06, RF-08, RF-10; CA-01, CA-02, CA-04, CA-05, CA-06, CA-07, CA-09.
  - **Validación:** `RegistrationViewModelTest` (10 pruebas).

- [x] **T-03 · Presentar el formulario y navegar.**
  - **Objetivo:** implementar `RegistrationScreen`, enlazar Login ↔ Registro en `App`, y cablear `AppContainer` y `MainActivity`.
  - **Dependencias:** T-01, T-02.
  - **Referencias:** RF-01, RF-06, RF-08, RF-09, RF-10; CA-01, CA-06, CA-07, CA-08, CA-09.
  - **Validación:** `assembleDebug`, `lintDebug` y recorrido en emulador.

- [x] **T-04 · Verificar el flujo integrado.**
  - **Objetivo:** ejecutar pruebas host, compilar, instalar y validar alta, login automático, duplicado, validación, resultado incierto y regresión de CU-02/03 con los backends reales.
  - **Dependencias:** T-01 a T-03; backends y emulador disponibles.
  - **Referencias:** RF-01 a RF-10; CA-01 a CA-10.
  - **Validación:** evidencia del [PLAN](PLAN.md), sección «Validación ejecutada».

## Cierre

**CA y evidencia:**

- **CA-01:** alta real en el emulador con login automático y `user-session.bin` cifrado; `validDataRegistersNormalizedAccountAndSignsInAutomatically` y `postsExactRegistrationWithoutBearerAndConfirmsCreated`.
- **CA-02:** formulario vacío e inválido bloqueado en el emulador; `invalidFieldsBlockTheRequestAndShowFieldErrors`. La estructura mínima del correo se valida localmente y el formato completo se delega a `@Email`; `acceptsEmailsAdmittedByTheBackendWithoutFalseRejections` cubre direcciones Unicode, sin punto y con parte local entrecomillada.
- **CA-03:** `presentsBackendValidationErrorsByFieldWithoutSession` y mapeo de `fieldErrors` en `mapsValidationFieldsAndDuplicateWithoutRetrying`.
- **CA-04:** duplicado real en el emulador con datos conservados y sin sesión; `duplicateIdentifierKeepsTheFormAndDoesNotAuthenticate`.
- **CA-05:** aviso incierto observado con un fallo de transporte real y contraseña descartada; `requestTimeoutIsUncertainAndSentOnce` y `uncertainResultClearsThePasswordWithoutAuthenticatingOrRetrying`.
- **CA-06:** `failedAutomaticLoginReportsCreatedAccountAndNeverRepeatsRegistration`; no se forzó en el emulador.
- **CA-07:** `repeatedSubmitSendsOneRegistration`.
- **CA-08:** Login ↔ Registro y vuelta verificados en el emulador; logout vuelve a Login. Regresión de sesión con 30 pruebas previas.
- **CA-09:** contraseña ausente del archivo de sesión, del `toString` y del formulario al volver a Login; `leavingTheFormDiscardsThePassword` y `accountRepresentationDoesNotExposeThePassword`.
- **CA-10:** 30 pruebas de sesión, login real, restauración tras force-stop y logout con eliminación del almacenamiento cifrado.

**Regresión:** 44 pruebas host, `assembleDebug`, `installDebug` y `lintDebug` ejecutados sobre el árbol actual el 2026-11-08. La verificación real usó las cuentas ficticias `cu01patient` y `cu01final`, y no modificó los backends.

**Desviaciones del PLAN:** ninguna de alcance o comportamiento. El alta con login fallido quedó cubierta solo por prueba automatizada porque requiere inyectar una falla entre el registro y la autenticación.

**Ajuste autorizado (2026-11-08):** se retiró la regex local de correo y se delegó el formato completo a `@Email`; se simplificaron los nombres de campo de `fieldErrors` y el guard de envío único. CA-02 se actualizó para distinguir la validación estructural local del formato validado por Turnos. No cambia el alcance ni los contratos externos.
