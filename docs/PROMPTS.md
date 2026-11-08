# Prompts para trabajar una funcionalidad

## Iniciar un caso de uso

Quiero implementar [CU-XX y nombre] en este repositorio, asociado al issue [enlace o borrador].

1. Leé `AGENTS.md`, `docs/GENERIC_RULES.md`, el CU y las secciones pertinentes del enunciado y del contrato de integración. Inspeccioná el código real antes de proponer cambios.
2. Escribí `docs/features/<funcionalidad>/SPEC.md` de forma breve: comportamiento, alcance, requisitos RF y criterios CA verificables. No incluyas diseño técnico.
3. Escribí `PLAN.md` en la misma carpeta: decisiones técnicas, archivos afectados y validación. Referenciá la SPEC por RF/CA sin copiarla.
4. Derivá `TASKS.md`: pasos ordenados, dependencias, referencias a RF/CA y método de validación. Sin matrices de trazabilidad.
5. Implementá el caso de uso completo, con cambios mínimos y las pruebas pertinentes. No agregues pantallas, navegación, capas ni dependencias que el CU todavía no necesite.
6. Ejecutá las comprobaciones disponibles y registrá en `TASKS.md` la evidencia real, lo pendiente y su motivo. No afirmes resultados no observados.

La autorización para desarrollar el CU cubre estas etapas: no pidas aprobaciones rutinarias intermedias. Consultá solo decisiones que cambien comportamiento, contratos, alcance o impliquen cambios destructivos. No hagas commits, push ni PR salvo que lo pida explícitamente.

## Revisar una implementación existente

Revisá [funcionalidad o archivos] contra su CU, la SPEC, el PLAN, las TASKS y el estado real del repositorio. Señalá problemas concretos de comportamiento, concurrencia, seguridad o pruebas, y explicá el fundamento. No reescribas el trabajo ajeno sin pedido. Si encontrás una contradicción documental, corregí solo las referencias vigentes y preservá el historial.
