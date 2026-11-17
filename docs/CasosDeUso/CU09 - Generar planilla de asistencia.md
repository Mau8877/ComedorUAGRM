# Caso de uso

| Id   | Nombre                           | Módulo                       | Prioridad | Estado |
| ---- | -------------------------------- | ---------------------------- | --------- | ------ |
| CU09 | Generar planilla de asistencia   | P5. Becados y asistencia     | Media     | Hecho  |

**Actor principal:** Administrador DUBSS
**Actores secundarios:** —
**Objetivo:** Que el Administrador DUBSS genere, para un periodo, la planilla imprimible del padrón de becados vigentes (con espacio para la firma y la fotocopia del carnet de cada uno), la guarde como respaldo físico del uso del beneficio ante las auditorías y pueda consultarla y descargarla después.
**Plataforma:** Web
**RF asociados:** RF-05

**Descripción:**
El Administrador DUBSS entra a «Planillas», elige una convocatoria con resultados publicados e ingresa el **periodo** (fecha desde y fecha hasta). El sistema le muestra cuántos becados **vigentes** entrarán en la planilla ([CU-23](CU23%20-%20Gestionar%20padrón%20de%20becados.md)) y, si quiere, una **vista previa** del documento. Al pulsar «Generar planilla», el sistema valida el periodo (que la fecha final no sea anterior a la inicial, que esté dentro de la gestión académica de la convocatoria y que quepa en la hoja), toma los becados vigentes de ese momento, genera el **PDF apaisado** (apellidos y nombres, CI, carrera, un recuadro para la fotocopia del carnet y una columna de firma por cada día del periodo), lo guarda en MinIO, registra la planilla (periodo, cantidad de becados, ruta del archivo, quién y cuándo) y la anota en la bitácora, en una misma operación. Después el Administrador puede **consultar** las planillas generadas de la convocatoria y **descargar** cualquiera de ellas; cada descarga también queda en la bitácora.

La planilla refleja el padrón **tal como estaba al generarla**: los cambios posteriores (bajas, suspensiones, reemplazos del [CU-23](CU23%20-%20Gestionar%20padrón%20de%20becados.md)) aparecen solo en las planillas que se generen después. Se pueden generar **varias planillas** de una misma convocatoria (una por periodo, o una nueva tras cambios del padrón); cada una es un registro de lo que se imprimió y no se edita ni se elimina. La planilla queda disponible para el cotejo con el registro digital de asistencia (CU-11, todavía sin implementar). Este CU **no cambia** el estado de ninguna beca, **no avisa** a nadie y **no registra** asistencia (CU-10).

---

## Precondiciones

- El Administrador DUBSS está autenticado ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) con el rol `ADMINISTRADOR_DUBSS` y su cuenta está activa.
- La convocatoria existe y tiene un **padrón de becados** ([CU-23](CU23%20-%20Gestionar%20padrón%20de%20becados.md)). Para **generar** hace falta al menos una beca **vigente**; para **consultar o descargar** basta con que ya haya planillas.
- **Datos de apoyo:** las becas vigentes salen del alta del padrón (CU-23). La pantalla ofrece solo las convocatorias con resultados publicados, que son las que tienen padrón.

## Flujo principal

### A. Generar una planilla

1. El Administrador DUBSS entra a «Planillas». El sistema muestra las convocatorias con resultados publicados, la primera ya elegida, junto con la tabla de sus planillas generadas (paginada) (A1).
2. El sistema muestra cuántos becados vigentes entrarían en la planilla y la gestión académica a la que debe ajustarse el periodo (A2).
3. El Administrador elige la convocatoria (si quiere otra) e ingresa el periodo: fecha desde y fecha hasta.
4. Si lo desea, pulsa «Vista previa»: el sistema muestra el documento tal como quedaría, marcado «BORRADOR», sin guardarlo ni anotarlo en la bitácora (A3, A4, A5).
5. El Administrador pulsa «Generar planilla».
6. El sistema valida que la convocatoria exista (A6) y que el periodo sea válido: fecha final no anterior a la inicial (A3), dentro de la gestión académica (A4) y de a lo más 31 días (A5).
7. El sistema obtiene los becados con beca **vigente** de la convocatoria (nombre y apellido, CI y carrera, ordenados por apellido y nombre) (A2).
8. El sistema genera el PDF y lo guarda en MinIO (A7).
9. El sistema registra la planilla (convocatoria, periodo, cantidad de becados, ruta del archivo, quién y cuándo) y la anota en la bitácora, en una misma operación.
10. El sistema muestra la planilla generada, la agrega a la tabla y permite descargarla e imprimirla.

### B. Consultar y descargar planillas

1. El Administrador DUBSS entra a «Planillas» y ve la tabla de planillas de la convocatoria elegida (periodo, becados, fecha de generación, quién la generó), la más reciente primero.
2. Puede ordenar por periodo o por fecha de generación y paginar.
3. Pulsa «Descargar» en la planilla que quiere.
4. El sistema obtiene la planilla registrada (A8) y comprueba que el archivo esté disponible (A9).
5. El sistema anota la descarga en la bitácora y entrega una dirección temporal y firmada para abrir el PDF (vigente 5 minutos).
6. El navegador descarga o abre el PDF para imprimirlo.

## Flujos alternativos y excepciones

- **A1 (paso 1):** Ninguna convocatoria tiene resultados publicados → el sistema lo informa («Todavía no hay convocatorias con resultados publicados») y no muestra el formulario.
- **A2 (pasos 2 y 7):** La convocatoria **no tiene becas vigentes** → el sistema lo informa, bloquea «Generar planilla» y «Vista previa»; si igual llega la petición, responde `409` (`ERR_PLA_04`) y no genera nada.
- **A3 (pasos 4 y 6):** La fecha final es **anterior** a la inicial → `400` (`ERR_PLA_01`); el sistema lo indica junto al campo y permite corregirlo. Faltan fechas → el sistema pide ambas y no consulta al servidor.
- **A4 (pasos 4 y 6):** El periodo **no está comprendido en la gestión académica** de la convocatoria → `400` (`ERR_PLA_02`).
- **A5 (pasos 4 y 6):** El periodo **supera los 31 días** (las columnas de día que caben en la hoja) → `400` (`ERR_PLA_03`); se sugiere generar una planilla por mes.
- **A6 (pasos 4 y 6):** La convocatoria no existe o fue eliminada → `404` (`ERR_PRI_01`); se vuelve a la selección de convocatoria.
- **A7 (paso 8):** Falla la generación del PDF o el guardado en MinIO → el sistema informa un error claro (`500` genérico, `ERR_SYS_00`), **no registra** la planilla ni la bitácora y permite reintentar.
- **A8 (flujo B, paso 4):** La planilla no existe o fue eliminada → `404` (`ERR_PLA_05`).
- **A9 (flujo B, paso 4):** La planilla está registrada pero su archivo no está en MinIO → `404` (`ERR_PLA_06`); el sistema avisa que se contacte a soporte y **no** anota la descarga.
- **A10 (pasos 5 y 6):** Cambia el padrón entre la vista previa y la generación (otro administrador da una baja) → la planilla generada refleja el padrón del momento de generar y su cantidad de becados lo dice.

## Reglas de negocio

- Solo entran las becas en estado **vigente** (`VIGENTE`); las suspendidas y las dadas de baja no ([CU-23](CU23%20-%20Gestionar%20padrón%20de%20becados.md)).
- El periodo incluye ambos extremos y **no puede salirse de la gestión académica** de la convocatoria. Puede ser de un solo día.
- Una planilla admite **hasta 31 días** de periodo: la hoja dibuja una columna de firma por día. ⚠️ Es un límite de diseño del formato, **por definir con la DUBSS** junto con el encabezado, los rótulos y los días hábiles.
- Se pueden generar **varias planillas** de la misma convocatoria y periodo; no hay unicidad. Cada una guarda cuántos becados incluyó.
- La planilla es un registro de lo impreso: **no se edita ni se elimina** (`PUT`, `PATCH` y `DELETE` responden `405`).
- El PDF lleva el CI de los becados: se entrega solo con una dirección temporal y firmada de MinIO, y cada descarga queda en la bitácora. Ni la bitácora, ni los logs ni las respuestas llevan nombres, CI, la ruta del archivo ni la dirección firmada ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md#nunca-loguear), [AUDITORIA_BITACORA_BACKEND.md](../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md)).
- Generar es una operación atómica: el archivo se sube antes de guardar la fila y si algo falla no queda registrada ninguna planilla. Si la base falla después de subir, el archivo queda huérfano pero nadie lo enlaza.
- La vista previa es el mismo documento marcado «BORRADOR»: no sube, no guarda y no se anota en la bitácora.
- Solo el rol `ADMINISTRADOR_DUBSS`; cualquier otro recibe `403` (`ERR_AUTH_07`) y sin sesión `401`.
- La planilla **no** cambia el estado de ninguna beca ni de la convocatoria y **no** notifica a nadie.

## Postcondiciones

- La planilla queda almacenada en MinIO y registrada con su periodo, cantidad de becados, responsable y fecha de generación.
- Queda anotada en la bitácora (`GENERACION_PLANILLA`) y cada descarga también (`DESCARGA_PLANILLA`).
- Queda disponible para su impresión y para el cotejo con el registro digital (CU-11).

## Criterios de aceptación

- **CA01:** Dado que la convocatoria tiene becas vigentes y el periodo es válido, el sistema debe responder `201`, guardar el PDF en MinIO, registrar la planilla con su periodo, la cantidad de becados, quién la generó y cuándo, y devolverla sin ruta de archivo.
- **CA02:** Dado que se genera una planilla, el PDF debe ser una hoja apaisada con el título, la convocatoria, la gestión, el periodo, la cantidad de becados, y por cada becado vigente su apellido y nombre, CI, carrera, un recuadro de fotocopia de carnet y una columna de firma por cada día del periodo (ninguna de más).
- **CA03:** Dado que la fecha final es anterior a la inicial, el sistema debe responder `400` con `ERR_PLA_01` y no generar nada.
- **CA04:** Dado que el periodo empieza antes o termina después de la gestión académica de la convocatoria, el sistema debe responder `400` con `ERR_PLA_02` y no generar nada.
- **CA05:** Dado que el periodo supera los 31 días, el sistema debe responder `400` con `ERR_PLA_03`; con exactamente 31 días debe generar.
- **CA06:** Dado que la convocatoria no tiene becas vigentes, el sistema debe responder `409` con `ERR_PLA_04` y no generar nada.
- **CA07:** Dado que hay becas vigentes, suspendidas y dadas de baja, el sistema debe incluir en la planilla solo las vigentes, ordenadas por apellido y nombre; y una planilla generada después de una baja no debe incluir a quien se dio de baja.
- **CA08:** Dado que falla la generación del PDF o el guardado en MinIO, el sistema debe responder `500` con `ERR_SYS_00` y no registrar la planilla ni la bitácora.
- **CA09:** Dado que la convocatoria tiene planillas, el sistema debe listarlas paginadas (`data` + `meta`), la más reciente primero, ordenables por `generadaEn` y `fechaDesde`; un `sort` o `filter` no soportado debe dar `400` con `ERR_SYS_05`.
- **CA10:** Dado que se descarga una planilla existente, el sistema debe devolver una dirección temporal y firmada (5 minutos) sin la ruta del archivo y anotar `DESCARGA_PLANILLA`; si la planilla no existe debe responder `404` con `ERR_PLA_05`; si su archivo falta, `404` con `ERR_PLA_06` sin anotar la descarga.
- **CA11:** Dado que se pide la vista previa con un periodo válido, el sistema debe devolver el PDF marcado «BORRADOR» sin subir nada, sin guardar ninguna fila y sin anotar la bitácora; con un periodo inválido o sin becas debe dar el mismo error que generar.
- **CA12:** Dado que quien llama no tiene el rol `ADMINISTRADOR_DUBSS`, el sistema debe responder `403` con `ERR_AUTH_07` en todos los endpoints; sin sesión, `401`.
- **CA13:** Dado que se genera o descarga una planilla, la bitácora debe llevar códigos, periodo y cantidad de becados, y nunca nombres, CI, la ruta del archivo ni la dirección firmada.
- **CA14:** Dado que la convocatoria no existe, el sistema debe responder `404` con `ERR_PRI_01`.
- **CA15:** Dado que se intenta editar o eliminar una planilla, el sistema debe responder `405`.
- **CA16:** Dado que se elige una convocatoria, la pantalla debe mostrar cuántos becados vigentes entrarán y los límites de su gestión; sin becas vigentes debe bloquear «Generar planilla» y «Vista previa».
- **CA17:** Dado que el periodo es inválido en la pantalla (sin fechas, final anterior a la inicial, fuera de la gestión o de más de 31 días), el sistema debe indicarlo junto al campo y no llamar al servidor.

## Desarrollador

Leonel Barrios
