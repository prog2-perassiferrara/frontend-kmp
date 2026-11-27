# Prompts para trabajar una funcionalidad

## Iniciar un caso de uso

Quiero preparar la especificación de [CU-XX y nombre] en este repositorio, asociado al issue [enlace o borrador].

1. Leé `AGENTS.md`, `docs/GENERIC_RULES.md`, el CU y las secciones pertinentes del enunciado y del contrato de integración. Inspeccioná el código real antes de proponer cambios.
2. Escribí `docs/features/<funcionalidad>/SPEC.md` de forma breve: comportamiento, alcance, requisitos RF y criterios CA verificables. No incluyas diseño técnico.
3. Presentá la SPEC para revisión. Su aprobación explícita permite preparar el PLAN técnico. La aprobación explícita del PLAN permite derivar las TASKS con dependencias y validación por RF/CA.

La implementación requiere una solicitud explícita. Antes de implementar, verificá la coherencia de SPEC, PLAN y TASKS. Registrá en TASKS las comprobaciones ejecutadas y los pendientes. Commits, push y PR requieren una indicación explícita.

## Revisar una implementación existente

Revisá [funcionalidad o archivos] contra su CU, la SPEC, el PLAN, las TASKS y el estado real del repositorio. Señalá problemas concretos de comportamiento, concurrencia, seguridad o pruebas, y explicá el fundamento. No reescribas el trabajo ajeno sin pedido. Si encontrás una contradicción documental, corregí solo las referencias vigentes y preservá el historial.
