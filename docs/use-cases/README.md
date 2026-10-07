# Casos de uso de la aplicación KMP

**Estado:** Once casos revisados y aprobados por el usuario el 2026-10-07. Estos casos describen comportamiento y no sustituyen las `SPEC.md` de cada funcionalidad ni autorizan implementar. Se conservan los pendientes de SPEC/PLAN.

**Fuentes:** [enunciado](../../../PROJECT_STATEMENT-v1.md), secciones 3.2, 4, 5, 7 a 9 y 10.1; [contrato de integración](../../../INTEGRATION_REFERENCE-v2.md), secciones 2, 4, 8 a 13, 15.4 a 15.10, 17 y 18; [CU de Catálogo](../../../backend-catalogo/docs/use-cases/README.md); [CU aprobados de Turnos](../../../backend-turnos/docs/use-cases/README.md); [continuidad](../../../SESSION_HANDOFF.md).

## Estructura de las descripciones

Cada ficha complementa el diagrama con objetivo, actores, disparador, precondiciones, flujo principal, alternativas vinculadas a pasos concretos y postcondiciones. Las reglas y decisiones pendientes se registran aparte para no mezclar el recorrido normal con restricciones o diseño técnico.

Las precondiciones describen la situación necesaria para iniciar el recorrido normal; las postcondiciones explican qué queda garantizado al terminar. Una alternativa indica el paso donde se desvía el recorrido y si continúa o termina. Esta es una convención narrativa del proyecto, no una plantilla textual obligatoria de UML.

Los CU corresponden a objetivos de la persona en la app, sin imponer una pantalla, endpoint o clase por caso. CU-08 incluye consultar y retomar el proceso; la recuperación y reconciliación central pertenecen a Turnos. Los números de CU son propios de cada repositorio.

## Actores y límite

- **Persona que se registra / usuario final:** crea su cuenta, inicia o cierra sesión, busca profesionales y gestiona únicamente sus procesos y reservas.
- **Backend Catálogo:** proporciona búsquedas sobre su réplica y estado resumido de sincronización.
- **Backend Turnos:** administra cuentas, emite JWT, calcula disponibilidad real y gestiona procesos y reservas propios.
- El límite es la aplicación KMP ejecutable en Android. Consulta directamente los dos backends usando el JWT de usuario emitido por Turnos.
- Cátedra, Redis, Kafka y las bases de datos no son actores directos de KMP. Sus operaciones y credenciales quedan en los backends. No se adopta arquitectura KMP en estos documentos.

## Diagrama

Requiere Mermaid 12.0.0 o superior (`usecase-beta`). Probado en Mermaid Live 12.0.0, incluida la relación entre registro e inicio de sesión.

```mermaid
usecase-beta
direction LR
actor Usuario("Usuario final")
actor Catalogo("Backend Catálogo")
actor Turnos("Backend Turnos")

systemBoundary App["Aplicación KMP (Android)"]
  Registrar("CU-01 Registrar usuario")
  Autenticar("CU-02 Iniciar sesión")
  Salir("CU-03 Cerrar sesión")
  Buscar("CU-04 Buscar profesionales")
  Disponibilidad("CU-05 Consultar disponibilidad")
  Iniciar("CU-06 Iniciar reserva")
  Informacion("CU-07 Completar información adicional")
  Proceso("CU-08 Consultar y retomar proceso")
  Reservas("CU-09 Consultar reservas propias")
  Cancelar("CU-10 Cancelar reserva propia")
  Abandonar("CU-11 Abandonar proceso propio")
end

Usuario --> Registrar
Usuario --> Autenticar
Usuario --> Salir
Usuario --> Buscar
Usuario --> Disponibilidad
Usuario --> Iniciar
Usuario --> Informacion
Usuario --> Proceso
Usuario --> Reservas
Usuario --> Cancelar
Usuario --> Abandonar
Catalogo --> Buscar
Turnos --> Registrar
Turnos --> Autenticar
Turnos --> Disponibilidad
Turnos --> Iniciar
Turnos --> Informacion
Turnos --> Proceso
Turnos --> Reservas
Turnos --> Cancelar
Turnos --> Abandonar
Registrar ..> : include Autenticar
```

Las asociaciones indican participación, no secuencia temporal. Registro incluye inicio de sesión por el login automático acordado. Búsqueda, disponibilidad, inicio y teléfono son etapas relacionadas, sin imponer `include` o `extend` para expresar su orden. Mostrar estado de sincronización forma parte de la búsqueda; no requiere un CU o endpoint separado en la app. Cerrar sesión describe terminar el acceso local, sin presuponer revocación remota del JWT.

## Índice

| Caso | Resultado principal |
| --- | --- |
| [CU-01](CU-01-registrar-usuario.md) | Cuenta activa y login automático, o registro completado con login pendiente. Aprobado. |
| [CU-02](CU-02-iniciar-sesion.md) | Acceso autenticado a ambos backends y sesión conservada hasta vencimiento. Aprobado. |
| [CU-03](CU-03-cerrar-sesion.md) | Acceso local terminado sin abandonar ni cancelar procesos. Aprobado. |
| [CU-04](CU-04-buscar-profesionales.md) | Búsqueda con los filtros obligatorios y aviso de posible desactualización. Aprobado. |
| [CU-05](CU-05-consultar-disponibilidad.md) | Horarios libres calculados por Turnos, diferenciados de una consulta fallida. Aprobado. |
| [CU-06](CU-06-iniciar-reserva.md) | Una acción inicia hold y confirmación inicial; sin falso éxito ni segundo proceso pendiente. Aprobado. |
| [CU-07](CU-07-completar-informacion.md) | Teléfono enviado a Turnos y rechazo corregible hasta vencimiento. Aprobado. |
| [CU-08](CU-08-consultar-proceso.md) | Progreso propio recuperado desde Turnos y acciones acordes con su estado. Aprobado. |
| [CU-09](CU-09-consultar-reservas.md) | Reservas y procesos propios con estados diferenciados y aviso cuando solo hay datos locales del backend. Aprobado. |
| [CU-10](CU-10-cancelar-reserva.md) | Solicitud explícita de cancelación y presentación del resultado verificado. Aprobado. |
| [CU-11](CU-11-abandonar-proceso.md) | Abandono explícito, incluyendo cancelación si hubo confirmación concurrente. Aprobado. |

**Acuerdos existentes preservados:**

- Login automático tras registro; nombres del paciente tomados de la cuenta; JWT técnico nunca entregado a KMP.
- Búsqueda directa en Catálogo. Su filtro de disponibilidad significa agenda habilitada para el día de la semana de la fecha elegida, sin garantizar huecos libres. Turnos calcula los huecos reales.
- Última copia consistente de Catálogo con aviso durante sincronización o fallo. Reservas y procesos locales de Turnos con aviso si cátedra no está disponible. Esto no establece soporte offline en el dispositivo.
- Una acción inicia hold y confirmación REST. La aceptación inicial o el envío del teléfono no constituyen reserva confirmada.
- Un proceso pendiente por usuario, también si el cierre sigue incierto. Pueden existir varias reservas confirmadas e historial propio.
- Volver atrás o cerrar la app conserva el proceso en Turnos sin ampliar `expiresAt`. Abandonar requiere acción explícita y también autoriza cancelar si se confirmó mientras se esperaba. La [aclaración docente registrada en Turnos](../../../backend-turnos/docs/use-cases/README.md) permite cancelar holds; los mensajes reales siguen pendientes de validación del backend.
- Teléfono rechazado corregible antes del vencimiento. Resultados inciertos, confirmación, cancelación, fallo y vencimiento deben distinguirse; el reloj de la pantalla no verifica un resultado central.

**Decisión para sesión:** Confirmada el 2026-10-07 y aclarada en esta revisión: conservar sesión al cerrar y reabrir mientras el JWT sea válido; cierre explícito; sin renovación automática. Comprobar vencimiento al abrir/reabrir, volver del segundo plano y antes de una petición protegida; si se detecta vencido, no enviar esa petición. Al detectar vencimiento o rechazo de autenticación del JWT de usuario por el backend (401), aplicar la misma limpieza local que el logout manual: retirar sesión y vistas privadas, descartar formularios/filtros/selecciones y pedir login, aunque vuelva la misma persona. No forzar cierre por temporizador mientras permanece en una pantalla. Un 403 por falta de permiso, timeout, indisponibilidad o problema de autenticación técnica entre servicios no provoca logout automático. No solicitar abandono ni cancelación ni presumir fallida una operación enviada. Después de autenticarse, consultar el progreso en Turnos sin repetir operaciones enviadas cuyo resultado siga desconocido; una respuesta tardía no restablece la sesión ni los borradores descartados ni cierra una sesión nueva. Los backends validan el JWT; el control local no sustituye esa validación.

**Decisión para navegación inicial:** Confirmada el 2026-10-07 y aclarada en esta revisión: al entrar tras login o reabrir con sesión válida, abrir automáticamente el pendiente propio solo si se verifica que existe; si no existe, ir a Búsqueda. La consulta no bloquea el acceso al resto de la app ni reinicia el hold. Si falla, abrir Búsqueda con aviso no bloqueante y opción de reintentar, sin afirmar ausencia de pendiente. Las demás funciones dependen de la disponibilidad de sus servicios y Turnos sigue verificando el límite antes de iniciar otra reserva. La presentación del aviso y la ubicación de los demás accesos se precisarán en la SPEC.

**Decisión para cancelación y abandono:** Confirmada el 2026-10-07: diálogo previo que identifique profesional, fecha y hora, sin pedir motivo. Volver desde el diálogo no envía la solicitud; confirmar abandono incluye la advertencia sobre cancelar una confirmación concurrente.

**Decisión para conectividad:** Confirmada el 2026-10-07: sin consultas offline ni cola de operaciones. Si Android no puede consultar al backend, informa indisponibilidad y permite volver a consultar. No ofrece datos guardados del dispositivo como una consulta vigente. Esto conserva el fallback acordado de Turnos cuando Android puede conectarse a él pero cátedra no está disponible, con su aviso.

**Decisión para conservación de estado temporal:** Confirmada el 2026-10-07 y aclarada en esta revisión: formularios sin enviar, filtros y selecciones conservados al navegar, rotar y pasar a segundo plano mientras viva el proceso Android; descartados al terminar ese proceso, cerrar sesión, detectar vencimiento/invalidez de la sesión o cambiar cuenta. La sesión válida es la excepción ya acordada: se conserva al reabrir. El progreso enviado se consulta desde Turnos; no depende de restaurar un formulario ni garantiza resolver incertidumbres contractuales.

**Pendientes de revisión:** los borradores de issues requieren revisión y aprobación propia. Las once fichas y las decisiones de alcance están aprobadas; permanecen los detalles indicados para SPEC/PLAN.

**Pendientes de SPEC:** filtros predeterminados y búsqueda por nombre, límites de fechas, presentación y orden de reservas, vocabulario visible de estados y adaptación visual. Precisar campos y textos de cada pantalla, aplicando las políticas mobile confirmadas. Las validaciones de registro final se precisarán con Turnos, sin copiar las de la cuenta técnica.

**Pendientes de PLAN:** arquitectura del profesor, tecnologías, protección y almacenamiento de sesión, contratos propios aún no acordados, transporte de actualizaciones, timeouts y tratamiento de respuestas perdidas. No se anticipan mecanismos ni se autoriza implementación.
