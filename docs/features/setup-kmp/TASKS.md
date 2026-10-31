# TASKS: Base KMP ejecutable en Android

**Referencias:** [SPEC aprobada](SPEC.md), [PLAN aprobado](PLAN.md) e [issue #3](https://github.com/prog2-perassiferrara/frontend-kmp/issues/3).
**Estado:** Completado. Implementación autorizada por el usuario y validada el 2026-10-31.

**Chequeo previo (2026-10-31):** coherencia documental RF → CA → PLAN → tareas comprobada; todos los requisitos y criterios cubiertos, sin alcance nuevo ni decisiones bloqueantes. Resultados reales en el cierre.

- [x] **T-01 · Incorporar la base y configurar el build.**
  - **Objetivo y alcance:** incorporar selectivamente wrapper y módulos `androidApp`/`shared`; aplicar versiones, Material estable, JDK 21 sin proveedor forzado y exclusiones del PLAN. Conservar documentación, configuración e identidad existentes.
  - **Dependencias:** autorización de implementación.
  - **Referencias:** RF-01, RF-04, RF-05; CA-01, CA-05, CA-06.
  - **Validación:** comprobar wrapper/JDK y configuración de módulos; revisar diferencias, exclusiones y ausencia de credenciales/permisos añadidos. El APK se verifica tras T-02.

- [x] **T-02 · Conectar la demo mínima con MVVM.**
  - **Objetivo y alcance:** adaptar `MainActivity` y `App`; crear `SetupScreen` y `SetupViewModel`, con el booleano observable y el callback del PLAN. Incluir KDoc pertinente y omitir muestras del wizard sin uso.
  - **Dependencias:** T-01.
  - **Referencias:** RF-02, RF-03, RF-05; CA-02, CA-03, CA-04, CA-05, CA-07.
  - **Validación:** inspeccionar propiedad del estado, owner y recorrido acción/estado/UI; sin persistencia del booleano ni efectos de negocio. Compilación y comportamiento se comprueban en T-03.

- [x] **T-03 · Verificar build e interacción Android.**
  - **Objetivo y alcance:** ejecutar `./gradlew check :androidApp:assembleDebug`, instalar y abrir; comprobar los escenarios de la estrategia de validación del PLAN, sin ampliar la demo.
  - **Dependencias:** T-02; SDK/dependencias y dispositivo o emulador accesible.
  - **Referencias:** RF-01, RF-02, RF-03, RF-05; CA-01, CA-02, CA-03, CA-04, CA-05, CA-07.
  - **Validación:** registrar verificaciones efectivamente ejecutadas y APK; alternancia, rotación/segundo plano con proceso vivo, muerte real del proceso, ejecución sin red y adaptación de texto/orientación. Distinguir inspección, ejecución observada y comprobaciones manuales pendientes.

- [x] **T-04 · Documentar ejecución y cerrar evidencia.**
  - **Objetivo y alcance:** actualizar README con requisitos y comandos comprobados; revisar preservación de documentos/CODEOWNERS y exclusiones. Completar el cierre siguiente sin informes adicionales.
  - **Dependencias:** T-03; los escenarios pendientes mantienen el cierre incompleto.
  - **Referencias:** RF-04, RF-06; CA-06, CA-07.
  - **Validación:** reproducir las instrucciones, revisar enlaces/formato y correspondencia entre evidencia y estado de cada CA.

## Cierre

| CA | Estado | Evidencia |
| --- | --- | --- |
| CA-01 | Verificado | E-01: wrapper, revisión y APK; E-02: identidad, SDK y launcher del artefacto. |
| CA-02 | Verificado | E-02/E-04: MVVM, instalación, apertura y alternancia observadas. |
| CA-03 | Verificado | E-04: estado conservado al rotar y regresar del segundo plano, con PID 5598. |
| CA-04 | Verificado | E-04: proceso 5598 terminado; reapertura con PID 7867 y contenido oculto. |
| CA-05 | Verificado | E-02/E-04: sin integración ni persistencia; arranque e interacción sin red activa. |
| CA-06 | Verificado | E-03: documentación/CODEOWNERS preservados y exclusiones comprobadas. |
| CA-07 | Verificado | E-04: comandos del README ejecutados, ambas orientaciones y texto al 200 % inspeccionados. |

**E-01 · Build (2026-10-31):** `./gradlew --version` comprobó Gradle 9.5.1 y JDK Temurin 21.0.12.1, con daemon Java 21 sin vendor obligatorio. `./gradlew check :androidApp:assembleDebug` final: BUILD SUCCESSFUL en 4 s, 68 tareas (6 ejecutadas y 62 actualizadas). Lint sin errores, seis avisos: tres de versiones disponibles y tres de recursos generados. Sin fuentes de tests; no equivale a pruebas funcionales automatizadas. APK: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

**E-02 · Artefacto e inspección (2026-10-31):** `aapt2 dump badging` verificó `com.prog2.frontend`, `FrontendKmp`, mínimo 24, target 37 y launcher `MainActivity`. Sin permiso Internet ni permisos de dispositivo; AndroidX añade únicamente su permiso propio de firma para receptores no exportados. Cuatro archivos Kotlin, un único booleano en el ViewModel y sin restauración persistente ni efectos de negocio.

**E-03 · Preservación (2026-10-31):** comparación de hashes de archivos previos y `git diff --check` correctos. `git check-ignore` comprobó `local.properties`, `.gradle`, `.kotlin`, `.idea` y los builds de ambos módulos. No fue necesario modificar `.gitignore`.

**E-04 · Ejecución Android (2026-10-31):** `./gradlew :androidApp:installDebug` instaló en un emulador Android 17/API 37.0 (BUILD SUCCESSFUL, 2 s); `am start -W` abrió `MainActivity` sin errores. Escenarios comprobados por ADB y jerarquía accesible: estado inicial oculto, mostrar/ocultar, rotación y segundo plano sin cambio de PID, muerte del proceso mediante `run-as`/SIGKILL y reapertura con estado inicial. Capturas inspeccionadas con texto al 200 % en ambas orientaciones, sin cortes ni superposición; botón utilizable y etiquetado. Arranque desde otro proceso e interacción con WiFi/datos desactivados y `Active default network: none` comprobados. Sin excepciones fatales observadas; ajustes de la instancia de prueba restaurados.

**Regresión:** E-01/E-02/E-04 corresponden al árbol de trabajo actual del 2026-10-31. README distingue build, verificaciones sin fuentes de tests y ensayos reales en emulador. Windows, API 24, hardware físico y renderizado de la preview en el IDE no se han probado. En el entorno inicial fallaron intentos de arranque del emulador y una instalación sin dispositivo operativo; el ensayo final completó los escenarios después de disponer de una instancia arrancada.

**Desviaciones del PLAN:** ninguna de alcance o comportamiento. Se conservó el soporte host de pruebas del wizard para conectar `commonTest`, sin agregar pruebas de ejemplo ni nuevas dependencias de tests.
