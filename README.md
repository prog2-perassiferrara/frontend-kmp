<!-- calidad:inicio -->
![Calidad](https://img.shields.io/badge/Calidad-sin%20c%C3%B3digo-lightgrey) ![Aprobar](https://img.shields.io/badge/Aprobar-no-red) ![Destacar](https://img.shields.io/badge/Destacar-no-lightgrey)

**Calidad (7 dimensiones):** sin código en `main` (no evaluable).
<!-- calidad:fin -->

# Aplicación KMP para Android

Cliente de Catálogo y Turnos para registro, búsqueda de profesionales y gestión de reservas propias. Android debe permitir completar el flujo funcional requerido por cátedra.

**Estado:** setup implementado, compilación debug y ejecución verificadas en un emulador Android API 37.0. Incluye una demo descartable con MVVM para mostrar u ocultar contenido. Los CU de negocio todavía no están implementados.

## Requisitos

- JDK 21. Gradle admite proveedores compatibles; se verificó con Eclipse Temurin 21.
- Android Studio compatible con AGP 9.1.1 para ejecutar y previsualizar la aplicación.
- Android SDK API 37 y Build Tools 36.0.0.
- Emulador o dispositivo desde API 24 para ejecutar. La primera descarga de dependencias requiere Internet.

Se conserva el stack generado: Kotlin 2.4.21, Compose Multiplatform/Material 1.12.2, Lifecycle 2.11.0 y Gradle Wrapper 9.5.1. Las versiones están en `gradle/libs.versions.toml`.

## Configuración local

Abrir esta carpeta (`frontend-kmp`) en Android Studio y usar JDK 21 para Gradle. El SDK se configura mediante `ANDROID_HOME` o `sdk.dir` en `local.properties`, archivo excluido de Git.

Para la terminal, ajustar las rutas al entorno local:

```sh
export JAVA_HOME=/ruta/al/jdk-21
export ANDROID_HOME=/ruta/al/Android/Sdk
```

El archivo `gradle/gradle-daemon-jvm.properties` exige Java 21 sin fijar proveedor. No se requieren credenciales ni backends para este setup.

## Compilación y revisión

Desde la raíz del repositorio:

```sh
./gradlew check :androidApp:assembleDebug
```

`check` ejecuta las verificaciones conectadas por los plugins, incluido lint. En este setup no hay fuentes de tests; un build correcto no acredita pruebas funcionales automatizadas. `assembleDebug` genera `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

La compilación y lint fueron comprobados en Linux; lint conserva seis avisos de versiones disponibles y recursos del wizard, sin errores. Windows y `gradlew.bat` no se comprobaron.

## Ejecución Android

Seleccionar `androidApp` y un emulador/dispositivo en Android Studio y pulsar **Run**. El título es `FrontendKmp`; el botón muestra u oculta el texto demostrativo. La demo conserva su estado al rotar o regresar del segundo plano con el proceso vivo; un proceso nuevo comienza con el contenido oculto.

Para instalar y abrir desde la terminal, con un dispositivo conectado:

```sh
./gradlew :androidApp:installDebug
adb -s <serial> shell am start -n com.prog2.frontend/.MainActivity
```

Ambos comandos se comprobaron en el emulador. Si `adb` no está en el PATH, usar el ejecutable de `platform-tools` del SDK.

`SetupScreen.kt` incluye `SetupScreenPreview`, una preview estática sin ViewModel. Su código compila; el renderizado en el IDE no forma parte de la evidencia de ejecución. Los escenarios y resultados están en [TASKS](docs/features/setup-kmp/TASKS.md).

## Estructura y documentación

- `androidApp`: entrada Android, manifest y recursos. Depende de `shared`.
- `shared`: UI Compose y ViewModel comunes; solo target Android.
- [SPEC](docs/features/setup-kmp/SPEC.md), [PLAN y diagrama](docs/features/setup-kmp/PLAN.md) y [TASKS](docs/features/setup-kmp/TASKS.md) del setup.
- [Casos de uso y diagrama](docs/use-cases/README.md) e [issues publicados](https://github.com/prog2-perassiferrara/frontend-kmp/issues).
- [Instrucciones](AGENTS.md), [reglas de trabajo](docs/GENERIC_RULES.md) y [guía mobile](docs/MOBILE_GUIDELINES.md).
- [Plantilla SPEC](docs/SPEC_TEMPLATE.md), [plantilla PLAN](docs/PLAN_TEMPLATE.md) y [prompts del workflow](docs/PROMPTS.md).

El flujo es CU → issue → SPEC → PLAN → TASKS → implementación autorizada. El setup no consume APIs ni agrega sesión, persistencia, navegación de negocio o soporte offline a los CU.
