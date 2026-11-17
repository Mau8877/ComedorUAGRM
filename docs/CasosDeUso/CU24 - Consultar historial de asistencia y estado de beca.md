# Caso de uso

| Id   | Nombre                                              | Módulo                   | Prioridad | Estado      |
| ---- | --------------------------------------------------- | ------------------------ | --------- | ----------- |
| CU24 | Consultar historial de asistencia y estado de beca | P5. Becados y asistencia | Media     | Hecho       |

**Actor principal:** Postulante (becado)
**Actores secundarios:** —
**Objetivo:** Que el becado consulte el estado de su beca y su historial de asistencia al comedor, para verificar el cumplimiento de los requisitos y conservar su beneficio.
**Plataforma:** Móvil y Web
**RF asociados:** RF-19

**Descripción:**
El becado entra a «Mi beca» (en la web o en la app) y el sistema muestra el estado de su beca (vigente, suspendida o dada de baja) junto con la convocatoria, la gestión académica, la fecha de adjudicación y el total de asistencias registradas. Si la beca está suspendida o dada de baja, también muestra el motivo que registró el Administrador ([CU-23](CU23%20-%20Gestionar%20padrón%20de%20becados.md)). Debajo, el historial de asistencia lista las fechas en que el Personal del Comedor registró su asistencia ([CU-10](CU10%20-%20Registrar%20asistencia%20digital.md)), con la hora, y se puede filtrar por rango de fechas.

Es una consulta de **solo lectura**: no modifica ningún dato y cada persona solo ve su propia beca. Si el usuario no figura como becado, el sistema informa que no cuenta con una beca y lo lleva a consultar el estado de su postulación ([CU-04](CU04%20-%20Consultar%20estado%20de%20postulación.md)). Si todavía no hay asistencias, muestra un mensaje informativo.

---

## Precondiciones

- El Postulante está autenticado ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) con el rol `POSTULANTE`.
- Su postulación fue adjudicada y figura en el padrón de becados ([CU-23](CU23%20-%20Gestionar%20padrón%20de%20becados.md)); si no, aplica A1.

## Flujo principal

1. El becado accede a la opción «Mi beca».
2. El sistema verifica que el usuario figure en el padrón de becados (A1).
3. El sistema muestra el estado de la beca, la convocatoria, la gestión académica, la fecha de adjudicación y el total de asistencias; con la beca suspendida o dada de baja, también el motivo (A3).
4. El sistema muestra el historial de asistencia: las fechas registradas, de la más reciente a la más antigua, paginadas (A2).
5. El becado revisa la información y, opcionalmente, filtra el historial por rango de fechas.

## Flujos alternativos y excepciones

- **A1 (paso 2):** El usuario no tiene beca → `404` (`ERR_MBE_01`); la pantalla informa que no cuenta con una beca y ofrece ir a «Mis postulaciones» (CU-04).
- **A2 (paso 4):** Aún no hay asistencias (o ninguna cae en el rango) → el listado viene vacío y la pantalla muestra un mensaje informativo.
- **A3 (paso 3):** La beca está suspendida o dada de baja → se muestra el estado y el motivo; el historial se conserva y sigue siendo consultable.
- **A4 (paso 1):** Intento de ver la información de otro becado → no es posible: la beca es siempre la del usuario del token y ningún endpoint recibe el identificador de otra persona. Un rol distinto de `POSTULANTE` recibe `403` (`ERR_AUTH_07`).
- **A5:** Rango con «desde» posterior a «hasta» → `400` (`ERR_MBE_02`); un `sort` no soportado → `400` (`ERR_MBE_03`).
- **A6:** Falla la conexión o el servidor → la pantalla informa el error y permite reintentar.
- **A7:** Sesión vencida → el cliente intenta renovarla y, si no puede, lleva al inicio de sesión (CU-01).

## Reglas de negocio

- Solo el rol `POSTULANTE` consulta; sin sesión `401`. El usuario sale siempre del token verificado, nunca de la URL ni del cuerpo.
- Si el usuario tiene becas de varias convocatorias se muestra la vigente y, si no hay, la más reciente; el historial es el de esa beca.
- El motivo es el del último movimiento de baja o suspensión de la beca ([CU-23](CU23%20-%20Gestionar%20padrón%20de%20becados.md)); no se muestra si la beca está vigente.
- El historial usa el contrato de listados de [ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md): `page`, `pageSize`, `filter[desde]`, `filter[hasta]` (días de calendario de Bolivia, extremos incluidos) y `sort` solo por `fecha` (por defecto `-fecha`).
- Es de solo lectura: no escribe, no se anota en la bitácora y no hay `POST`, `PUT`, `PATCH` ni `DELETE`.
- El sistema solo guarda las asistencias registradas: **no tiene calendario de días de comedor**, por lo que no calcula «faltas» ni porcentaje de cumplimiento (el mockup los dibuja; ver el registro de diferencias).
- Ninguna respuesta lleva ids internos.

## Postcondiciones

- El becado visualiza el estado de su beca y su historial de asistencia; no cambia ningún dato.

## Criterios de aceptación

- **CA01:** Dado que el usuario tiene una beca, el sistema debe responder `200` con su código, estado, convocatoria, gestión, fecha de adjudicación, fechas de inicio y fin, motivo y total de asistencias.
- **CA02:** Dado que la beca está suspendida o dada de baja, el sistema debe incluir el motivo del último movimiento y seguir devolviendo el historial.
- **CA03:** Dado que el usuario no tiene beca, el sistema debe responder `404` con `ERR_MBE_01`, y la pantalla debe llevarlo a «Mis postulaciones».
- **CA04:** Dado que la beca tiene asistencias, el sistema debe listarlas paginadas con `meta`, de la más reciente a la más antigua por defecto, cada una con su fecha y la hora de registro.
- **CA05:** Dado un rango `filter[desde]`/`filter[hasta]`, el sistema debe devolver solo las asistencias de esos días (extremos incluidos).
- **CA06:** Dado que no hay asistencias, el sistema debe devolver una página vacía con `meta` y la pantalla debe mostrar un mensaje informativo.
- **CA07:** Dado que «desde» es posterior a «hasta», el sistema debe responder `400` con `ERR_MBE_02`; con un `sort` no soportado, `400` con `ERR_MBE_03`.
- **CA08:** Dado que dos postulantes becados consultan, cada uno debe ver solo su beca y sus asistencias.
- **CA09:** Dado que el rol no es `POSTULANTE`, el sistema debe responder `403` con `ERR_AUTH_07`; sin sesión, `401`.
- **CA10:** Dado que se intenta escribir en `/api/v1/mi-beca`, el sistema debe responder `405`.
- **CA11:** Dado que el becado entra a «Mi beca» en la web o en la app, la pantalla debe mostrar el estado, el resumen y el historial con su filtro por fechas.

## Desarrollador

Leonel Barrios
