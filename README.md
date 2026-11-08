<!-- calidad:inicio -->
![Calidad](https://img.shields.io/badge/Calidad-sin%20c%C3%B3digo-lightgrey) ![Aprobar](https://img.shields.io/badge/Aprobar-no-red) ![Destacar](https://img.shields.io/badge/Destacar-no-lightgrey)

**Calidad (7 dimensiones):** sin código en `main` (no evaluable).
<!-- calidad:fin -->

# Aplicación KMP para Android

Cliente de Catálogo y Turnos para registro, búsqueda de profesionales y gestión de reservas propias. Android debe permitir completar el flujo funcional requerido por cátedra.

**Estado:** registro, inicio, conservación y cierre de sesión implementados con MVVM. Búsqueda de profesionales implementada sobre Catálogo con filtros, paginación y aviso de copia; el área autenticada muestra esa pantalla. Los procesos de reserva se integran en sus respectivos CU. Ver resultados, límites y verificaciones en los planes de [registro](docs/features/registro-usuario/PLAN.md), [sesión](docs/features/sesion-usuario/PLAN.md) y [búsqueda](docs/features/busqueda-profesionales/PLAN.md).

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

El archivo `gradle/gradle-daemon-jvm.properties` exige Java 21 sin fijar proveedor. Para iniciar sesión o registrarte hace falta Turnos y datos de una cuenta de usuario final. No usar credenciales técnicas de cátedra como cuenta de usuario.

Debug usa los siguientes destinos desde el emulador Android:

| Servicio | Origen |
| --- | --- |
| Turnos | `http://10.0.2.2:8081` |
| Catálogo | `http://10.0.2.2:8080` |

Las propiedades Gradle `TURNOS_BASE_URL` y `CATALOG_BASE_URL` permiten configurar otros orígenes HTTPS, mediante `-P` o el `gradle.properties` local del usuario. Por ejemplo, `-PTURNOS_BASE_URL=https://turnos.example.test`. Configurar los dos servicios según el entorno; no incluir rutas, query, fragmento ni credenciales en las URLs. En release ambos orígenes HTTPS son obligatorios, sin defaults de producción.

HTTP está permitido únicamente en debug para `10.0.2.2`; no se deshabilita la validación TLS. Un dispositivo físico requiere conectividad hacia orígenes HTTPS configurados. La app no necesita CORS para su cliente Android nativo.

En Android 17/API 37, el acceso a esos destinos locales exige un permiso adicional,
declarado solo en debug. Después de instalar, habilitarlo para el emulador:

```sh
adb -s <serial> shell pm grant com.prog2.frontend android.permission.ACCESS_LOCAL_NETWORK
```

Si se deniega o revoca, el login informa indisponibilidad y el registro un resultado incierto; ambos permiten reintentar y no conservan una sesión por omisión. Release no declara este permiso y usa los orígenes HTTPS configurados. Ver [permiso de red local de Android](https://developer.android.com/privacy-and-security/local-network-permission).

## Compilación y revisión

Desde la raíz del repositorio:

```sh
./gradlew check :androidApp:assembleDebug
```

`check` ejecuta las pruebas host de sesión y registro, y las verificaciones conectadas por los plugins, incluido lint. `assembleDebug` genera `androidApp/build/outputs/apk/debug/androidApp-debug.apk`. Las pruebas host usan reloj controlado y MockEngine; no requieren un backend, emulador ni credenciales reales.

La compilación y lint fueron comprobados en Linux; quedan avisos de versiones y recursos del launcher, sin errores. Windows y `gradlew.bat` no se comprobaron.

Con un dispositivo o emulador conectado, ejecutar además las pruebas de Keystore/archivo reales, que no están incluidas en `check`:

```sh
./gradlew :shared:connectedAndroidDeviceTest
```

## Ejecución Android

Seleccionar `androidApp` y un emulador/dispositivo en Android Studio y pulsar **Run**. Ingresar usuario o correo y contraseña de una cuenta activa de Turnos. «Recordarme» solicita 30 días en lugar de 24 horas; la app respeta el vencimiento recibido en ambos casos.

Para instalar y abrir desde la terminal, con un dispositivo conectado:

```sh
./gradlew :androidApp:installDebug
adb -s <serial> shell am start -n com.prog2.frontend/.MainActivity
```

Si `adb` no está en el PATH, usar el ejecutable de `platform-tools` del SDK. Las verificaciones de cada revisión están en el plan de la funcionalidad; instalar o compilar no sustituye comprobar el comportamiento.

`LoginScreen.kt` incluye `LoginScreenPreview`, estática y sin ViewModel/red/almacenamiento. Su código compila; el renderizado en el IDE no forma parte de la evidencia de ejecución.

La contraseña/formulario se mantienen solo en el ViewModel; no se restauran tras terminar el proceso Android. Se conserva únicamente el JWT cifrado con Android Keystore en un archivo privado excluido del backup. Logout y vencimiento/invalidez detectados retiran la sesión y el estado temporal, sin cancelar procesos o reservas. No hay renovación automática, consultas de negocio offline ni cola de solicitudes.

## Estructura y documentación

- `androidApp`: entrada/Application Android, manifest, URLs y recursos por variante. Depende de `shared`.
- `shared`: UI Compose, ViewModel, coordinador de sesión y cliente HTTP comunes; almacenamiento/ciclo de vida en `androidMain`. Solo target Android.
- [Plan técnico de sesión](docs/features/sesion-usuario/PLAN.md) y [plan técnico de registro](docs/features/registro-usuario/PLAN.md).
- [SPEC](docs/features/setup-kmp/SPEC.md), [PLAN y diagrama](docs/features/setup-kmp/PLAN.md) y [TASKS](docs/features/setup-kmp/TASKS.md) del setup.
- [Casos de uso y diagrama](docs/use-cases/README.md) e [issues publicados](https://github.com/prog2-perassiferrara/frontend-kmp/issues).
- [Instrucciones](AGENTS.md), [reglas de trabajo](docs/GENERIC_RULES.md) y [guía mobile](docs/MOBILE_GUIDELINES.md).
- [Plantilla SPEC](docs/SPEC_TEMPLATE.md), [plantilla PLAN](docs/PLAN_TEMPLATE.md) y [prompts del workflow](docs/PROMPTS.md).

El flujo es CU → issue → SPEC breve → PLAN breve → TASKS breve → implementación y verificación. Turnos emite el JWT de usuario; el cliente protegido lo envía directamente a ambos backends. La app no recibe JWT técnico ni accede a bases de datos, Redis o Kafka.
