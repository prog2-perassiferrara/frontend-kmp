# SPEC: Búsqueda de profesionales

**Estado:** Implementada y verificada el 2026-11-08.
**CU e issue de referencia:** [CU-04](../../use-cases/CU-04-buscar-profesionales.md); issue #7.
**Fuentes:** [CU-04 de KMP](../../use-cases/CU-04-buscar-profesionales.md), CU-04 de Catálogo, contrato de Catálogo, enunciado e [instrucciones KMP](../../../AGENTS.md).

## Objetivo

Permitir que una persona autenticada encuentre profesionales sobre la copia local de Catálogo, combinando categoría, nombre, estado habilitado y agenda semanal para una fecha, con resultados paginados y aviso de posible desactualización.

## Alcance

- **RF-01 · Filtros:** ofrecer categoría (opciones de Catálogo), nombre (fragmento de nombre completo, máximo 100 caracteres), estado (todos, habilitados, deshabilitados) y disponibilidad (fecha opcional elegida con el calendario de la plataforma y enviada como `AAAA-MM-DD`). Los filtros se combinan con AND y los vacíos se omiten.
- **RF-02 · Disponibilidad:** el filtro de fecha significa agenda habilitada para ese día de la semana; no promete horarios libres.
- **RF-03 · Consulta:** `GET /api/catalog/professionals` con el JWT de usuario, página inicial 0 y tamaño 20. No se consulta cátedra, Redis ni Kafka, ni se ejecuta sincronización.
- **RF-04 · Resultados:** mostrar nombre y apellido, categoría, habilitación del profesional y de su categoría, y totales. El orden lo define Catálogo (apellido, nombre, ID).
- **RF-05 · Paginación:** permitir avanzar y retroceder por las páginas informadas, sin solicitar páginas fuera de rango.
- **RF-06 · Aviso de copia:** si `catalog.state` no es `CURRENT` o `copyAvailable` es falso, mostrar un aviso de posible desactualización junto con los resultados o el vacío válido.
- **RF-07 · Estados:** mostrar carga, resultado vacío válido, error de filtros, indisponibilidad de Catálogo y reintento manual. La falta de copia (`CATALOG_COPY_UNAVAILABLE`) no se presenta como cero coincidencias.
- **RF-08 · Categorías:** obtener `GET /api/catalog/categories` para el filtro, incluidas las deshabilitadas. Un fallo de categorías no impide buscar y se informa.
- **RF-09 · Sesión:** reutilizar el JWT y el cliente protegido existentes. Un 401 conserva el cierre de sesión ya implementado. Al cerrar sesión se descartan filtros y resultados.
- **RF-10 · Navegación mínima:** la búsqueda sustituye el área autenticada provisoria y conserva una acción de cierre de sesión. No se agregan destinos, pestañas ni flujos de reserva.
- **RF-11 · Conservación:** filtros, resultados y página se conservan al rotar o volver del segundo plano mientras viva el proceso; se pierden al terminar el proceso o cerrar sesión.

## Fuera de alcance

- Selección de un profesional y navegación a CU-05, disponibilidad real y reservas.
- BottomBar, pestañas, pantallas futuras y navegación hacia procesos.
- Consultas offline, caché local de resultados, orden configurable y tamaños de página distintos de 20.
- Sincronización del catálogo y acceso a cátedra, Redis o Kafka.

## Flujo de usuario

1. Al entrar autenticada, la app consulta sin filtros y muestra la primera página.
2. La persona ajusta filtros y elige “Buscar”; la app valida el formato local y consulta desde la página 0.
3. Mientras espera se muestra el progreso y se deshabilitan las acciones.
4. Con resultados se muestran los profesionales, los totales y la paginación; si la copia no es vigente, aparece el aviso.
5. Sin coincidencias se informa un vacío válido. Ante un error se explica el problema y se ofrece “Reintentar”.

## Reglas y casos alternativos

- Catálogo es la autoridad de filtros, orden, límites y formato de fecha; la app solo descarta nombre de más de 100 caracteres y fechas que no respetan `AAAA-MM-DD`.
- El nombre se envía sin espacios exteriores y un nombre vacío omite el filtro.
- El filtro de estado admite “Todos” (parámetro omitido), habilitados y deshabilitados.
- La fecha de disponibilidad se muestra como `DD/MM/AAAA`, se envía como `AAAA-MM-DD` y puede quitarse. Cambiarla o quitarla invalida la paginación anterior hasta la próxima búsqueda; los demás filtros se conservan.
- Las categorías deshabilitadas se muestran en resultados y opciones; no se ocultan.
- Una página posterior al final conserva los totales reales; la app solo navega entre páginas válidas.
- El aviso de desactualización no convierte la búsqueda en un error ni impide paginar.

## Criterios de aceptación

- **CA-01 · RF-01, RF-03:** los cuatro filtros se envían combinados y con los nombres del contrato; los vacíos se omiten y la búsqueda parte de `page=0`, `size=20`.
- **CA-02 · RF-04, RF-05:** los resultados muestran nombre, categoría y habilitación; los totales coinciden con el backend y la paginación solicita páginas adyacentes válidas.
- **CA-03 · RF-06:** con `catalog.state != CURRENT` o `copyAvailable=false` se muestra el aviso; con `CURRENT` no aparece.
- **CA-04 · RF-07:** un resultado vacío se informa como falta de coincidencias, sin error ni reintento.
- **CA-05 · RF-07:** `503 CATALOG_COPY_UNAVAILABLE` y la indisponibilidad de Catálogo se informan como error, diferenciados del vacío.
- **CA-06 · RF-07:** un fallo de transporte se informa y “Reintentar” repite la consulta vigente.
- **CA-07 · RF-09:** un 401 cierra la sesión con el comportamiento existente y al cerrar sesión se descartan filtros y resultados.
- **CA-08 · RF-08:** las categorías pobladas por Catálogo se ofrecen en el filtro; un fallo de categorías no bloquea la búsqueda.
- **CA-09 · RF-10, RF-11:** el área autenticada muestra la búsqueda y conserva filtros y resultados durante la rotación; no se agregan destinos futuros.
- **CA-10 · RF-02:** la pantalla aclara que la fecha filtra agenda semanal y no garantiza horarios libres.

## Verificación

La evidencia por criterio, los comandos y los límites están en [TASKS.md](TASKS.md). Las decisiones técnicas están en [PLAN.md](PLAN.md).
