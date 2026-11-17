# Caso de uso

| Id   | Nombre                      | Módulo                   | Prioridad | Estado      |
| ---- | --------------------------- | ------------------------ | --------- | ----------- |
| CU10 | Registrar asistencia digital | P5. Becados y asistencia | Media     | Hecho       |

**Actor principal:** Personal del Comedor
**Actores secundarios:** —
**Objetivo:** Que el Personal del Comedor registre desde la app móvil la asistencia diaria de un becado, ingresando su CI y confirmando, sin duplicar el registro si se repite.
**Plataforma:** Móvil
**RF asociados:** RF-05

**Descripción:**
El Personal del Comedor entra a «Registrar asistencia» en la app, ingresa el CI del becado y pulsa «Buscar». El sistema busca la beca asociada a ese CI y muestra el nombre, CI, carrera y estado de la beca ([CU-23](CU23%20-%20Gestionar%20padrón%20de%20becados.md)). Si la beca está vigente, el personal confirma con «Registrar asistencia» y el sistema guarda la asistencia de **hoy** con la hora y el usuario que la registra. El sistema no tiene biometría: la identidad la verifica el comedor por sus medios y el sistema solo guarda el resultado.

Hay **una sola asistencia por beca y día** (RNF-03): si ya estaba registrada, el sistema no crea otra y avisa la hora del registro existente. Si el CI no figura en el padrón, o la beca está suspendida o dada de baja, no se registra y se informa el motivo. El registro queda disponible para el historial del becado (CU-24), el cotejo con la planilla física ([CU-09](CU09%20-%20Generar%20planilla%20de%20asistencia.md), CU-11) y los indicadores (CU-12).

---

## Precondiciones

- El Personal del Comedor está autenticado en la app ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) con el rol `PERSONAL_COMEDOR` y su cuenta está activa.
- Existe un padrón de becados ([CU-23](CU23%20-%20Gestionar%20padrón%20de%20becados.md)).
- El personal verificó la identidad del becado por los medios del comedor.

## Flujo principal

1. El Personal del Comedor entra a «Registrar asistencia» (al iniciar sesión, la app lo lleva ahí).
2. El sistema muestra la fecha de hoy y pide el CI del becado.
3. El Personal ingresa el CI y pulsa «Buscar».
4. El sistema busca la beca del CI y obtiene su estado (A1, A2).
5. El sistema muestra nombre, CI, carrera, convocatoria y estado de la beca.
6. El Personal confirma con «Registrar asistencia».
7. El sistema comprueba que no haya asistencia de esa beca hoy (A3).
8. El sistema registra la asistencia con la fecha de hoy, la hora y el usuario.
9. El sistema confirma el registro y deja la pantalla lista para el siguiente becado.

## Flujos alternativos y excepciones

- **A1 (paso 4):** El CI no corresponde a ningún becado → `404` (`ERR_ASI_01`): «El estudiante no figura en el padrón de becados».
- **A2 (pasos 4 y 6):** La beca está suspendida o dada de baja → la app muestra el estado y no permite registrar; si igual llega la petición, `409` (`ERR_ASI_02`).
- **A3 (paso 7):** Ya hay asistencia de hoy → no se crea otra; el sistema responde `200` con `yaRegistrada = true` y la hora del primer registro, y la app muestra «Asistencia ya registrada».
- **A4 (paso 6):** La beca ya no existe (eliminada entre la búsqueda y la confirmación) → `404` (`ERR_ASI_03`).
- **A5 (pasos 4 y 8):** Falla la conexión o el servidor → la app informa el error y permite reintentar; no se registra nada hasta que la petición tenga éxito, y reintentar no duplica (A3).
- **A6:** Sesión vencida → la app intenta renovarla y, si no puede, lleva al inicio de sesión (CU-01). Otro rol → `403` (`ERR_AUTH_07`).
- **A7 (CU-11):** En una auditoría, el Administrador puede cotejar los registros contra la planilla física (CU-11, aún sin implementar).

## Reglas de negocio

- Solo el rol `PERSONAL_COMEDOR` busca y registra; cualquier otro recibe `403` (`ERR_AUTH_07`) y sin sesión `401`.
- Solo una beca `VIGENTE` registra asistencia. La búsqueda sí muestra becas suspendidas o dadas de baja, para explicar por qué no se registra.
- Si el CI tiene becas de varias convocatorias se usa la vigente y, si no hay, la más reciente.
- **Una asistencia por beca y día** (índice único parcial `beca_id, fecha`): repetir la petición, o dos a la vez, no duplica.
- La **fecha la fija el servidor** (hora de Bolivia, `America/La_Paz`): el cliente no puede registrar otro día.
- Una asistencia no se edita ni se elimina (`PUT`, `PATCH` y `DELETE` responden `405`).
- El CI viaja en el cuerpo de un `POST` (no en la URL) y nunca se escribe en logs ni en errores ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md#nunca-loguear)).
- No se anota en la bitácora: la fila ya guarda quién y cuándo, y se generaría un evento por comida de cada becado. Registrar no cambia el estado de la beca ni notifica a nadie.

## Postcondiciones

- La asistencia del becado queda registrada una sola vez para la fecha, con la hora y el usuario.
- Queda disponible para CU-24, CU-11 y CU-12.

## Criterios de aceptación

- **CA01:** Dado que el CI corresponde a un becado, el sistema debe responder `200` con nombre, CI, carrera, convocatoria, estado de la beca, la fecha de hoy y la hora si ya registró hoy.
- **CA02:** Dado que el CI no corresponde a ningún becado, el sistema debe responder `404` con `ERR_ASI_01`.
- **CA03:** Dado que la beca está vigente y no hay asistencia hoy, el sistema debe responder `201`, guardar la asistencia con la fecha de hoy, la hora y el usuario, y devolver `yaRegistrada = false`.
- **CA04:** Dado que ya hay asistencia de hoy, el sistema debe responder `200` con `yaRegistrada = true` y la hora del primer registro, sin crear otra fila.
- **CA05:** Dado que dos peticiones simultáneas registran la misma beca el mismo día, el sistema debe dejar una sola fila.
- **CA06:** Dado que la beca está suspendida o dada de baja, el sistema debe responder `409` con `ERR_ASI_02` y no registrar.
- **CA07:** Dado que la beca no existe, el sistema debe responder `404` con `ERR_ASI_03`.
- **CA08:** Dado que el rol no es `PERSONAL_COMEDOR`, el sistema debe responder `403` con `ERR_AUTH_07`; sin sesión, `401`.
- **CA09:** Dado que el CI viene vacío o supera 20 caracteres, el sistema debe responder `400` con `ERR_SYS_01`.
- **CA10:** Dado que se intenta editar o eliminar una asistencia, el sistema debe responder `405`.
- **CA11:** Dado que entra un usuario `PERSONAL_COMEDOR`, la app debe llevarlo a «Registrar asistencia».
- **CA12:** Dado que la beca encontrada no está vigente o ya registró hoy, la app debe deshabilitar «Registrar asistencia» y mostrar el motivo.
- **CA13:** Dado que el registro tiene éxito, la app debe confirmarlo con la hora y permitir pasar al siguiente becado.

## Desarrollador

Leonel Barrios
