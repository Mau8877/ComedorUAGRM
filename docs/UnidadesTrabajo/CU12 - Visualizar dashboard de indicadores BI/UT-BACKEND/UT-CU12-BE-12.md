# UT-CU12-BE-12 — Excel del análisis, PDF ampliado y endpoints

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-12` |
| **Nombre** | Excel del análisis, PDF ampliado y endpoints |
| **Caso de uso** | CU12-BE |
| **Depende de** | `UT-CU12-BE-06`, `UT-CU12-BE-07`, `UT-CU12-BE-11` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) y [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) (`ERR_DAS_02` ya existe: se actualiza su sección).

## Solicitud de negocio

El Directivo quiere llevarse el análisis completo, no solo el PDF de los totales.

## Objetivo

Descarga del análisis en Excel y un PDF con un resumen del análisis.

## Alcance

- `pom.xml`: `poi-ooxml` (propiedad `poi.version`).
- `B/features/dashboard/service/BiExcelService.java`: `generar(actor, gestion, convocatoria)` → `ExcelGenerado(nombreArchivo, contenido)`. Hojas: Resumen, Por mes, Por facultad, Por carrera, Convocatorias, Puntajes, Asistencia y Tendencia. `ERR_DAS_02` si `sinDatos`. Nombre `bi-{gestion}-{yyyyMMdd}.xlsx`. Solo lectura, sin bitácora.
- `B/features/dashboard/service/IndicadoresPdfService.java` y `R/reports/indicadores_dashboard.jrxml`: banda «Análisis BI» (embudo, facultades, convocatorias).
- `B/features/dashboard/controller/DashboardController.java`: `GET /api/v1/dashboard/bi` y `GET /api/v1/dashboard/bi/excel` (solo `DIRECTIVO`, con `filter[gestion]` y `filter[convocatoria]`).
- `docs/errors/dashboard/ERRORES_DASHBOARD.md`: `ERR_DAS_01` y `ERR_DAS_02` también los lanzan `BiService` y `BiExcelService`.

Archivos esperados (crear/tocar): `BiExcelService`, `IndicadoresPdfService`, `indicadores_dashboard.jrxml`, `DashboardController`, `pom.xml`, `ERRORES_DASHBOARD.md`.

## Fuera de alcance (no tocar)

- Otros formatos (CSV) o envío por correo.
- La pantalla (ver `UT-FRONTEND`).

## Resultado esperado

El Directivo descarga el Excel del análisis y el PDF incluye un resumen del análisis.

## Validación

1. `BiExcelServiceTest` (4): hojas, contenido y `ERR_DAS_02` sin datos.
2. `IndicadoresPdfServiceTest` (8) y `DashboardControllerTest` (16): el PDF sigue empezando con `%PDF`; `/bi` y `/bi/excel` devuelven el sobre y `401`/`403`.
