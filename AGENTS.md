# Guía para agentes: Aplicación KMP

## Lectura y alcance

Las rutas de este documento son relativas a la raíz de este repositorio.

- Antes de planificar, revisar o modificar, leer explícitamente [las instrucciones generales](../AGENTS.md) y [las reglas de trabajo](docs/GENERIC_RULES.md). No suponer que el archivo padre o los enlaces se cargan automáticamente. Si ya fueron leídos en la tarea y siguen vigentes, no repetir la lectura.
- Consultar las secciones pertinentes del [enunciado](../PROJECT_STATEMENT-v1.md) y del [contrato de integración](../INTEGRATION_REFERENCE-v2.md). Las reglas locales especializan las generales; no sustituyen los requisitos ni los contratos de cátedra.
- Si el repositorio se abre o clona por separado y faltan los documentos compartidos, o sus permisos impiden leerlos, informar qué falta y solicitar su ubicación para el trabajo que dependa de ellos. No inventar su contenido ni copiarlos como versiones independientes.
- Responder en español. Priorizar el aprendizaje: explicar decisiones y orientar las revisiones sin implementar soluciones no solicitadas.
- El estado actual es setup. Verificar el contenido real antes de trabajar; no generar código ni elegir dependencias por la sola presencia de estas instrucciones.
- Revisar `git status --short` en este repositorio antes de editar. Conservar cambios del usuario; una tarea local no autoriza a modificar los otros repositorios.

## Flujo por funcionalidad

1. Leer `docs/SPEC_TEMPLATE.md` para especificar y `docs/PLAN_TEMPLATE.md` para planificar. Usar `docs/PROMPTS.md` como ayuda, no como requisitos del producto.
2. Mantener los documentos en `docs/features/<nombre-de-funcionalidad>/`. Reutilizar la carpeta existente al continuar una funcionalidad. No crear documentos de funcionalidades no solicitadas.
3. Completar `SPEC.md` con comportamiento, alcance y criterios verificables, distinguiendo hechos, propuestas y pendientes. Mantener los IDs `RF-` y `CA-` de la plantilla; conservar su estructura y comentarios. No introducir diseño técnico en la SPEC.
4. Tras aprobación explícita de la SPEC, preparar `PLAN.md` con responsabilidades, contratos, decisiones técnicas y validación. Referenciar requisitos y criterios sin copiar la SPEC. No alterar las plantillas compartidas por una funcionalidad puntual.
5. Tras aprobación explícita del PLAN, derivar `TASKS.md`: tareas pequeñas y ordenadas, cada una con casilla de seguimiento, ID, objetivo, alcance, dependencias, criterios relacionados y método de validación.
6. Implementar únicamente ante autorización explícita del usuario. No deducirla de un documento completo o aprobado, ni solicitar de nuevo autorizaciones ya otorgadas.
7. Registrar resultados y evidencias reales en `TASKS.md` o en el informe de validación acordado. No marcar tareas completas con comprobaciones pendientes. Si cambia un requisito, actualizar y revisar los documentos afectados antes de implementar ese cambio.

Las funcionalidades que cruzan repositorios deben referenciar los contratos y documentos relacionados, identificando productor, consumidor y decisiones pendientes. No declarar acordado un contrato unilateralmente.

Los casos de uso de `docs/use-cases/` deben mantener el formato de `../backend-catalogo/docs/use-cases/`: identificación en tabla y las mismas secciones y orden en las fichas; README con estructura de las descripciones, actores y límite, diagrama Mermaid `usecase-beta` e índice. Adaptar actores y responsabilidad a la app y registrar su propio estado de revisión, sin copiar aprobaciones de los backends.

## Issues, PR y commits

- Los issues siguen la estructura de los issues funcionales de Catálogo: `## Objetivo y referencias`, `## Alcance`, `## Criterios de aceptación`, `## Dependencias y decisiones pendientes` y `## Preparación e implementación`. Conservar decisiones confirmadas, referencias y pendientes pertinentes a KMP; no trasladar arquitectura ni obligaciones de pruebas de los backends.
- Los borradores locales sirven para revisión previa y no autorizan publicar. No se requiere una carpeta de issues permanente en el repositorio; una vez publicados y verificados, pueden retirarse los borradores y sus enlaces locales, utilizando los issues reales como referencia.
- Los PR se escriben en español, con título que identifique el cambio y cuerpo con `## Alcance` y `## Verificación`, seguido de `Closes #N` para los issues que resuelvan. Describir únicamente cambios finales y verificaciones realmente ejecutadas; señalar comprobaciones pendientes cuando corresponda. Es el formato del PR de setup de Catálogo, adaptado al alcance de cada cambio.
- Los commits siguen Conventional Commits en inglés: `<type>(<scope>): <description>` o `<type>: <description>` si no necesitan scope. Descripción imperativa, sin punto final; `docs(use-cases)` para CU y `docs(sdd)` para reglas/plantillas son las convenciones verificadas en los backends. Mantener unidades coherentes por funcionalidad y separar cambios sin relación.
- No crear issues, PR, commits ni hacer push sin indicación explícita del usuario.

## Responsabilidad de la aplicación

- Ofrecer en Android los flujos requeridos de registro e inicio de sesión, búsqueda y filtros, disponibilidad, reserva con información adicional, consulta y cancelación de reservas propias.
- Consumir las APIs de los backends del proyecto con la identidad del usuario final. No acceder directamente a bases de datos, Redis o Kafka de cátedra ni recibir sus credenciales o JWT técnico.
- Reflejar los resultados del backend: una confirmación inicial o una acción local no equivalen por sí solas a una reserva confirmada.
- Referenciar los contratos de los backends al especificar cada flujo. Marcar como pendientes las operaciones o respuestas aún no acordadas; no inventar APIs para completar una pantalla.
- KMP consulta directamente Catálogo para búsquedas y estado resumido de sincronización, y Turnos para cuentas, disponibilidad real, procesos y reservas. Turnos emite el JWT de usuario que ambos backends validan.
- Conservar el login automático tras registro, el único proceso pendiente por usuario y el abandono explícito acordados en los CU de Turnos. Salir de una pantalla, cerrar la app o cerrar sesión no solicita cancelación ni extiende el vencimiento. Un resultado incierto no habilita crear otro proceso.
- Decisión de sesión confirmada: conservarla al reabrir mientras el JWT siga válido y ofrecer cierre explícito, sin renovación automática. Comprobar vencimiento al abrir/reabrir, volver del segundo plano y antes de una petición protegida; si se detecta vencido, no enviar la petición. Al detectar vencimiento o rechazo de autenticación del JWT de usuario por el backend (401), aplicar la misma limpieza local que el logout manual, incluyendo formularios/filtros/selecciones y vistas privadas, y solicitar login aunque vuelva la misma persona. No forzar cierre por temporizador mientras permanece en una pantalla. Un 403 por falta de permiso, timeout, indisponibilidad o error de autenticación técnica entre servicios no provoca logout automático. No cancelar ni abandonar procesos; consultar el progreso en Turnos después de autenticarse, sin repetir a ciegas operaciones enviadas. Las respuestas tardías no restablecen la sesión ni los datos descartados ni cierran una sesión nueva; el mecanismo se define en el PLAN.
- Decisiones de navegación y acciones confirmadas: abrir automáticamente el pendiente propio al entrar solo si se verifica que existe. Su consulta no bloquea el resto de la app; sin pendiente o ante fallo, abrir Búsqueda. Ante fallo, aviso no bloqueante y opción de reintentar, sin deducir ausencia ni omitir el control de Turnos antes de otra reserva. Cancelar y abandonar requieren un diálogo previo que identifique profesional, fecha y hora; no solicitar motivo. Volver desde el diálogo no envía la operación.
- Alcance mobile confirmado el 2026-10-07: sin consultas offline ni cola de operaciones. Formularios sin enviar, filtros y selecciones se conservan al navegar, rotar o pasar a segundo plano mientras viva el proceso Android; se descartan al terminarlo, cerrar sesión, detectar vencimiento/invalidez de la sesión o cambiar cuenta. La sesión válida sí se conserva para reabrir; el progreso enviado se consulta en Turnos, sin extender vencimientos ni prometer recuperar estados que el contrato todavía no permite verificar.

## Planificación y comportamiento mobile

- Consultar `docs/MOBILE_GUIDELINES.md` al especificar, planificar y validar. Aplicar solo los aspectos pertinentes y registrar el motivo de los no aplicables.
- Definir con el usuario estados de carga, vacío, error y éxito; navegación, interrupciones, restauración de estado, conectividad y prevención de envíos duplicados según la funcionalidad.
- No presumir soporte offline, persistencia local, reintentos de reservas o almacenamiento de credenciales. Distinguir los requisitos confirmados de decisiones por resolver.
- La arquitectura KMP queda pendiente de las indicaciones del profesor. No trasladar automáticamente la skill hexagonal de los backends ni adoptar una arquitectura o librería de otro ejemplo.
- Mantener separadas la especificación del comportamiento y las decisiones técnicas del PLAN. No elegir plataformas adicionales, librerías, versiones o módulos sin necesidad vinculada al pedido.

## Verificación

- En el setup actual no hay comandos de compilación ni pruebas disponibles. Cuando se incorpore el build, verificar wrappers y tareas reales y documentar los comandos en el README; no inventarlos.
- Cuando se implemente, seleccionar evidencia adecuada para cada criterio: comprobaciones automatizadas o manuales en dispositivo/emulador según el comportamiento. Las pruebas automatizadas KMP son opcionales según el enunciado; eso no elimina la validación de los criterios acordados.
- Una captura no demuestra persistencia, recuperación tras cierre ni comportamiento de red. Registrar las comprobaciones reales y sus limitaciones.
- Para cambios solo documentales, revisar coherencia, rutas y referencias; no ejecutar builds ajenos ni crear pruebas artificiales.
