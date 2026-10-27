# Prompt para iniciar una especificación

Quiero que me ayudes a escribir la especificación de [funcionalidad] para este repositorio. Seguimos un flujo SPEC → PLAN → TASKS → implementación → evidencia; esta solicitud cubre únicamente la especificación.

Los CU y el issue organizan el trabajo previo; no sustituyen la SPEC ni aprueban su implementación. Si el issue todavía es un borrador local, conservar ese estado y no publicarlo.

1. Lee las instrucciones del proyecto, `docs/GENERIC_RULES.md`, `docs/SPEC_TEMPLATE.md` y la documentación pertinente del enunciado y del contrato de integración.
2. Inspecciona el estado real del repositorio. No presupongas que existe código o comportamiento implementado.
3. Consulta `docs/MOBILE_GUIDELINES.md` y aplica únicamente los puntos pertinentes a la funcionalidad. No anticipes las decisiones arquitectónicas pendientes del profesor.
4. Lee el CU pertinente y los acuerdos de los backends. Prepara un borrador de `SPEC.md` en `docs/features/<funcionalidad>/`, con hechos comprobados, alcance, reglas, errores y criterios de aceptación verificables. Usa la plantilla y conserva sus comentarios.
5. Distingue requisitos confirmados, propuestas y decisiones pendientes. Haz pocas preguntas por vez y actualiza el borrador con mis respuestas. No conviertas propuestas en decisiones aprobadas.
6. Mantén el diseño técnico para `PLAN.md`. Cuando solicite esa etapa, utiliza `docs/PLAN_TEMPLATE.md` y las decisiones arquitectónicas acordadas para KMP.

Mantén los documentos proporcionales al alcance y referencia los acuerdos existentes sin repetirlos. Para cambios sustanciales posteriores a una aprobación, aplica la nota breve definida en `AGENTS.md`.

No marques documentos como aprobados sin mi confirmación. No implementes durante la especificación ni la planificación; la implementación requiere una solicitud explícita.

## Prompt para planificar una funcionalidad

Quiero preparar `PLAN.md` junto a la SPEC aprobada de [funcionalidad], usando `docs/PLAN_TEMPLATE.md`. Revisá los contratos consumidos de Catálogo y Turnos, las decisiones mobile y el estado real del repositorio. Si faltan las indicaciones arquitectónicas del profesor, dejá esa decisión pendiente y no inventes una arquitectura KMP. Consultá las decisiones técnicas abiertas, vinculá la solución y su validación con RF/CA y mantené las propuestas diferenciadas de los acuerdos. No implementes ni marques el PLAN como aprobado sin confirmación.

## Prompt para derivar tareas

Quiero derivar `TASKS.md` del PLAN aprobado de [funcionalidad], en la misma carpeta. Cada tarea tendrá casilla, ID, objetivo, alcance, dependencias, referencias RF/CA y método de validación. Registrá los resultados solo después de ejecutar las comprobaciones. No empieces la implementación sin autorización explícita ni elijas decisiones que siguen pendientes en el PLAN.

Antes de implementar, realizá el chequeo de coherencia definido en `AGENTS.md` y registrá una línea en `TASKS.md`. Al terminar la funcionalidad, completá su cierre breve con CA/evidencia, regresión y desviaciones del PLAN, sin otro reporte.
