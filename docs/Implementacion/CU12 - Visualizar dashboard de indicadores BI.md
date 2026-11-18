# CU12 — Visualizar dashboard de indicadores BI

- **Caso de uso:** [enlace](../CasosDeUso/CU12%20-%20Visualizar%20dashboard%20de%20indicadores%20BI.md) · **Mapa de código:** [DOCUMENTACION.md](../DOCUMENTACION.md#cu12)
- **Diagramas de grado:** CU img 071 · análisis img 084 · secuencia img 099 · mockup img 112 · diseño lógico img 091 (§5.4.12, §6.2.12, §7.4.12, §7.5.12 del documento de taller)
- **Estado frente a los diagramas:** Difiere (ver tabla)

## Flujo real implementado

Todo exige sesión de Directivo (`401` sin sesión, `403` `ERR_AUTH_07` con otro rol). Es de solo lectura y no anota en la bitácora. Los datos salen de vistas materializadas (`mv_indicadores_convocatoria` y cuatro `mv_bi_*`) que `RefrescoIndicadoresJob` refresca cada noche a las 02:00 (y `RefrescoIndicadoresService.refrescarSiEstaVacia()` al arrancar).

**Entrar al dashboard** (web `/dashboard`)

1. `DashboardScreen` → `GET /api/v1/dashboard/filtros` (`200`) → `DashboardController` → `FiltrosDashboardService`: gestiones con sus convocatorias; la `vigente` marca la gestión por defecto.
2. `DashboardScreen` → `GET /api/v1/dashboard/indicadores` (`200`) → `IndicadoresService.consultar()`: totales, serie por mes y `actualizadoEn`. La asistencia sale de `IndicadoresRepository.asistenciaTotal/asistenciaMensual` (asistieron sobre esperados).
3. En paralelo, `DashboardScreen` → `GET /api/v1/dashboard/bi` (`200`) → `BiService.consultar()` (misma selección; `IndicadoresService.resolver()` valida gestión y convocatoria, `404` `ERR_DAS_01`) → `BiRepository`: embudo, facultades con carreras, comparación de convocatorias, histograma de puntajes, cobertura, asistencia por día de la semana y tendencia entre gestiones.
4. La pantalla muestra las tarjetas, los gráficos por mes y la sección «Análisis BI» (`EmbudoProceso`, `RankingFacultades`, `ComparacionConvocatorias`, `HistogramaPuntajes`, `CoberturaCupos`, `AsistenciaPorDiaSemana`, `TendenciaGestiones`). Sin datos (A1) o con el lote sin ejecutar (A2) lo informa; si falla solo el análisis (A8), ofrece «Reintentar» sin perder lo demás.

**Filtrar**

5. El Directivo cambia gestión o convocatoria y pulsa «Aplicar»; o elige una convocatoria en la comparación, que aplica el filtro directamente. Con `ERR_DAS_01` (A3) la pantalla vuelve a la selección vigente y avisa. Elegir una facultad solo despliega sus carreras en pantalla (no consulta al servidor).

**Descargar**

6. «Descargar PDF» → `GET /api/v1/dashboard/indicadores/pdf` → `IndicadoresPdfService.generar()` (Jasper `indicadores_dashboard.jrxml`, con una banda «Análisis BI» de resumen). Sin datos: `400` `ERR_DAS_02`.
7. «Descargar Excel» → `GET /api/v1/dashboard/bi/excel` → `BiExcelService.generar()` (Apache POI, hojas Resumen, Por mes, Por facultad, Por carrera, Convocatorias, Puntajes, Asistencia y Tendencia; `bi-{gestion}-{yyyyMMdd}.xlsx`). Sin datos: `400` `ERR_DAS_02`.

## Diseño del PDF (todos los PDF del sistema)

El PDF del dashboard comparte base con los de CU06, CU08 y CU09: banda azul UAGRM con título y franja roja, bloque de datos, tabla con cabecera azul y filas alternadas, y pie con «Generado el … por …» y «Página X de Y». Los estilos viven en un solo archivo, [`_estilos_uagrm.xml`](../../projects/backend/comedor-uagrm-core/src/main/resources/reports/_estilos_uagrm.xml), que [`ReportePdf`](../../projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend/common/report/ReportePdf.java) inserta en cada plantilla (marca `<!--ESTILOS_UAGRM-->`) al compilarla. El dashboard suma tarjetas de totales y dos gráficos de barras (embudo y facultades) dibujados con [`GraficosPdf`](../../projects/backend/comedor-uagrm-core/src/main/java/com/comedoruagrm/backend/common/report/GraficosPdf.java); el gráfico de facultades muestra las 8 que más postulan y el texto debajo lista todas.

## Diferencias con los diagramas

| Diagrama | Lo que dibuja | Lo que hace la implementación | Por qué |
| --- | --- | --- | --- |
| Secuencia (img 099) | Consulta de `IndicadoresConvocatoria` por gestión | Vistas materializadas refrescadas por lote (job nocturno) y lectura por `IndicadoresService`/`BiService` | RNF «BI en lote»: la consulta no afecta a las operaciones |
| Secuencia (img 099) | Solo indicadores | Además filtros, análisis BI, PDF y Excel | Decisiones de Mau |
| Análisis (img 084) | `DashboardView`, `DashboardController`, `IndicadoresConvocatoria` | Más `FiltrosDashboardService`, `BiService`, `BiExcelService`, `IndicadoresPdfService`, `RefrescoIndicadoresJob` y las vistas `mv_bi_*` | Capas y piezas del lote |
| Mockup (img 112) | Cuatro indicadores y gráficos; menú «Reportes» | Filtros con «Aplicar», análisis BI (siete bloques), botones «Descargar PDF» y «Descargar Excel»; sin «Reportes» | Decisión de Mau |
| Mockup (img 112) | Gráfico de asistencia por mes | Igual, más asistencia por día de la semana y becas en riesgo | Análisis BI |
| Diseño lógico (img 091) | Sin vistas | Cinco vistas materializadas (sin tablas nuevas) | Lote de BI |
| Casos de uso (img 071) | Actor Directivo | Igual; el backend exige el rol `DIRECTIVO` | — |
| Clases (img 090) | Sin cambios | Sin cambios (los DTOs `BiResponse` e `IndicadoresResponse` son de salida) | — |

## Para rehacer los diagramas

| Diagrama | Imagen | ¿Cambia? | Qué corregir | Hecho |
| --- | --- | --- | --- | --- |
| Secuencia | 099 | Sí | Ver especificación abajo | [ ] |
| Análisis | 084 | Sí | Ver especificación abajo | [ ] |
| Mockup | 112 | Sí | Ver especificación abajo | [ ] |
| Clases / dominio | 090 | No | — | — |
| Diseño lógico BD | 091 | Sí | Agregar las cuatro vistas `mv_bi_*` (ver abajo) | [ ] |
| Casos de uso | 071 | No | — | — |

Arquitectura (img 118 a 120): sin cambios (el lote nocturno ya estaba contemplado). Texto de grado: en `04_detalle_casos_uso.tex` (CU-12) y en RF-07 agregar el análisis BI, la descarga en Excel y el filtro cruzado por convocatoria; los criterios de tasa de asistencia y de «beca en riesgo» quedan **a validar con la DUBSS**.

### Especificación de cada diagrama afectado

#### Secuencia — img 099 (rehacer completo)

**Líneas de vida (izquierda → derecha):** `Actor` Directivo · `Boundary` DashboardScreen · `Control` DashboardController · `Control` IndicadoresService · `Control` BiService · `Control` BiExcelService · `Entity` mv_indicadores_convocatoria · `Entity` mv_bi_*

| N.º | De → a | Mensaje | Estado |
| --- | --- | --- | --- |
| 1 | Directivo → DashboardScreen | `abrirDashboard()` | [igual] |
| 2 | DashboardScreen → DashboardController | `GET /api/v1/dashboard/filtros` → `200` | [nuevo] |
| 3 | DashboardScreen → DashboardController | `GET /api/v1/dashboard/indicadores` → `200` | [cambia] |
| 4 | DashboardController → IndicadoresService | `consultar(gestion, convocatoria)` (`ERR_DAS_01` → `404`) | [cambia] |
| 5 | IndicadoresService → mv_indicadores_convocatoria | `find(totales, porMes, actualizadoEn)` | [cambia] |
| 6 | DashboardScreen → DashboardController | `GET /api/v1/dashboard/bi` → `200` | [nuevo] |
| 7 | DashboardController → BiService | `consultar(gestion, convocatoria)` | [nuevo] |
| 8 | BiService → mv_bi_* | `find(embudo, facultades, convocatorias, puntaje, cobertura, asistencia, tendencia)` | [nuevo] |
| 9 | DashboardScreen → Directivo | `mostrarIndicadoresYAnalisis()` | [cambia] |
| opt | [descargar Excel] DashboardScreen → DashboardController → BiExcelService | `GET /api/v1/dashboard/bi/excel` → `.xlsx` (`ERR_DAS_02` → `400` sin datos) | [nuevo] |
| opt | [descargar PDF] DashboardScreen → DashboardController → IndicadoresPdfService | `GET /api/v1/dashboard/indicadores/pdf` → PDF | [nuevo] |
| bg | RefrescoIndicadoresJob → vistas | `REFRESH MATERIALIZED VIEW CONCURRENTLY` cada noche a las 02:00 (en orden: la de asistencia por día antes que `mv_bi_convocatoria`) | [nuevo] |

#### Análisis (comunicación) — img 084 (rehacer completo)

Clases: `<<boundary>>` DashboardView (`DashboardScreen`) · `<<control>>` DashboardController · `<<control>>` IndicadoresService · `<<control>>` BiService · `<<control>>` BiExcelService · `<<control>>` IndicadoresPdfService · `<<entity>>` IndicadoresConvocatoria (`mv_indicadores_convocatoria`) · `<<entity>>` IndicadoresBi (`mv_bi_*`). Actor: Directivo.

Enlaces: Directivo — DashboardView; DashboardView — DashboardController; DashboardController — IndicadoresService, BiService, BiExcelService, IndicadoresPdfService; IndicadoresService — IndicadoresConvocatoria; BiService — IndicadoresBi y IndicadoresConvocatoria; BiExcelService — BiService.

Mensajes: `1 verDashboard()`, `1.1 consultarIndicadores()`, `1.1.1 find(indicadores)`, `2 consultarAnalisisBi()`, `2.1 find(analisis)`, `3 descargarExcel()`, `4 descargarPdf()`.

#### Mockup — img 112 (rehacer completo)

- Encabezado: título «Dashboard de indicadores», leyenda «Gestión … · actualizado el …», botones «Descargar Excel» y «Descargar PDF».
- Filtros: «Gestión» (selector), «Convocatoria» (selector con «Todas las convocatorias») y «Aplicar».
- Cuatro tarjetas: «Postulaciones recibidas», «Finalistas», «Becas adjudicadas», «Asistencia promedio» («Sin datos» si no hay registros).
- Gráficos: «Postulaciones por mes» (barras) y «Asistencia por mes» (línea).
- «Análisis BI»: «Embudo del proceso» (cinco barras con «% de la etapa anterior»); «Postulaciones por facultad» (barras de postulaciones y adjudicadas; al elegir una, «Carreras de …»); «Comparación entre convocatorias» (tabla: convocatoria, postulaciones, variación, finalistas, adjudicadas / cupos, tasa, puntaje IA; elegir una filtra el dashboard); «Distribución de puntajes de la IA» (7 tramos y promedio); «Cobertura de cupos» (porcentaje, barra y suspendidas, dadas de baja, reemplazos); «Asistencia por día de la semana» (barras y aviso de becas en riesgo); «Tendencia entre gestiones» (barras de postulaciones y adjudicadas).
- Estados: cargando (esqueletos), error con «Reintentar», sin datos (mensaje informativo), sin menú «Reportes».

#### Diseño lógico de la BD — img 091 (parchear)

Sin tablas nuevas. Cinco vistas materializadas (solo lectura, refrescadas por lote): `mv_indicadores_convocatoria` (convocatoria, mes), `mv_bi_asistencia_dia` (beca, día), `mv_bi_convocatoria` (convocatoria: embudo, cupos, cobertura, puntaje promedio, becas en riesgo), `mv_bi_carrera_convocatoria` (convocatoria, facultad, carrera) y `mv_bi_puntaje_convocatoria` (convocatoria, tramo de 10 puntos). Detalle en [`DiagramaConceptual.mermaid`](../DiagramaConceptual.mermaid) y la migración `V20261123100000__crear_vistas_bi_dashboard.sql`.
