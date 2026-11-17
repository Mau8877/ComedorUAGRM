# Caso de uso

| Id   | Nombre                                  | Módulo                   | Prioridad | Estado |
| ---- | --------------------------------------- | ------------------------ | --------- | ------ |
| CU11 | Cotejar registro contra respaldo físico | P5. Becados y asistencia | Media     | Hecho  |

**Actor principal:** Administrador DUBSS
**Actores secundarios:** —
**Objetivo:** Que el Administrador DUBSS compare, durante una auditoría, los registros digitales de asistencia con la planilla física firmada y deje constancia del resultado.
**Plataforma:** Web
**RF asociados:** RF-05

**Descripción:**
El Administrador DUBSS entra a «Cotejo de asistencia», elige una convocatoria con resultados publicados y una de sus planillas generadas ([CU-09](CU09%20-%20Generar%20planilla%20de%20asistencia.md)). El sistema muestra cuántos becados tiene la planilla, cuántos **registros digitales** de asistencia ([CU-10](CU10%20-%20Registrar%20asistencia%20digital.md)) hay en su periodo y la lista de esos registros (becado, carrera, fecha y hora). El Administrador **compara a mano** esos registros con las firmas de la planilla física y registra lo que constató: total de registros, coincidencias, discrepancias, resultado (conforme o con discrepancias) y observaciones. El sistema guarda el cotejo con quién lo hizo y la fecha, lo anota en la bitácora y lo agrega al historial de cotejos de la planilla.

El sistema **no lee** la planilla firmada: no hay lectura automática (OCR) ni carga de la planilla escaneada; eso queda para una versión futura. Hoy el total y las coincidencias los declara el Administrador, y el sistema deduce las discrepancias y el resultado y exige observaciones cuando hay discrepancias. Se pueden registrar **varios cotejos** de una misma planilla y se conservan todos. El cotejo **no modifica** ninguna asistencia, ninguna planilla ni ninguna beca, y no avisa a nadie.

---

## Precondiciones

- El Administrador DUBSS está autenticado ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) con el rol `ADMINISTRADOR_DUBSS` y su cuenta está activa.
- Existe una planilla generada para el periodo a cotejar ([CU-09](CU09%20-%20Generar%20planilla%20de%20asistencia.md)).
- El Administrador dispone de la planilla física firmada correspondiente.

## Flujo principal

1. El Administrador entra a «Cotejo de asistencia». El sistema muestra las convocatorias con resultados publicados y las planillas de la primera, con la más reciente elegida (A4, A5).
2. El Administrador elige la convocatoria y la planilla a cotejar.
3. El sistema obtiene la planilla y los registros digitales de asistencia de sus becados en su periodo, y muestra los totales y la lista paginada (A1, A6).
4. El Administrador compara los registros digitales con las firmas de la planilla física.
5. El Administrador ingresa el total de registros comparados y cuántos coinciden; el sistema deduce las discrepancias y el resultado (A2).
6. El Administrador agrega las observaciones y pulsa «Guardar cotejo».
7. El sistema valida lo ingresado y registra el cotejo con el usuario y la fecha de hoy, y lo anota en la bitácora, en una misma operación (A7).
8. El sistema confirma el registro, limpia el formulario y agrega el cotejo al historial (A3).

## Flujos alternativos y excepciones

- **A1 (paso 3):** No hay registros digitales en el periodo de la planilla → el sistema lo informa y permite registrar el cotejo igual (un cotejo con todas las firmas «sin registro digital» es justamente una discrepancia).
- **A2 (paso 5):** El resultado es «con discrepancias» → el sistema exige observaciones y no deja guardar sin ellas.
- **A3 (paso 8):** La planilla ya fue cotejada → el sistema registra el nuevo cotejo y conserva los anteriores.
- **A4 (paso 1):** Ninguna convocatoria tiene resultados publicados → el sistema lo informa y no muestra el formulario.
- **A5 (paso 1):** La convocatoria no tiene planillas → el sistema lo informa y apunta a «Planillas» (CU-09).
- **A6 (pasos 2 y 3):** La planilla no existe o fue eliminada → `404` (`ERR_COT_01`); se vuelve a la selección de planilla.
- **A7 (paso 7):** Datos inválidos → `400`: las coincidencias y las discrepancias no suman el total (`ERR_COT_02`), el resultado no corresponde a las discrepancias (`ERR_COT_03`), o hay discrepancias sin observaciones (`ERR_COT_04`); campos vacíos, negativos o fuera de rango → `400` (`ERR_SYS_01`).
- **A8:** Falla la conexión o el servidor → el sistema informa el error, no registra nada y permite reintentar.

## Reglas de negocio

- Solo el rol `ADMINISTRADOR_DUBSS`; cualquier otro recibe `403` (`ERR_AUTH_07`) y sin sesión `401`.
- **Registros digitales de una planilla:** las asistencias del periodo de la planilla de las becas de su convocatoria que ya existían cuando se generó. Se usa esa aproximación porque la planilla no guarda la lista de becados que incluyó (ver el follow-up).
- **Consistencia:** `coincidencias + discrepancias = total de registros`; `CONFORME` exige 0 discrepancias y `CON_DISCREPANCIAS` al menos una, con observaciones no vacías. La base lo exige también con restricciones.
- El **resultado se deduce** de las discrepancias en la pantalla; el backend solo valida lo que llega. No hay lectura automática de la planilla firmada.
- Un cotejo es un registro de lo constatado: **no se edita ni se elimina** (`PUT`, `PATCH` y `DELETE` responden `405`). Se pueden registrar varios por planilla.
- La fecha del cotejo es la de hoy (hora de Bolivia, `America/La_Paz`).
- La bitácora (`COTEJO_ASISTENCIA`) lleva códigos, cantidades y resultado, y **nunca** las observaciones, nombres ni CI ([AUDITORIA_BITACORA_BACKEND.md](../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md)). La lista de registros digitales no incluye el CI.
- El cotejo **no** modifica asistencias, planillas ni becas, y **no** notifica a nadie.

## Postcondiciones

- El cotejo queda registrado con sus cantidades, resultado, observaciones, responsable y fecha, y anotado en la bitácora.
- Los registros de asistencia digital no se modifican.

## Criterios de aceptación

- **CA01:** Dado que la planilla existe, el sistema debe responder `200` con su periodo, los becados que incluyó, el total de registros digitales de su periodo y de cuántos becados, contando solo las asistencias del periodo de las becas de su convocatoria que ya existían al generarla.
- **CA02:** Dado que la planilla no tiene registros digitales, el sistema debe responder `200` con 0 y la pantalla debe informarlo sin impedir el cotejo.
- **CA03:** Dado que se pide la lista de registros digitales, el sistema debe devolverla paginada (`data` + `meta`), por fecha y apellidos por defecto, ordenable por `fecha` y `apellidos`, sin CI ni ids internos; un `sort` o `filter` no soportado debe dar `400` con `ERR_SYS_05`.
- **CA04:** Dado un cotejo consistente, el sistema debe responder `201`, guardarlo con el usuario, la fecha de hoy, las cantidades, el resultado y las observaciones sin espacios sobrantes, y devolverlo.
- **CA05:** Dado que las coincidencias y las discrepancias no suman el total, el sistema debe responder `400` con `ERR_COT_02` y no guardar nada.
- **CA06:** Dado que el resultado no corresponde a las discrepancias, el sistema debe responder `400` con `ERR_COT_03` y no guardar nada.
- **CA07:** Dado que hay discrepancias y las observaciones faltan o solo tienen espacios, el sistema debe responder `400` con `ERR_COT_04` y no guardar nada.
- **CA08:** Dado que se registran varios cotejos de la misma planilla, el sistema debe conservarlos todos y listarlos paginados, el más reciente primero, ordenables por `creadoEn` y `fechaCotejo`.
- **CA09:** Dado que se registra un cotejo, la bitácora debe llevar códigos, cantidades y resultado, y nunca las observaciones ni datos personales; y las asistencias, la planilla y las becas no deben cambiar.
- **CA10:** Dado que la planilla no existe, el sistema debe responder `404` con `ERR_COT_01` en todos los endpoints.
- **CA11:** Dado que quien llama no tiene el rol `ADMINISTRADOR_DUBSS`, el sistema debe responder `403` con `ERR_AUTH_07` en todos los endpoints; sin sesión, `401`.
- **CA12:** Dado que se intenta editar o eliminar un cotejo, el sistema debe responder `405`.
- **CA13:** Dado que se ingresan el total y las coincidencias, la pantalla debe mostrar las discrepancias y el resultado deducidos, y marcar las observaciones como obligatorias si hay discrepancias.
- **CA14:** Dado que el formulario es inválido (vacío, no entero, coincidencias mayores que el total, o discrepancias sin observaciones), la pantalla debe indicarlo junto al campo y no llamar al servidor.
- **CA15:** Dado que el registro tiene éxito, la pantalla debe confirmarlo, limpiar el formulario y agregar el cotejo al historial.

## Desarrollador

Leonel Barrios
