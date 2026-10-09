# CU-01. Registrar usuario

**Estado:** Revisado y aprobado por el usuario el 2026-10-07.

## Identificación

| Campo | Descripción |
| --- | --- |
| Sistema | Aplicación KMP (Android). |
| Objetivo | Crear una cuenta activa de usuario final y acceder con login automático. |
| Actor principal | Persona que se registra. |
| Actor de apoyo | Backend Turnos. |
| Disparador | La persona elige registrarse. |

## Precondiciones

- La persona no tiene una sesión iniciada y dispone de los datos de registro.

## Flujo principal

1. La app presenta el registro con `login`, `password`, `firstName`, `lastName`, `email`, `langKey` e `imageUrl` opcional.
2. La persona completa y envía los datos; la app comunica el envío en curso y evita duplicarlo por pulsaciones repetidas.
3. Turnos valida y crea la cuenta activa, administrando los campos internos y protegiendo la contraseña.
4. El registro incluye [CU-02](CU-02-iniciar-sesion.md) para iniciar sesión sin pedir otra acción a la persona.
5. La app presenta el resultado y accede al recorrido autenticado de CU-02.

## Flujos alternativos y de excepción

- **2a/3a. Datos inválidos o duplicados:** muestra los errores que comunica el contrato; permite corregir y regresar al paso 2.
- **3b. Fallo de registro conocido:** informa el fallo, sin declarar creada una cuenta; termina el intento.
- **3c. Respuesta perdida o desconexión:** informa que el resultado no pudo verificarse; no muestra éxito ni repite el registro indiscriminadamente. La resolución se precisará con el contrato de Turnos.
- **4a. Cuenta creada y login automático fallido:** informa registro completado y permite iniciar sesión por separado, sin pedir otro registro.

## Postcondiciones

- **Éxito:** cuenta activa y acceso autenticado.
- **Alternativa:** error corregible, resultado incierto o cuenta creada sin sesión, claramente diferenciados.

## Reglas y relaciones

- Incluye CU-02 por el login automático aprobado en Turnos. No implica un endpoint combinado.
- No presenta campos para autoridades, activación, auditoría, identidad interna o cuenta técnica de cátedra.
- No requiere verificación por correo. La app no muestra ni registra contraseñas o JWT en mensajes de diagnóstico.
- Aplica la [política mobile confirmada](README.md): sin operaciones offline; formulario temporal conservado durante el proceso Android y descartado al terminarlo o cambiar de sesión.

## Fuentes y pendientes

**Fuentes:** [enunciado, sección 3.2](../../../PROJECT_STATEMENT-v1.md); [CU-01 de Turnos](../../../backend-turnos/docs/use-cases/CU-01-registrar-usuario.md).

**Pendiente para la SPEC:** validaciones del usuario final, selección de `langKey` y presentación de `imageUrl`. No se presupone carga de fotos. Aplicar la conservación temporal acordada sin persistir credenciales para reautenticar.

**Pendiente para el PLAN:** contrato de registro/login y tratamiento de respuesta perdida, acordados con Turnos.
