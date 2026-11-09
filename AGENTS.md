# Guía para agentes: Aplicación KMP

## Lectura y alcance

- Las rutas son relativas a este repositorio. Leer las [instrucciones generales](../AGENTS.md), [las reglas de trabajo](docs/GENERIC_RULES.md) y las secciones pertinentes del [enunciado](../PROJECT_STATEMENT-v1.md) y del [contrato de integración](../INTEGRATION_REFERENCE-v2.md).
- Si el repositorio se clona por separado y faltan esos documentos, informar qué falta; no inventar su contenido.
- Responder en español. Priorizar el aprendizaje y explicar decisiones sin implementar soluciones no solicitadas.
- Mantener aquí reglas estables. El estado vigente se consulta en `../SESSION_HANDOFF.md`, la documentación de la funcionalidad y Git; verificarlo antes de trabajar.
- Revisar `git status --short` antes de editar y conservar cambios del usuario. Una tarea local no autoriza a modificar los otros repositorios.
- El usuario maneja Git: no crear ramas, preparar staging, commitear, hacer fetch/pull/push, merge/rebase ni eliminar referencias sin pedido explícito.

## Flujo de trabajo

- Partir del CU y del issue correspondiente. Mantener en `docs/features/<funcionalidad>/` los tres documentos obligatorios y breves: `SPEC.md` (comportamiento, alcance y RF/CA), `PLAN.md` (decisiones técnicas, archivos y validación) y `TASKS.md` (pasos, dependencias y evidencia). Flujo: CU/issue → SPEC → PLAN → TASKS → implementación y verificación.
- La autorización para desarrollar el CU habilita todas las etapas; no solicitar aprobaciones rutinarias intermedias. Consultar solo decisiones que cambien comportamiento, contratos, alcance o impliquen cambios destructivos. Implementar solo ante pedido explícito y no repetir autorizaciones ya dadas.
- Mantener el alcance mínimo: no agregar pantallas vacías, navegación futura, capas, dependencias ni abstracciones especulativas. El código necesario para el CU pedido se implementa completo.
- Registrar en `TASKS.md` la validación realmente ejecutada y sus límites. No marcar como verificado lo que no se ejecutó ni reutilizar evidencia de otra implementación.
- Si cambia un contrato o un comportamiento acordado, dejar una nota breve en el documento afectado y consultarlo antes de continuar. Evitar matrices de trazabilidad y repetir la SPEC o las tareas en varios documentos.

## Stack y arquitectura

- Proyecto Kotlin Multiplatform con una aplicación Android ejecutable en `androidApp/` y el código compartido en `shared/`.
- Compose Material 1, MVVM ligero, Ktor y kotlinx. Mantener las versiones del catálogo salvo necesidad demostrada.
- Pantallas como clases con `@Composable fun Content(...)`. El estado de presentación vive en un `ViewModel`; las dependencias se reciben por constructor y se cablean en `AppContainer`/`FrontendApplication`.
- No trasladar la arquitectura hexagonal de los backends. No agregar frameworks de DI ni módulos nuevos.
- El área autenticada navega con Navigation Compose según el enfoque de cátedra: `MainScreen` concentra `Scaffold`, `NavHost` y el único `NavController`; `AppDestination` define las pestañas y `AppBottomBar` es presentacional. Pestañas: Buscar y Cuenta (reservas propias y cierre de sesión). Login/registro quedan fuera; disponibilidad y proceso son destinos secundarios. No agregar un segundo mecanismo de navegación.
- Código en inglés; KDoc y comentarios en español para responsabilidades, contratos y decisiones no evidentes.

## Sesión y APIs

- Conservar el JWT únicamente en `AndroidEncryptedSessionStore` (AES-GCM, clave en Android Keystore, `noBackupFilesDir`). No guardar contraseñas ni usar almacenamiento sin cifrar.
- `SessionManager` es la única fuente de sesión. El `epoch` distingue aperturas: una respuesta o un 401 tardío no publica datos ni cierra una sesión posterior. El logout es local, espera una escritura aceptada y elimina el token; si la limpieza falla, bloquea un login nuevo hasta reintentarla.
- Controlar vencimiento al restaurar, volver del segundo plano y antes de una petición protegida. Un 401 de una API protegida cierra la sesión; 403, timeout o error de transporte no. No hay refresh ni temporizador de expulsión.
- `JwtExpiry` solo lee `exp`; la firma y los permisos los validan los backends. El JWT, la contraseña y los cuerpos privados nunca aparecen en logs, mensajes ni `toString`.
- KMP consulta directamente Catálogo para búsquedas y Turnos para cuentas, disponibilidad, procesos y reservas, usando el JWT de usuario. No acceder a bases, Redis, Kafka ni credenciales técnicas de cátedra.

## Responsabilidad de la aplicación

- Cubrir registro, login, búsqueda y filtros, disponibilidad, reserva con información adicional, consulta y cancelación de reservas propias.
- Reflejar el resultado de los backends: una confirmación inicial o una acción local no equivalen por sí solas a una reserva confirmada. No inventar endpoints ni estados para completar una pantalla.
- Mantener un solo proceso pendiente por usuario cuando Turnos lo verifique. Salir de una pantalla, cerrar la app o cerrar sesión no cancela ni abandona operaciones.
- CU-11 no está en el alcance activo: Turnos no expone abandono de proceso. La cancelación de una reserva confirmada pertenece a CU-10 y conserva su diálogo de confirmación.
- Sin consultas offline ni cola de operaciones. Conservar formularios, filtros y selecciones mientras viva el proceso Android; descartarlos al terminarlo, cerrar sesión, detectar vencimiento/invalidez o cambiar de cuenta. La sesión válida sí se conserva al reabrir.

## Verificación

- Comandos reales: `./gradlew :shared:testAndroidHostTest` y `./gradlew :androidApp:assembleDebug`. Con dispositivo o emulador, `./gradlew :shared:connectedAndroidDeviceTest`.
- Elegir evidencia adecuada: pruebas automatizadas para lógica y contratos; dispositivo/emulador para almacenamiento cifrado, ciclo de vida, teclado y flujo integrado. Una captura no demuestra persistencia ni red.
- Las pruebas KMP son opcionales según el enunciado, pero los criterios acordados del CU deben validarse. CI y Sonar no se exigen para este repositorio.
- Informar qué se ejecutó, qué falló y qué quedó bloqueado por el entorno. No afirmar resultados no observados.

## Issues, PR y commits

- Issues con `## Objetivo y referencias`, `## Alcance`, `## Criterios de aceptación`, `## Dependencias y decisiones pendientes` y `## Preparación e implementación`.
- PR en español con `## Alcance`, `## Verificación` y `Closes #N`, describiendo solo cambios finales y comprobaciones ejecutadas.
- Commits en Conventional Commits, en inglés e imperativo, agrupados por funcionalidad coherente.
- No crear issues, PR, commits ni push sin indicación explícita. No incluir secretos en el repositorio ni en la documentación.
