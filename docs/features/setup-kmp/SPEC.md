# SPEC: Base KMP ejecutable en Android

**Estado:** Aprobada <!-- Borrador | En revisión | Aprobada -->
**Aprobación:** Confirmada por el usuario el 2026-10-31.
**CU e issue de referencia:** [Issue #3](https://github.com/prog2-perassiferrara/frontend-kmp/issues/3). Setup transversal; los [CU aprobados](../../use-cases/README.md) se implementarán en sus funcionalidades.
**Fuentes y acuerdos relacionados:** [CU aprobados](../../use-cases/README.md), [guía mobile](../../MOBILE_GUIDELINES.md) e [instrucciones KMP](../../../AGENTS.md). Acuerdos del setup: base del wizard para Android, orientación a objetos y MVVM; Material/Material 3 se decide en el PLAN.

<!-- PARA LA PERSONA
Copia esta plantilla como SPEC.md en una carpeta de la funcionalidad.
Pide al agente que la complete contigo usando MOBILE_GUIDELINES.md.
SPEC.md define qué debe cumplirse; PLAN.md desarrolla cómo implementarlo;
TASKS.md organiza los pasos de ejecución.
-->

<!-- PARA EL AGENTE
- Lee las instrucciones del proyecto y MOBILE_GUIDELINES.md. Inspecciona el
  repositorio para comprobar el comportamiento actual. Si falta la guía, pide su ubicación.
- Completa esta spec con la persona: investiga lo comprobable y consulta las
  decisiones pendientes. Haz pocas preguntas por vez y actualiza las respuestas.
- No inventes requisitos ni exclusiones. Distingue propuestas de decisiones
  confirmadas y marca como PENDIENTE lo que aún no esté resuelto.
- Aplica las consideraciones mobile relevantes sin ampliar el alcance automáticamente.
- No incluyas diseño de clases, tablas, componentes, archivos o algoritmos:
  esos detalles pertenecen a PLAN.md. Sí registra restricciones explícitas del pedido.
- Mantén el documento breve y proporcional a la funcionalidad. Conserva los comentarios.
- Un documento completo no está aprobado automáticamente. Solicita aprobación
  antes de marcarlo como Aprobada. No implementes durante esta etapa.
- Para cambios de significado posteriores a la aprobación, registra una nota
  breve de cambio, motivo e impacto según AGENTS.md, sin historial separado.
-->

## Qué construimos y para quién

<!-- Qué necesidad resolvemos, quién tiene esa necesidad y qué podrá hacer.
Describe el objetivo en lenguaje de producto. -->

Preparar una aplicación KMP que pueda compilarse y ejecutarse en Android de forma
reproducible. Su pantalla inicial tendrá una interacción local mínima para
comprobar el recorrido entre acción, estado e interfaz antes de incorporar los CU.
La demo es descartable y se limita a lo necesario para validar el setup, sin
desarrollo visual adicional ni componentes pensados para funcionalidades futuras.

## Situación actual

<!-- Comportamiento actual relevante, limitación que queremos resolver y
comportamientos existentes que deben conservarse. No describas la arquitectura. -->

El repositorio contiene documentación y configuración de GitHub, sin código ni
build. La base `FrontendKmp`, generada con el wizard oficial para Android y
`com.prog2.frontend`, permanece fuera del repositorio y solo fue revisada
estáticamente. No se han comprobado su compilación ni ejecución. Deben conservarse
la documentación existente, sus aprobaciones y la configuración del repositorio.

## Dentro del alcance

<!-- Requisitos concretos, con identificadores estables para vincularlos a
criterios, decisiones del plan y tareas. -->

- **RF-01:** Incorporar la base del wizard y permitir compilar e instalar una aplicación KMP ejecutable en Android, fuera de contenedores.
- **RF-02:** Mostrar una pantalla inicial identificable como `FrontendKmp`, con una acción local para mostrar u ocultar un contenido demostrativo. La interfaz refleja el estado actual y cada pulsación produce la transición correspondiente, sin operaciones de negocio.
- **RF-03:** Conservar ese estado durante recomposiciones, rotación y regreso del segundo plano mientras viva el proceso Android. Tras terminarse el proceso, iniciar nuevamente en el estado inicial, sin persistir el estado demostrativo.
- **RF-04:** Ofrecer un build reproducible mediante el wrapper y configuración versionada, sin depender de rutas absolutas de esta máquina. Preservar los archivos existentes al incorporar la base y mantener fuera de Git SDK local, cachés y resultados de compilación.
- **RF-05:** El arranque y la interacción no necesitan los backends ni cátedra, no realizan llamadas remotas ni solicitan permisos de dispositivo. No incorporar cuentas, credenciales, JWT ni almacenamiento de datos de negocio.
- **RF-06:** Documentar requisitos, configuración local y comandos realmente comprobados para compilar, instalar y ejecutar en Android, junto con el alcance del setup y sus límites de validación.

## Fuera de alcance

<!-- Exclusiones acordadas, no deducidas por el agente. Si no hay exclusiones
adicionales, indícalo tras revisarlo con la persona. -->

- Registro, login, sesión/JWT, búsquedas, disponibilidad, procesos, reservas, cancelación y abandono: permanecen en los issues funcionales.
- Plataformas adicionales, navegación entre pantallas de negocio, clientes HTTP, bases locales, consultas offline, colas y tareas en segundo plano.
- Configuración de CI, SonarCloud, Kover o un porcentaje obligatorio de cobertura para KMP. Las pruebas automatizadas siguen siendo opcionales; comprobar los criterios acordados sí es necesario.

## Flujo de usuario

<!-- Cómo se inicia, qué hace el usuario y qué resultado obtiene.
Incluye pantallas afectadas, navegación y alternativas relevantes. -->

1. Con los requisitos documentados, se compila e instala la aplicación en un emulador o dispositivo Android.
2. Al abrirla, aparece la pantalla inicial con el contenido demostrativo oculto.
3. Una pulsación lo muestra; otra lo oculta. La interacción es exclusivamente local.
4. Al rotar o regresar del segundo plano se conserva el estado si el proceso sigue vivo; después de terminarlo, la reapertura vuelve al paso 2.

## Datos y reglas de negocio

<!-- Información que necesita el usuario, campos obligatorios, validaciones,
límites y reglas como duplicados u orden de presentación. Describe significado
y comportamiento, sin diseñar tablas, DTO, DAO ni almacenamiento. -->

- Solo hay estado demostrativo temporal, con un único origen y sin identidad de usuario ni datos de catálogo o reservas.
- Mostrar u ocultar contenido no inicia un proceso, no reserva un horario ni acredita disponibilidad de los backends.
- El primer aprovisionamiento de herramientas y dependencias puede necesitar Internet; ejecutar esta pantalla no requiere conectividad. Esto no agrega soporte offline a los CU.

## Comportamiento mobile y casos alternativos

<!-- Adapta la tabla usando MOBILE_GUIDELINES.md. Añade escenarios relevantes.
Marca No aplica con su motivo cuando corresponda. No presupongas soporte offline
ni conservación de todo el estado. Expresa resultados, no mecanismos técnicos. -->

| Situación | Comportamiento esperado |
| --- | --- |
| Carga o acción en curso | La pantalla responde a la acción local sin bloquear la interfaz ni simular una espera remota. |
| Pulsaciones repetidas o respuesta perdida | Cada pulsación alterna el estado; no hay solicitudes ni resultados remotos que reconciliar. |
| Sin datos | La pantalla inicial es válida sin datos de negocio ni servicios externos. |
| Entrada inválida | No aplica: no hay formulario ni entrada libre. |
| Error o espera excesiva | Un fallo de build o ejecución se diagnostica como tal; no se presenta como arranque verificado. |
| Sin conexión o conexión interrumpida | La pantalla local mantiene su funcionamiento; no intenta consultas ni pone acciones en cola. |
| Cancelar o volver atrás | Se respeta la salida de Android; no hay navegación interna ni operaciones que cancelar. |
| Pasar a segundo plano y regresar | Conserva estado si el proceso continúa; no añade trabajo en segundo plano. |
| Recrear la pantalla | La rotación conserva el estado dentro del mismo proceso. |
| Reabrir después de terminarse el proceso | Vuelve al estado inicial, sin restauración persistente del contenido demostrativo. |
| Sesión vencida, cierre de sesión o cambio de usuario | No aplica: este setup no establece sesión. |
| Otros puntos aplicables de la guía | Texto y acción legibles y accesibles, sin depender solo del color; conservar su uso al rotar o aumentar el texto. |

**Puntos de la guía no aplicables y motivo:** permisos, persistencia/migraciones, sincronización, formularios y trabajo en segundo plano no tienen una operación que resolver en este setup. No introducirlos por anticipación.

## Restricciones del pedido

<!-- Condiciones ya impuestas: compatibilidad, límites de alcance, requisitos
de accesibilidad o rendimiento medibles, o una tecnología expresamente exigida.
Ejemplo: Usar Room puede ser una restricción; el diseño de entidades va en PLAN.md.
No conviertas una preferencia del agente en una restricción. -->

- Usar KMP, Android, Gradle con Kotlin DSL y Compose Multiplatform, partiendo de la base ya generada con el wizard. Conservar `FrontendKmp` y `com.prog2.frontend` como nombre técnico e identificador elegidos.
- **MVVM es una restricción explícita del usuario.** La vista representa el estado y comunica acciones; el ViewModel gestiona el estado de presentación y sus transiciones mediante flujo unidireccional. La pantalla inicial debe demostrar esa separación, con estado observable e inmutable para sus consumidores, sin duplicar su origen.
- Seguir la orientación a objetos de las clases: pantallas y componentes pertinentes representados por clases con responsabilidades claras y contenido composable, colaboradores recibidos explícitamente cuando existan y ausencia de acceso a red/BD dentro de la UI. La integración de datos futura se resolverá fuera de la vista mediante los repositorios pertinentes; este setup no crea repositorios, modelos de negocio ni capas vacías.
- La distribución concreta de clases, APIs de estado/ciclo de vida, documentación Kotlin y dependencias corresponde al PLAN. No trasladar automáticamente hexagonal de los backends ni añadir navegación o frameworks de DI sin una necesidad del alcance.
- **Material o Material 3 y sus versiones quedan pendientes para el PLAN.** Priorizar compatibilidad y estabilidad; la versión alpha generada por el wizard no se considera aprobada por haber sido descargada. Las demás versiones y el JDK/SDK también deben validarse antes de adoptarlos.
- Esta etapa solo prepara la SPEC. Su aprobación no autoriza copiar la base, implementar ni modificar Git.

## Criterios de aceptación

<!-- Resultados observables que permitan decidir si se cumple cada requisito.
Incluye los casos alternativos acordados. No uses Funciona correctamente.
Repite el formato según sea necesario. -->

- **CA-01 · RF-01, RF-04:** Con los requisitos documentados y las dependencias disponibles, el wrapper compila un APK instalable sin requerir un Gradle global ni rutas privadas versionadas.
- **CA-02 · RF-01, RF-02:** Instalar y abrir el APK muestra la pantalla inicial; pulsaciones sucesivas muestran y ocultan el contenido. La vista consume estado y envía acciones al ViewModel, sin gestionar otra copia del mismo estado ni realizar efectos de negocio.
- **CA-03 · RF-03:** Tras mostrar el contenido, rotar y pasar a segundo plano/regresar conserva ese estado mientras permanece vivo el proceso; recomponer la interfaz no lo reinicia.
- **CA-04 · RF-03:** Terminar el proceso y reabrir devuelve el contenido al estado inicial oculto, diferenciándolo de una simple recreación de pantalla.
- **CA-05 · RF-05:** Con ambos backends y cátedra inaccesibles, la app abre y permite alternar el contenido sin conexiones, credenciales, permisos de dispositivo ni persistencia de negocio.
- **CA-06 · RF-04:** La incorporación conserva los CU aprobados, las reglas/plantillas y CODEOWNERS; adapta el README y las exclusiones solo cuando corresponde. No versiona configuración local del SDK, cachés, secretos ni artefactos de build.
- **CA-07 · RF-02, RF-06:** Los comandos y requisitos del README permiten reproducir compilación y ejecución; la pantalla y su acción siguen utilizables en ambas orientaciones y con texto aumentado. La evidencia distingue compilación, ejecución e inspección de MVVM, sin presentar pruebas de ejemplo como validación funcional.

## Cómo se comprueba el comportamiento

<!-- Una fila por criterio: escenario y resultado que debemos comprobar.
La selección de tests, herramientas, comandos y evidencias se desarrolla en PLAN.md.
No marques los criterios como superados durante la especificación. -->

| Criterio | Condiciones y pasos | Resultado esperado |
| --- | --- | --- |
| CA-01 | Seguir los requisitos y compilar mediante el wrapper. | APK generado; configuración reproducible sin Gradle global ni rutas privadas versionadas. |
| CA-02 | Instalar, abrir y alternar el contenido; inspeccionar el recorrido de acción/estado. | Interacción visible y separación MVVM efectiva, sin estado duplicado ni efectos de negocio. |
| CA-03 | Mostrar contenido, rotar y regresar del segundo plano con proceso vivo. | Estado conservado; no reinicio por recomposición/recreación. |
| CA-04 | Terminar el proceso y volver a abrir la aplicación. | Estado inicial oculto, sin restauración persistente. |
| CA-05 | Repetir el flujo sin servicios disponibles e inspeccionar sus dependencias y efectos. | Sin llamadas, credenciales ni requisitos de datos/permisos de dispositivo. |
| CA-06 | Revisar los archivos incorporados y las exclusiones frente al estado previo. | Documentación/aprobaciones conservadas y archivos locales/generados excluidos. |
| CA-07 | Reproducir el README y revisar interfaz con rotación y texto aumentado. | Comandos comprobados e interacción accesible; evidencia con método, revisión y límites. |

## Decisiones pendientes

<!-- Al resolverlas, actualiza las secciones afectadas. Escribe Ninguna cuando
no queden pendientes funcionales ni restricciones por decidir. -->

- Sin decisiones funcionales pendientes: se confirmó la interacción de mostrar/ocultar como demo mínima y descartable.
- Las decisiones técnicas del setup están resueltas en el [PLAN aprobado](PLAN.md). El setup no necesita elegir aún clientes HTTP, persistencia ni navegación de negocio.

<!-- ANTES DE SOLICITAR APROBACIÓN
Comprueba que el alcance está acordado, los flujos son coherentes, los puntos
mobile relevantes están cubiertos y cada requisito tiene criterios comprobables.
Resuelve las dudas y los marcadores pendientes. Mantén el diseño técnico en PLAN.md.
-->
