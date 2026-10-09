# CU-04. Buscar profesionales

**Estado:** Revisado y aprobado por el usuario el 2026-10-07.

## Identificación

| Campo | Descripción |
| --- | --- |
| Sistema | Aplicación KMP (Android). |
| Objetivo | Encontrar profesionales mediante los filtros obligatorios. |
| Actor principal | Usuario final. |
| Actor de apoyo | Backend Catálogo. |
| Disparador | El usuario abre Búsqueda o solicita una búsqueda con filtros. |

## Precondiciones

- El usuario dispone de una sesión válida.

## Flujo principal

1. La app ofrece filtros por categoría, nombre, estado habilitado y disponibilidad de agenda para una fecha.
2. El usuario indica los filtros y solicita la búsqueda.
3. La app presenta carga y consulta directamente Catálogo usando el JWT de usuario.
4. Catálogo devuelve los profesionales y el estado resumido aplicable; la app presenta resultados y aviso de posible desactualización cuando corresponde.
5. El usuario puede seleccionar un profesional y continuar en [CU-05](CU-05-consultar-disponibilidad.md).

## Flujos alternativos y de excepción

- **2a. Filtros inválidos:** informa el problema y permite corregir; vuelve al paso 2.
- **4a. Sin coincidencias:** muestra un resultado vacío válido y permite cambiar filtros; vuelve al paso 2.
- **4b. Catálogo actualizando o sincronización fallida con copia consistente:** muestra resultados con aviso; continúa en el paso 5.
- **4c. Sin copia consistente en Catálogo:** informa que no puede resolverse la búsqueda; no lo muestra como cero coincidencias. Termina la consulta.
- **3a/4d. Sin conexión o Catálogo inaccesible:** informa el fallo y permite volver a consultar, sin resolver consultas offline ni presentar datos guardados del dispositivo como vigentes. Termina el intento.

## Postcondiciones

- **Éxito:** resultados o vacío válidos con estado de actualización distinguible.
- **Fallo:** la indisponibilidad se diferencia de una búsqueda sin coincidencias.

## Reglas y relaciones

- Los datos locales de búsqueda pertenecen al backend Catálogo, no necesariamente al dispositivo.
- El filtro de disponibilidad significa agenda habilitada para el día de la semana elegido; no demuestra un hueco libre. CU-05 obtiene disponibilidad real de Turnos.
- La app no busca directamente en cátedra ni ejecuta su sincronización.
- Filtros y selección siguen la [conservación temporal acordada](README.md); una selección restaurada no demuestra disponibilidad vigente.

## Fuentes y pendientes

**Fuentes:** [enunciado, sección 4.1](../../../PROJECT_STATEMENT-v1.md); [CU-04 de Catálogo](../../../backend-catalogo/docs/use-cases/CU-04-buscar-profesionales.md); [CU-06 de Catálogo](../../../backend-catalogo/docs/use-cases/CU-06-estado-de-sincronizacion.md).

**Pendiente para la SPEC:** valores predeterminados, combinación concreta de filtros, búsqueda por nombre, orden y paginación. Conservación temporal ya acordada.

**Pendiente para el PLAN:** contrato de búsqueda y estado resumido, coordinado con Catálogo; no se presupone un endpoint independiente para el aviso.
