# Unidades de trabajo — CU12-BE

**Caso de uso:** Backend del dashboard de indicadores BI del Directivo: una vista precalculada por lote (postulaciones recibidas, finalistas, becas adjudicadas y asistencia) más un análisis BI (embudo, facultades, convocatorias, puntajes, cobertura, asistencia y tendencia), que se consultan por gestión académica y convocatoria, con descarga en PDF y Excel.
**Fuente CU:** [`CU12 - Visualizar dashboard de indicadores BI`](../../../CasosDeUso/CU12%20-%20Visualizar%20dashboard%20de%20indicadores%20BI.md)
**Diagramas de grado:** CU-12 en la documentación de grado (contexto `071`, análisis `084`, secuencia `099` y maqueta `112`). Diseño lógico: `diseno_base_de_datos.md` §BI (vistas materializadas).
**Recurso HTTP / contrato:** ver la tabla de abajo.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_BACKEND.md`, `CONVENCIONES_JAVA_BACKEND.md`, `PERSISTENCIA_BD_BACKEND.md`, `SEGURIDAD_AUTH_BACKEND.md`, `EXCEPCIONES_BACKEND.md`, `GUIA_ERRORES_BACKEND.md`, `RESPONSES_BACKEND.md`, `ENDPOINTS_BACKEND.md`, `LOGGING_BACKEND.md` y `TESTING_BACKEND.md`. Estas fichas no las sustituyen: si hay conflicto, ganan las rules.

## Decisiones de diseño

`B/` = `projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend/`, `T/` = `.../src/test/java/com/comedoruagrm/backend/`, `R/` = `.../src/main/resources/`.

Decisiones confirmadas por Mau en el CU12; las marcadas *(propuesta)* las fijó quien redactó estas UT y se revisan si hace falta.

- **Sin tablas nuevas:** una sola **vista materializada** `mv_indicadores_convocatoria` con una fila por `(convocatoria, mes)`. Se crea con una migración; no se toca ninguna tabla de otro CU.
- **Módulo nuevo `features/dashboard/`** (paquete P6), dueño de la vista. Lee `postulacion`, `finalista`, `adjudicacion`, `publicacion_resultado`, `convocatoria` y `gestion_academica` solo **dentro de la definición SQL de la vista** y con consultas JDBC propias para los catálogos; ningún módulo lee el repository del otro.
- **Qué cuenta cada columna de la vista:**
  - `postulaciones_recibidas`: postulaciones con `fecha_envio` no nula (los borradores nunca tienen `fecha_envio`), en el mes de su envío.
  - `finalistas`: filas de `finalista`, en el mes de su creación.
  - `adjudicadas`: adjudicaciones con resultado `ADJUDICADA` **solo de convocatorias con fila en `publicacion_resultado`**, en el mes de su `fecha_adjudicacion`.
  - `calculado_en`: `now()` del momento del refresco; la «fecha de la última actualización» es el máximo de esa columna.
  - Los meses se calculan en la zona horaria `America/La_Paz` *(propuesta)*.
- **Asistencia:** las UT 01–09 se hicieron antes del CU-10 (asistencia en `null`). Con el CU-10 hecho, las UT 10–13 la completan con el registro real: tasa = asistieron / esperados (becas vigentes por día de servicio con registro) y beca en riesgo = ≥ 5 días de servicio y < 60 %. ⚠️ Criterios adoptados, **a validar con la DUBSS**.
- **Análisis BI (UT 10–13):** cuatro vistas materializadas más (`mv_bi_*`), un `BiService` con su endpoint, un Excel con Apache POI y un resumen en el PDF. Decidido con Mau; la tendencia compara gestiones ya registradas, no proyecta.
- **Refresco por lote:** `REFRESH MATERIALIZED VIEW CONCURRENTLY` (por eso la vista lleva un índice único) desde una tarea programada **cada noche a las 02:00** (`app.dashboard.refresco-cron`, con ese valor por defecto, zona `America/La_Paz`) y **también al arrancar si la vista está vacía**. No hay botón de «actualizar ahora». Hoy el proyecto no tiene `@EnableScheduling`: lo agrega la UT-02.
- **Una instancia:** el refresco corre en cada instancia del backend; con una sola (el caso actual) no hay problema, y `CONCURRENTLY` tolera que dos coincidan. Si algún día hay réplicas se coordina con un bloqueo (follow-up).
- **Gestión por defecto:** la gestión vigente (`fecha_inicio <= hoy <= fecha_fin`); si no hay ninguna vigente, la de `fecha_inicio` más reciente; si no existen gestiones, la respuesta viene vacía (A1), no es un error.
- **Filtros:** `filter[gestion]` y `filter[convocatoria]` (códigos UUID). Con solo `filter[convocatoria]` la gestión es la de esa convocatoria. Una gestión o convocatoria inexistente, o una convocatoria que no es de la gestión, es `404` `ERR_DAS_01`. Un valor que no es UUID es `400` `ERR_SYS_05`.
- **Series sin huecos:** `porMes` trae todos los meses entre el primero y el último con datos (con ceros en los vacíos) para que el gráfico quede continuo.
- **Solo lectura y sin bitácora:** ni la consulta ni el PDF se anotan en la bitácora (CA09): son agregados sin datos personales.
- **PDF con Jasper:** misma dependencia y mismo patrón que el CU-06 (`ListaPriorizadaPdfService`); plantilla `indicadores_dashboard.jrxml`. Sin datos → `400` `ERR_DAS_02`.
- **Dos códigos de error propios**, prefijo `DAS`: `ERR_DAS_01` (`404`) y `ERR_DAS_02` (`400`). Se documentan en `docs/errors/` y su índice.
- **Rol:** `@PreAuthorize("hasRole('DIRECTIVO')")` en cada método público del `service` (catálogo, indicadores y PDF). El Administrador DUBSS no accede.
- **Catálogo de filtros** como un único objeto (`data` objeto, no colección): las gestiones son pocas y cada una trae sus convocatorias; no se pagina.
- **Rendimiento:** la consulta lee solo la vista (CA13); nunca las tablas transaccionales.
- **Datos de ejemplo:** UT-08, solo con perfil `dev`.

## Contrato de datos / recurso HTTP

Todos exigen sesión y rol `DIRECTIVO`.

| Método | Ruta | Query | Respuesta | Errores |
|--------|------|-------|-----------|---------|
| `GET` | `/api/v1/dashboard/filtros` | — | `200` `FiltrosDashboardResponse` | — |
| `GET` | `/api/v1/dashboard/indicadores` | `filter[gestion]`, `filter[convocatoria]` | `200` `IndicadoresResponse` | `404` `ERR_DAS_01`; `400` `ERR_SYS_05` |
| `GET` | `/api/v1/dashboard/indicadores/pdf` | los mismos | `200` `application/pdf` (adjunto) | `404` `ERR_DAS_01`; `400` `ERR_DAS_02`, `ERR_SYS_05` |

- `FiltrosDashboardResponse` = `gestiones[]`: `codigo`, `etiqueta`, `vigente` (booleano) y `convocatorias[]` (`codigo`, `nombre`). Orden: gestiones por `-fechaInicio`; convocatorias por `-fechaApertura`.
- `IndicadoresResponse` = `gestion` (`codigo`, `etiqueta`; `null` si no hay gestiones), `convocatoria` (`codigo`, `nombre`; `null` = «Todas»), `totales` (`postulacionesRecibidas`, `finalistas`, `becasAdjudicadas`, `asistenciaPromedio` = `null`), `porMes[]` (`mes` como `yyyy-MM`, `postulaciones`, `finalistas`, `adjudicadas`, `asistencia` = `null`), `actualizadoEn` (`null` si el lote nunca se ejecutó) y `sinDatos` (verdadero si todos los totales son cero).
- El PDF se entrega con `Content-Disposition: attachment; filename="indicadores-{gestion}-{yyyyMMdd}.pdf"`; es el único endpoint sin sobre `ApiResponse` (igual que la exportación del CU-06) y los errores sí usan el sobre.

Todos los identificadores son códigos públicos (UUID).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU12-BE-01](./UT-CU12-BE-01.md) | Migración: vista materializada `mv_indicadores_convocatoria` | — |
| [UT-CU12-BE-02](./UT-CU12-BE-02.md) | Refresco por lote de la vista | 01 |
| [UT-CU12-BE-03](./UT-CU12-BE-03.md) | Códigos de error y DTOs | — |
| [UT-CU12-BE-04](./UT-CU12-BE-04.md) | Catálogo de filtros (gestiones y convocatorias) | 03 |
| [UT-CU12-BE-05](./UT-CU12-BE-05.md) | Consulta de indicadores | 01, 03, 04 |
| [UT-CU12-BE-06](./UT-CU12-BE-06.md) | Descarga del dashboard en PDF con Jasper | 05 |
| [UT-CU12-BE-07](./UT-CU12-BE-07.md) | Endpoints del Directivo | 04, 05, 06 |
| [UT-CU12-BE-08](./UT-CU12-BE-08.md) | Datos de prueba del dashboard | 01, 02 |
| [UT-CU12-BE-09](./UT-CU12-BE-09.md) | Pruebas de integración con base real | 02, 07, 08 |
| [UT-CU12-BE-10](./UT-CU12-BE-10.md) | Migración: vistas materializadas del análisis BI | 01, 02 |
| [UT-CU12-BE-11](./UT-CU12-BE-11.md) | Consulta del análisis BI | 05, 10 |
| [UT-CU12-BE-12](./UT-CU12-BE-12.md) | Excel del análisis, PDF ampliado y endpoints | 06, 07, 11 |
| [UT-CU12-BE-13](./UT-CU12-BE-13.md) | Datos de prueba y pruebas de integración del análisis BI | 08, 09, 12 |

**Cadena:** `01 → 02, 05`; `03` independiente; `03 → 04`; `01, 03, 04 → 05`; `05 → 06`; `04, 05, 06 → 07`; `01, 02 → 08`; `02, 07, 08 → 09`; `01, 02 → 10`; `05, 10 → 11`; `06, 07, 11 → 12`; `08, 09, 12 → 13`.

## Criterios de aceptación cubiertos

| CA (CU12) | Descripción | UTs |
|---|---|---|
| CA01 | Indicadores de la gestión vigente con fecha de actualización | 01, 05, 09 |
| CA02 | Filtrar por gestión | 04, 05, 07, 09 |
| CA03 | Filtrar por convocatoria | 04, 05, 07, 09 |
| CA04 | Sin datos: mensaje y ceros, sin error | 05, 09 |
| CA05 | Lote sin ejecutar: últimos datos o aviso | 02, 05, 09 |
| CA06 | Los borradores no cuentan | 01, 09 |
| CA07 | Solo adjudicaciones de resultados publicados | 01, 09 |
| CA08 | Asistencia «Sin datos» o real | 05, 09, 11 |
| CA09 | Solo lectura, sin bitácora | 05, 06, 09 |
| CA10 | `403` / `401` / `404` | 04, 05, 06, 07, 09 |
| CA11 | PDF con indicadores, fecha de actualización y quién lo generó | 06, 07, 09 |
| CA12 | Sin datos no hay PDF (`ERR_DAS_02`) | 06, 09 |
| CA14–CA16 | Embudo, facultades y comparación de convocatorias | 10, 11, 13 |
| CA17–CA19 | Puntajes, cobertura y asistencia | 10, 11, 13 |
| CA20 | Tendencia entre gestiones | 11, 13 |
| CA21 | Excel (y `ERR_DAS_02` sin datos) | 12, 13 |
| CA22 | Denominador cero → «—» | 11 |
| CA13 | Rendimiento ≤ 3 s con 20 000 postulaciones | 01, 05, 09 |

## Follow-ups (no se implementan en estas UT)

- **Fórmula de la tasa de asistencia y umbral de «beca en riesgo»:** validarlos con la DUBSS.
- **Varias réplicas:** coordinar el refresco con un bloqueo si algún día hay más de una instancia.
- **Proyecciones a futuro:** descartadas por el taller; no se incluyen.

## Qué NO incluyen estas UT

- La pantalla (ver [`UT-FRONTEND`](../UT-FRONTEND/README.md)).
- Registro de asistencia (CU-10), cotejo (CU-11), padrón de becados (CU-23) y bitácora (CU-25).
- Un botón para refrescar la vista a mano.
- Otros formatos de exportación (CSV).
- La app móvil.
