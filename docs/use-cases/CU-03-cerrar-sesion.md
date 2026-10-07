# CU-03. Cerrar sesión

**Estado:** Revisado y aprobado por el usuario el 2026-10-07. Cierre explícito confirmado por el usuario el 2026-10-07.

## Identificación

| Campo | Descripción |
| --- | --- |
| Sistema | Aplicación KMP (Android). |
| Objetivo | Terminar el acceso local de la cuenta actual. |
| Actor principal | Usuario final. |
| Actores de apoyo | Ninguno para terminar el acceso local. |
| Disparador | El usuario solicita explícitamente cerrar sesión. |

## Precondiciones

- Hay una sesión iniciada en la app.

## Flujo principal

1. El usuario elige cerrar sesión.
2. La app deja de utilizar la sesión, descarta formularios/filtros/selecciones temporales y retira los datos de la cuenta de las vistas accesibles.
3. La app presenta el acceso a registro e inicio de sesión.
4. Para volver a consultar procesos o reservas, se exige autenticación mediante [CU-02](CU-02-iniciar-sesion.md).

## Flujos alternativos y de excepción

- **2a. Hay un proceso pendiente o una operación enviada:** cierra el acceso local sin solicitar abandono ni cancelación. El progreso se conserva en Turnos; una respuesta tardía no restablece la sesión ni aparece en la cuenta siguiente. Continúa en el paso 3.
- **2b. Sin conexión:** termina el acceso local; no depende de una respuesta del backend para hacerlo. Continúa en el paso 3.
- **4a. Se autentica otra persona:** solo se muestran registros de esa identidad; no se reutilizan vistas o respuestas privadas de la cuenta anterior.

## Postcondiciones

- **Éxito:** la sesión anterior ya no permite acceder desde la app a funciones protegidas; no se exponen sus datos a otra cuenta.
- Los procesos y reservas conservan su propietario y estado en Turnos.

## Reglas y relaciones

- Cerrar sesión no es abandonar: [CU-11](CU-11-abandonar-proceso.md) requiere su propia acción explícita.
- El cierre no demuestra revocación remota del JWT ni altera el vencimiento del hold.
- [CU-02](CU-02-iniciar-sesion.md) aplica esta misma limpieza local al detectar vencimiento o rechazo del JWT de usuario como inválido, al abrir/reabrir, volver del segundo plano, antes de una petición protegida o ante rechazo de autenticación del backend. No se fuerza el cierre por temporizador. Se descartan los borradores aunque la misma persona vuelva a autenticarse; las operaciones ya enviadas conservan su progreso en Turnos y se consultan después del login.
- Al volver con la misma cuenta, CU-02 recupera el proceso pendiente si existe.

## Fuentes y pendientes

**Fuentes:** [seguridad del enunciado, sección 9](../../../PROJECT_STATEMENT-v1.md); [conservación del proceso en Turnos](../../../backend-turnos/docs/use-cases/CU-10-abandonar-proceso.md); [decisión de sesión](README.md).

**Pendiente para la SPEC:** textos y ubicación de la acción de cierre; los formularios sin enviar se descartan por decisión confirmada.

**Pendiente para el PLAN:** eliminación segura de la sesión conservada y aislamiento de datos y respuestas en curso entre identidades.
