<!-- evaluacion:inicio -->
# Evaluación — prog2-perassiferrara/frontend-kmp

> Documento **generado automáticamente** por la cátedra de Programación II (UM): RepoSense + análisis de calidad de **7 dimensiones**. **No editar a mano**: se regenera en cada evaluación.
> Rama evaluada: `main`. Análisis **heurístico** (regex + métricas) + build: es una criba para orientar la revisión, **no una nota**. La **documentación** del repositorio no se evalúa.
> Generado: 2026-11-17.

## 1. Veredicto

| Aspecto | Resultado |
|---|---|
| Índice de calidad | **41/100** |
| Tipo de repositorio | `app-kmp` |
| Build (`mvnw verify` / `gradlew check`) | OK |
| Código (LOC) | 9,035 |
| Archivos de código | 80 |
| Clases | 126 |
| Código dentro de clases (%POO) | 75% |
| Funciones fuera de clases | 32 |
| Duplicación (puerta) | 8.1% |
| Duplicación lógica | 8.2% |
| Duplicación de boilerplate | 17.8% |
| Tope duro aplicado a D1 | `SUELTAS>=10` → D1 ≤ 20 |

> Este informe **describe** lo que el análisis midió con los criterios ya establecidos de la materia. No es una nota.

## 2. Semáforo de aprobación

Criterios de la cátedra (`CRITERIOS-CALIDAD.md`): **Aprobar** se juega con el perfil `estandar`; **destacar**, con el `exigente` (meta de excelencia).

| Semáforo | Resultado |
|---|---|
| Aprobar (`estandar`) | **no cumple** — 4/11 |
| Destacar (`exigente`) | **no cumple** — 4/11 |

**Para aprobar, falta cumplir:** SOLID; MVVM; Duplicación

**Para destacar, falta cumplir:** SOLID; MVVM; Duplicación

## 3. Autoría (RepoSense)

| Autor | Commits | Código (LOC) | Tests (LOC) | Docs (LOC) | Días activos | % del código |
|---|---:|---:|---:|---:|---:|---:|
| Valentino Perassi Ferrara | 22 | 9,922 | 0 | 3,519 | 7 | 100% |

> _El LOC de autoría lo calcula RepoSense sobre el historial y puede diferir del LOC de código del análisis (que mira el estado de `main`)._

## 4. Dimensiones evaluadas

Cada dimensión indica **qué mide**, **por qué importa** y **tu resultado**. Cuando el puntaje queda por debajo de la referencia, se agregan preguntas para **investigar la causa** (no hay recetas).

| Dimensión | Peso | Puntaje |
|:--|--:|--:|
| **D1** Diseño OO, SOLID y patrones (20%) | 20% | 2 |
| **D2** Arquitectura hexagonal — backend (15%) | 15% | N/A |
| **D3** Frontend KMP: MVVM y buenas prácticas (10%) | 10% | 60 |
| **D4** Integración y contrato (10%) | 10% | N/A |
| **D5** Seguridad y robustez (10%) | 10% | 95 |
| **D6** Pruebas y cobertura (20%) | 20% | 50 |
| **D7** Proceso y entrega (15%) | 15% | 31 |

### D1. Diseño OO, SOLID y patrones (20%)

- **Qué mide:** Métricas orientadas a objetos (clases, métodos, campos, cohesión, acoplamiento, herencia) y proxies de SOLID, más patrones de diseño.
- **Por qué importa:** El eje de la materia es el buen uso de la POO y su arquitectura: un diseño con clases claras y poco acopladas es más fácil de entender, probar y cambiar.
- **Tu resultado:** **2/100**
- **Para investigar la causa:**
    - ¿Se pensó el modelo de clases antes de codificar o se agrupó después?
    - ¿Qué clases concentran muchas responsabilidades a la vez?
    - ¿Los colaboradores llegan como parámetros o se crean por dentro?

### D3. Frontend KMP: MVVM y buenas prácticas (10%)

- **Qué mide:** Uso de ViewModel, estado de UI y flujo unidireccional, y ausencia de acceso a red/BD dentro de la UI.
- **Por qué importa:** Separar la UI del estado y del acceso a datos hace la app más predecible, testeable y menos propensa a bugs de ciclo de vida.
- **Tu resultado:** **60/100**

### D5. Seguridad y robustez (10%)

- **Qué mide:** Credenciales embebidas, consultas SQL por concatenación, y presencia de config de entorno; aislamiento de secretos.
- **Por qué importa:** Un secreto en el repositorio o una consulta construida por concatenación son riesgos concretos, no hipotéticos.
- **Tu resultado:** **95/100**

### D6. Pruebas y cobertura (20%)

- **Qué mide:** Cantidad y resultado de pruebas y cobertura de línea (JaCoCo/Kover, Surefire/Failsafe) cuando hay build.
- **Por qué importa:** Las pruebas son la red de seguridad para refactorizar e integrar; la cobertura acota las zonas sin verificar.
- **Tu resultado:** **50/100**
- **Para investigar la causa:**
    - ¿Qué partes del sistema no tienen pruebas?
    - ¿Las pruebas cubren caminos de error o solo el caso feliz?
    - ¿Qué falla al ejecutar la suite completa (no solo la del módulo propio)?

### D7. Proceso y entrega (15%)

- **Qué mide:** Artefactos verificables de proceso/entrega: `compose`, `.env.example`, CI (`.github/workflows/`), `CODEOWNERS` y wrapper de build (`mvnw`/`gradlew`). **No** puntúa documentación.
- **Por qué importa:** Un proyecto que compila, se levanta y se valida solo es más fácil de revisar y de integrar en equipo.
- **Tu resultado:** **31/100**
- **Para investigar la causa:**
    - ¿El proyecto se levanta con un único comando desde el repo limpio?
    - ¿Qué pasa en CI cuando se sube un cambio?
    - ¿La configuración necesaria está versionada (sin secretos)?

## 5. Señales detectadas

El análisis encontró las siguientes señales (archivo, sin número de línea a propósito):

**POO** (60)
- función 'quotedUrl' fuera de una clase — `androidApp/build.gradle.kts`
- clase 'AppContainer' con 6 campos públicos (encapsulación) — `shared/src/androidMain/kotlin/com/prog2/frontend/config/AppContainer.kt`
- función 'initialCalendar' fuera de una clase — `shared/src/androidMain/kotlin/com/prog2/frontend/ui/components/AndroidDatePickerModal.kt`
- función 'startOfToday' fuera de una clase — `shared/src/androidMain/kotlin/com/prog2/frontend/ui/components/AndroidDatePickerModal.kt`
- función 'formatInstantInZone' fuera de una clase — `shared/src/androidMain/kotlin/com/prog2/frontend/ui/components/AndroidInstantFormat.kt`
- función 'parseInstant' fuera de una clase — `shared/src/androidMain/kotlin/com/prog2/frontend/ui/components/AndroidInstantFormat.kt`
- clase 'StartBody' con 16 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/reservation/data/KtorReservationsApi.kt`
- clase 'PhoneBody' con 12 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/reservation/data/KtorReservationsApi.kt`
- clase 'ProcessResponse' con 12 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/reservation/data/KtorReservationsApi.kt`
- clase 'ReservationSelection' con 10 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/reservation/data/ReservationsApi.kt`
- clase 'StartReservation' con 6 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/reservation/data/ReservationsApi.kt`
- clase 'ReservationProcess' con 23 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/reservation/data/ReservationsApi.kt`
- clase 'CancellationProgress' con 6 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/reservation/data/ReservationsApi.kt`
- clase 'OwnReservationsPage' con 6 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/reservation/data/ReservationsApi.kt`
- clase 'ProfessionalAvailability' con 4 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/availability/data/AvailabilityApi.kt`
- clase 'AvailabilityResponse' con 4 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/availability/data/KtorAvailabilityApi.kt`
- clase 'ProfessionalPageResponse' con 6 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/catalog/data/KtorCatalogApi.kt`
- clase 'ProfessionalResponse' con 5 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/catalog/data/KtorCatalogApi.kt`
- clase 'CatalogCategory' con 18 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/catalog/data/CatalogApi.kt`
- clase 'Professional' con 18 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/catalog/data/CatalogApi.kt`
- clase 'CatalogState' con 13 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/catalog/data/CatalogApi.kt`
- clase 'ProfessionalPage' con 13 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/catalog/data/CatalogApi.kt`
- clase 'ProfessionalQuery' con 7 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/catalog/data/CatalogApi.kt`
- clase 'RegistrationRequest' con 6 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/registration/data/KtorRegistrationApi.kt`
- clase 'NewAccount' con 6 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/registration/data/RegistrationApi.kt`
- clase 'MainScreen' con 25 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/navigation/MainScreen.kt`
- clase 'SearchUiState' con 22 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/search/SearchUiState.kt`
- función 'isoDate' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/components/DatePickerModal.kt`
- función 'displayDate' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/components/DatePickerModal.kt`
- clase 'SessionUiState' con 8 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/session/SessionUiState.kt`
- clase 'ReservationBannerState' con 17 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationUiState.kt`
- clase 'ReservationUiState' con 14 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationUiState.kt`
- función 'start' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- función 'retryStart' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- función 'verify' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- función 'changePhone' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- función 'submitPhone' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- función 'verifyPhone' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- función 'refresh' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- función 'present' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- función 'discover' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- función 'retryDiscovery' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- función 'dismiss' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- función 'reset' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- función 'submit' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- función 'onStartFailure' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- función 'load' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- función 'onPhoneFailure' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- función 'loadPending' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- clase 'MyReservationsUiState' con 15 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/MyReservationsUiState.kt`
- función 'reservationStateLabel' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/MyReservationsPresentation.kt`
- función 'reservationProfessional' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/MyReservationsPresentation.kt`
- función 'reservationSchedule' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/MyReservationsPresentation.kt`
- función 'reservationCancellationNotice' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/MyReservationsPresentation.kt`
- función 'processReason' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt`
- función 'phoneNotice' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt`
- función 'displayExpiration' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt`
- función 'expirationNotice' fuera de una clase — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt`
- clase 'AvailabilityUiState' con 13 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/availability/AvailabilityUiState.kt`
- clase 'RegistrationUiState' con 13 campos públicos (encapsulación) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/registration/RegistrationUiState.kt`

**DIP/concretos** (66)
- clase 'MainActivity' muy acoplada (CBO=10) — `androidApp/src/main/kotlin/com/prog2/frontend/MainActivity.kt`
- clase 'MainActivity' instancia 5 concretos (CBO=10) — `androidApp/src/main/kotlin/com/prog2/frontend/MainActivity.kt`
- clase 'AndroidEncryptedSessionStore' muy acoplada (CBO=35) — `shared/src/androidMain/kotlin/com/prog2/frontend/session/data/AndroidEncryptedSessionStore.kt`
- clase 'AndroidEncryptedSessionStore' instancia 12 concretos (CBO=35) — `shared/src/androidMain/kotlin/com/prog2/frontend/session/data/AndroidEncryptedSessionStore.kt`
- clase 'AppContainer' muy acoplada (CBO=20) — `shared/src/androidMain/kotlin/com/prog2/frontend/config/AppContainer.kt`
- clase 'AppContainer' instancia 14 concretos (CBO=20) — `shared/src/androidMain/kotlin/com/prog2/frontend/config/AppContainer.kt`
- clase 'App' muy acoplada (CBO=38) — `shared/src/commonMain/kotlin/com/prog2/frontend/App.kt`
- clase 'App' instancia 19 concretos (CBO=38) — `shared/src/commonMain/kotlin/com/prog2/frontend/App.kt`
- clase 'SessionError' muy acoplada (CBO=11) — `shared/src/commonMain/kotlin/com/prog2/frontend/session/SessionError.kt`
- clase 'SessionError' instancia 8 concretos (CBO=11) — `shared/src/commonMain/kotlin/com/prog2/frontend/session/SessionError.kt`
- clase 'SessionManager' muy acoplada (CBO=31) — `shared/src/commonMain/kotlin/com/prog2/frontend/session/SessionManager.kt`
- clase 'SessionManager' instancia 18 concretos (CBO=31) — `shared/src/commonMain/kotlin/com/prog2/frontend/session/SessionManager.kt`
- clase 'JwtExpiry' muy acoplada (CBO=10) — `shared/src/commonMain/kotlin/com/prog2/frontend/session/JwtExpiry.kt`
- clase 'KtorAuthApi' muy acoplada (CBO=17) — `shared/src/commonMain/kotlin/com/prog2/frontend/session/data/KtorAuthApi.kt`
- clase 'KtorAuthApi' instancia 6 concretos (CBO=17) — `shared/src/commonMain/kotlin/com/prog2/frontend/session/data/KtorAuthApi.kt`
- clase 'KtorReservationsApi' muy acoplada (CBO=52) — `shared/src/commonMain/kotlin/com/prog2/frontend/reservation/data/KtorReservationsApi.kt`
- clase 'KtorReservationsApi' instancia 7 concretos (CBO=52) — `shared/src/commonMain/kotlin/com/prog2/frontend/reservation/data/KtorReservationsApi.kt`
- clase 'ReservationProcess' muy acoplada (CBO=9) — `shared/src/commonMain/kotlin/com/prog2/frontend/reservation/data/ReservationsApi.kt`
- clase 'CancellationProgress' muy acoplada (CBO=11) — `shared/src/commonMain/kotlin/com/prog2/frontend/reservation/data/ReservationsApi.kt`
- clase 'OwnReservationsPage' muy acoplada (CBO=10) — `shared/src/commonMain/kotlin/com/prog2/frontend/reservation/data/ReservationsApi.kt`
- clase 'ReservationsApi' muy acoplada (CBO=9) — `shared/src/commonMain/kotlin/com/prog2/frontend/reservation/data/ReservationsApi.kt`
- clase 'KtorAvailabilityApi' muy acoplada (CBO=16) — `shared/src/commonMain/kotlin/com/prog2/frontend/availability/data/KtorAvailabilityApi.kt`
- clase 'KtorAvailabilityApi' instancia 6 concretos (CBO=16) — `shared/src/commonMain/kotlin/com/prog2/frontend/availability/data/KtorAvailabilityApi.kt`
- clase 'KtorCatalogApi' muy acoplada (CBO=17) — `shared/src/commonMain/kotlin/com/prog2/frontend/catalog/data/KtorCatalogApi.kt`
- clase 'KtorCatalogApi' instancia 5 concretos (CBO=17) — `shared/src/commonMain/kotlin/com/prog2/frontend/catalog/data/KtorCatalogApi.kt`
- clase 'KtorRegistrationApi' muy acoplada (CBO=19) — `shared/src/commonMain/kotlin/com/prog2/frontend/registration/data/KtorRegistrationApi.kt`
- clase 'KtorRegistrationApi' instancia 6 concretos (CBO=19) — `shared/src/commonMain/kotlin/com/prog2/frontend/registration/data/KtorRegistrationApi.kt`
- clase 'AppDestination' muy acoplada (CBO=9) — `shared/src/commonMain/kotlin/com/prog2/frontend/navigation/AppDestination.kt`
- clase 'AppDestination' instancia 3 concretos (CBO=9) — `shared/src/commonMain/kotlin/com/prog2/frontend/navigation/AppDestination.kt`
- clase 'MainScreen' muy acoplada (CBO=43) — `shared/src/commonMain/kotlin/com/prog2/frontend/navigation/MainScreen.kt`
- clase 'MainScreen' instancia 21 concretos (CBO=43) — `shared/src/commonMain/kotlin/com/prog2/frontend/navigation/MainScreen.kt`
- clase 'SearchViewModel' muy acoplada (CBO=28) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/search/SearchViewModel.kt`
- clase 'SearchViewModel' instancia 6 concretos (CBO=28) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/search/SearchViewModel.kt`
- clase 'SearchScreen' muy acoplada (CBO=51) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/search/SearchScreen.kt`
- clase 'SearchScreen' instancia 56 concretos (CBO=51) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/search/SearchScreen.kt`
- clase 'AppBottomBar' muy acoplada (CBO=8) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/components/AppBottomBar.kt`
- clase 'AppBottomBar' instancia 5 concretos (CBO=8) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/components/AppBottomBar.kt`
- clase 'SessionViewModel' muy acoplada (CBO=19) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/session/SessionViewModel.kt`
- clase 'SessionViewModel' instancia 4 concretos (CBO=19) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/session/SessionViewModel.kt`
- clase 'LoginScreen' muy acoplada (CBO=30) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/session/LoginScreen.kt`
- clase 'LoginScreen' instancia 24 concretos (CBO=30) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/session/LoginScreen.kt`
- clase 'ProcessScreen' muy acoplada (CBO=51) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessScreen.kt`
- clase 'ProcessScreen' instancia 69 concretos (CBO=51) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessScreen.kt`
- clase 'StartStatus' muy acoplada (CBO=9) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationUiState.kt`
- clase 'ReservationBannerState' muy acoplada (CBO=16) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationUiState.kt`
- clase 'ReservationBannerState' instancia 6 concretos (CBO=16) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationUiState.kt`
- clase 'ReservationUiState' muy acoplada (CBO=13) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationUiState.kt`
- clase 'ReservationUiState' instancia 5 concretos (CBO=13) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationUiState.kt`
- clase 'MyReservationsScreen' muy acoplada (CBO=37) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/MyReservationsScreen.kt`
- clase 'MyReservationsScreen' instancia 56 concretos (CBO=37) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/MyReservationsScreen.kt`
- clase 'MyReservationsViewModel' muy acoplada (CBO=24) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/MyReservationsViewModel.kt`
- clase 'MyReservationsViewModel' instancia 4 concretos (CBO=24) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/MyReservationsViewModel.kt`
- clase 'ProcessView' muy acoplada (CBO=27) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt`
- clase 'ProcessView' instancia 11 concretos (CBO=27) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt`
- clase 'AvailabilityScreen' muy acoplada (CBO=36) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/availability/AvailabilityScreen.kt`
- clase 'AvailabilityScreen' instancia 34 concretos (CBO=36) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/availability/AvailabilityScreen.kt`
- clase 'AvailabilityViewModel' muy acoplada (CBO=23) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/availability/AvailabilityViewModel.kt`
- clase 'AvailabilityViewModel' instancia 6 concretos (CBO=23) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/availability/AvailabilityViewModel.kt`
- clase 'RegistrationScreen' muy acoplada (CBO=29) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/registration/RegistrationScreen.kt`
- clase 'RegistrationScreen' instancia 31 concretos (CBO=29) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/registration/RegistrationScreen.kt`
- clase 'RegistrationViewModel' muy acoplada (CBO=30) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/registration/RegistrationViewModel.kt`
- clase 'RegistrationViewModel' instancia 8 concretos (CBO=30) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/registration/RegistrationViewModel.kt`
- clase 'ApiConfiguration' muy acoplada (CBO=9) — `shared/src/commonMain/kotlin/com/prog2/frontend/network/ApiConfiguration.kt`
- clase 'ApiConfiguration' instancia 3 concretos (CBO=9) — `shared/src/commonMain/kotlin/com/prog2/frontend/network/ApiConfiguration.kt`
- clase 'AuthenticatedApiClient' muy acoplada (CBO=18) — `shared/src/commonMain/kotlin/com/prog2/frontend/network/AuthenticatedApiClient.kt`
- clase 'AuthenticatedApiClient' instancia 7 concretos (CBO=18) — `shared/src/commonMain/kotlin/com/prog2/frontend/network/AuthenticatedApiClient.kt`

**SRP/responsabilidad** (55)
- método 'AndroidEncryptedSessionStore.read' con complejidad 6 — `shared/src/androidMain/kotlin/com/prog2/frontend/session/data/AndroidEncryptedSessionStore.kt`
- clase 'AppContainer' grande (LOC=27, métodos=0, campos=8) — `shared/src/androidMain/kotlin/com/prog2/frontend/config/AppContainer.kt`
- método 'App.Content' de 71 líneas — `shared/src/commonMain/kotlin/com/prog2/frontend/App.kt`
- clase 'SessionManager' grande (LOC=141, métodos=9, campos=12) — `shared/src/commonMain/kotlin/com/prog2/frontend/session/SessionManager.kt`
- método 'SessionManager.restore' de 31 líneas — `shared/src/commonMain/kotlin/com/prog2/frontend/session/SessionManager.kt`
- método 'SessionManager.restore' con complejidad 7 — `shared/src/commonMain/kotlin/com/prog2/frontend/session/SessionManager.kt`
- método 'SessionManager.signIn' con complejidad 6 — `shared/src/commonMain/kotlin/com/prog2/frontend/session/SessionManager.kt`
- método 'KtorAuthApi.authenticate' con complejidad 7 — `shared/src/commonMain/kotlin/com/prog2/frontend/session/data/KtorAuthApi.kt`
- clase 'KtorReservationsApi' grande (LOC=210, métodos=14, campos=1) — `shared/src/commonMain/kotlin/com/prog2/frontend/reservation/data/KtorReservationsApi.kt`
- clase 'StartBody' grande (LOC=38, métodos=0, campos=13) — `shared/src/commonMain/kotlin/com/prog2/frontend/reservation/data/KtorReservationsApi.kt`
- clase 'PhoneBody' grande (LOC=31, métodos=0, campos=12) — `shared/src/commonMain/kotlin/com/prog2/frontend/reservation/data/KtorReservationsApi.kt`
- clase 'ProcessResponse' grande (LOC=29, métodos=0, campos=12) — `shared/src/commonMain/kotlin/com/prog2/frontend/reservation/data/KtorReservationsApi.kt`
- clase 'ReservationProcess' grande (LOC=36, métodos=0, campos=23) — `shared/src/commonMain/kotlin/com/prog2/frontend/reservation/data/ReservationsApi.kt`
- método 'KtorCatalogApi.searchProfessionals' con complejidad 8 — `shared/src/commonMain/kotlin/com/prog2/frontend/catalog/data/KtorCatalogApi.kt`
- clase 'CatalogCategory' grande (LOC=36, métodos=0, campos=15) — `shared/src/commonMain/kotlin/com/prog2/frontend/catalog/data/CatalogApi.kt`
- clase 'Professional' grande (LOC=34, métodos=0, campos=15) — `shared/src/commonMain/kotlin/com/prog2/frontend/catalog/data/CatalogApi.kt`
- clase 'CatalogState' grande (LOC=23, métodos=0, campos=11) — `shared/src/commonMain/kotlin/com/prog2/frontend/catalog/data/CatalogApi.kt`
- clase 'ProfessionalPage' grande (LOC=21, métodos=0, campos=11) — `shared/src/commonMain/kotlin/com/prog2/frontend/catalog/data/CatalogApi.kt`
- método 'KtorRegistrationApi.failureFor' con complejidad 8 — `shared/src/commonMain/kotlin/com/prog2/frontend/registration/data/KtorRegistrationApi.kt`
- clase 'AppDestination' sin métodos (3 campos): solo datos — `shared/src/commonMain/kotlin/com/prog2/frontend/navigation/AppDestination.kt`
- clase 'MainScreen' grande (LOC=248, métodos=1, campos=23) — `shared/src/commonMain/kotlin/com/prog2/frontend/navigation/MainScreen.kt`
- clase 'SearchViewModel' grande (LOC=143, métodos=17, campos=7) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/search/SearchViewModel.kt`
- clase 'SearchViewModel' con baja cohesión (18%) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/search/SearchViewModel.kt`
- método 'SearchViewModel.search' con complejidad 6 — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/search/SearchViewModel.kt`
- método 'SearchViewModel.perform' de 46 líneas — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/search/SearchViewModel.kt`
- clase 'SearchScreen' grande (LOC=208, métodos=4, campos=3) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/search/SearchScreen.kt`
- clase 'SearchUiState' grande (LOC=27, métodos=0, campos=22) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/search/SearchUiState.kt`
- clase 'SessionUiState' grande (LOC=12, métodos=0, campos=8) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/session/SessionUiState.kt`
- clase 'SessionViewModel' con baja cohesión (7%) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/session/SessionViewModel.kt`
- método 'SessionViewModel.signIn' con complejidad 8 — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/session/SessionViewModel.kt`
- clase 'ProcessScreen' grande (LOC=219, métodos=6, campos=3) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessScreen.kt`
- clase 'ProcessScreen' con baja cohesión (0%) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessScreen.kt`
- clase 'ReservationBannerState' grande (LOC=63, métodos=1, campos=16) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationUiState.kt`
- clase 'ReservationUiState' grande (LOC=54, métodos=1, campos=14) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationUiState.kt`
- función 'submitPhone' de 30 líneas — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- función 'submitPhone' con complejidad 8 — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- función 'onStartFailure' de 35 líneas — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- función 'onStartFailure' con complejidad 6 — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- función 'load' con complejidad 6 — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- función 'loadPending' con complejidad 6 — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationViewModel.kt`
- clase 'MyReservationsUiState' grande (LOC=28, métodos=0, campos=15) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/MyReservationsUiState.kt`
- clase 'MyReservationsViewModel' grande (LOC=143, métodos=11, campos=5) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/MyReservationsViewModel.kt`
- método 'MyReservationsViewModel.confirmCancellation' de 36 líneas — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/MyReservationsViewModel.kt`
- método 'MyReservationsViewModel.confirmCancellation' con complejidad 8 — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/MyReservationsViewModel.kt`
- método 'MyReservationsViewModel.read' de 31 líneas — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/MyReservationsViewModel.kt`
- método 'ProcessView.processView' de 42 líneas — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt`
- clase 'AvailabilityUiState' grande (LOC=16, métodos=0, campos=13) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/availability/AvailabilityUiState.kt`
- clase 'AvailabilityViewModel' con baja cohesión (33%) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/availability/AvailabilityViewModel.kt`
- clase 'RegistrationViewModel' grande (LOC=150, métodos=14, campos=6) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/registration/RegistrationViewModel.kt`
- clase 'RegistrationViewModel' con baja cohesión (4%) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/registration/RegistrationViewModel.kt`
- método 'RegistrationViewModel.submit' de 49 líneas — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/registration/RegistrationViewModel.kt`
- método 'RegistrationViewModel.submit' con complejidad 7 — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/registration/RegistrationViewModel.kt`
- método 'RegistrationViewModel.showFailure' con complejidad 8 — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/registration/RegistrationViewModel.kt`
- clase 'RegistrationUiState' grande (LOC=17, métodos=0, campos=13) — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/registration/RegistrationUiState.kt`
- método 'AuthenticatedApiClient.post' con complejidad 6 — `shared/src/commonMain/kotlin/com/prog2/frontend/network/AuthenticatedApiClient.kt`

**LSP/overrides** (5)
- 'App.Content' vacío o 'no implementado' — `shared/src/commonMain/kotlin/com/prog2/frontend/App.kt`
- 'KtorReservationsApi.startFailure' vacío o 'no implementado' — `shared/src/commonMain/kotlin/com/prog2/frontend/reservation/data/KtorReservationsApi.kt`
- 'ReservationBannerState.toString' vacío o 'no implementado' — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationUiState.kt`
- 'ReservationUiState.toString' vacío o 'no implementado' — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ReservationUiState.kt`
- 'MyReservationsScreen.PendingProcess' vacío o 'no implementado' — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/MyReservationsScreen.kt`

**?** (3)
- clase 'SessionException' con herencia profunda (DIT=2) — `shared/src/commonMain/kotlin/com/prog2/frontend/session/SessionError.kt`
- clase 'AvailabilityException' con herencia profunda (DIT=2) — `shared/src/commonMain/kotlin/com/prog2/frontend/availability/AvailabilityError.kt`
- clase 'CatalogException' con herencia profunda (DIT=2) — `shared/src/commonMain/kotlin/com/prog2/frontend/catalog/CatalogError.kt`

**duplicación** (6)
- duplicación lógica: 8.1% · boilerplate: 17.8%
- bloque de 8 líneas repetido 5 veces en shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt:19, shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt:23, shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt:35 — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt`
- bloque de 8 líneas repetido 5 veces en shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt:17, shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt:21, shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt:33 — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt`
- bloque de 8 líneas repetido 5 veces en shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt:18, shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt:22, shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt:34 — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt`
- bloque de 8 líneas repetido 5 veces en shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt:20, shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt:24, shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt:36 — `shared/src/commonMain/kotlin/com/prog2/frontend/ui/reservation/ProcessPresentation.kt`
- bloque de 8 líneas repetido 4 veces en shared/src/commonTest/kotlin/com/prog2/frontend/ui/reservation/MyReservationsViewModelTest.kt:305, shared/src/commonTest/kotlin/com/prog2/frontend/ui/reservation/MyReservationsViewModelTest.kt:346, shared/src/commonTest/kotlin/com/prog2/frontend/ui/reservation/MyReservationsViewModelTest.kt:382 — `shared/src/commonTest/kotlin/com/prog2/frontend/ui/reservation/MyReservationsViewModelTest.kt`

**MVVM/KMP** (1)
- 1 archivo(s) de UI con red/BD dentro

## 6. Evolución por commit

Métricas calculadas sobre el estado de `main` en **cada commit** (9 commits con trabajo), del primero al último. La calidad usa el perfil `exigente`; el aporte de código lo mide RepoSense. El índice puede **bajar** al crecer el proyecto: se acumulan señales.

```mermaid
xychart-beta
    title "Índice de calidad a lo largo de los commits"
    x-axis ["0b4f0ba", "ceff7fd", "0cf918f", "41e6876", "e6debd7", "d4fe833", "8b8b58e", "0f6864f", "cc4b102"]
    y-axis "Índice" 0 --> 100
    line [0, 15, 15, 32, 39, 42, 44, 41, 41]
```

| # | Fecha | Commit | Autor | LOC | Índice | %POO | Duplic. | Sueltas |
|---|---|---|---|---:|---:|---:|---:|---:|
| 1 | 2026-09-07 | `0b4f0ba` | Valentino Perassi Ferrara | 0 | 0 | 0% | 0.0% | 0 |
| 2 | 2026-09-10 | `ceff7fd` | Ing. Daniel Quinteros | 0 | 15 | 0% | 0.0% | 0 |
| 3 | 2026-10-09 | `0cf918f` | Valentino Perassi Ferrara | 0 | 15 | 0% | 0.0% | 0 |
| 4 | 2026-10-31 | `41e6876` | Valentino Perassi Ferrara | 223 | 32 | 27% | 11.1% | 0 |
| 5 | 2026-11-08 | `e6debd7` | Valentino Perassi Ferrara | 2,634 | 39 | 66% | 4.3% | 1 |
| 6 | 2026-11-09 | `d4fe833` | Valentino Perassi Ferrara | 4,542 | 42 | 75% | 5.3% | 1 |
| 7 | 2026-11-09 | `8b8b58e` | Valentino Perassi Ferrara | 7,041 | 44 | 75% | 6.4% | 19 |
| 8 | 2026-11-09 | `0f6864f` | Valentino Perassi Ferrara | 8,944 | 41 | 76% | 8.4% | 32 |
| 9 | 2026-11-09 | `cc4b102` | Valentino Perassi Ferrara | 9,035 | 41 | 75% | 8.1% | 32 |

---
_Informe generado por la cátedra. Las dimensiones y umbrales corresponden a `calidad.py` (perfil `exigente`); el semáforo, a `CRITERIOS-CALIDAD.md`. La documentación del repositorio no se evalúa. Ante dudas, revisá el archivo señalado._
<!-- evaluacion:fin -->
