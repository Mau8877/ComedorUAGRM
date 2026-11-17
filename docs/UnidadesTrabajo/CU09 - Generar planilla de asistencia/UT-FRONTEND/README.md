# Unidades de trabajo — CU09-FRONTEND

**Caso de uso:** La pantalla «Planillas» del panel web: elegir convocatoria y periodo, ver cuántos becados entran, previsualizar, generar la planilla y descargar las ya generadas.
**Fuente CU:** [`CU09 - Generar planilla de asistencia.md`](../../../CasosDeUso/CU09%20-%20Generar%20planilla%20de%20asistencia.md)
**Diagramas de grado:** contexto img 068 · análisis img 081 · secuencia img 096 · mockup img 109
**Recurso HTTP / contrato:** ver [`UT-BACKEND/README.md`](../UT-BACKEND/README.md#contrato-de-datos--recurso-http). Pantalla `/planillas` (solo `ADMINISTRADOR_DUBSS`).

> En estas fichas, `F/` = `projects/frontend/comedoruagrm/src`.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md), [`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md), [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md), [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md), [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md) y [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md). Estas fichas **no** sustituyen esas reglas.

Los moldes son la pantalla del padrón (CU23, selector de convocatoria y tabla) y el panel del acta (CU08, vista previa en `<iframe>` y descarga con dirección firmada).

## Decisiones de diseño

- **Las convocatorias del selector son las publicadas** (`useConvocatorias` con `estado: 'PUBLICADA'`), porque son las que tienen padrón; el backend no lo exige, la pantalla sí lo ofrece así.
- **El periodo se valida en el cliente con las mismas reglas del backend** (`crearPeriodoSchema`, fábrica que recibe los límites de la gestión que trae el resumen) y el servidor vuelve a validar; su error se muestra tal cual. Un formulario de TanStack Form comparte la validación entre «Generar planilla» y «Vista previa» (un ref indica qué botón envió).
- **No hay fechas por defecto**: lo que la DUBSS quiera como periodo lo escribe el Administrador; los `<input type="date">` solo acotan con `min`/`max` la gestión.
- **No hay ventana de confirmación**: generar no es destructivo (una planilla es un registro de lo impreso y se pueden generar varias), a diferencia del acta.
- **La columna «PL-0007» del mockup se reemplaza por el periodo** (ver las decisiones del backend).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU09-FE-01](./UT-CU09-FE-01.md) | API: tipos, keys, hooks y mensajes de error | `UT-CU09-BE-06` |
| [UT-CU09-FE-02](./UT-CU09-FE-02.md) | Validación del periodo (Zod) | FE-01 |
| [UT-CU09-FE-03](./UT-CU09-FE-03.md) | Pantalla «Planillas», ruta y menú | FE-01, FE-02 |

**Cadena:** `01 → 02 → 03`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo A (1 a 5, 10) | Elegir convocatoria y periodo, ver el resumen, previsualizar, generar | 01, 03 |
| Flujo B | Tabla de planillas, orden, paginación y descarga | 01, 03 |
| A1 | Sin convocatorias publicadas | 03 |
| A2 | Sin becas vigentes: botones bloqueados | 03 |
| A3, A4, A5 | Periodo inválido, en el cliente y en el servidor | 01, 02, 03 |
| A6 | Convocatoria inexistente | 01, 03 |
| A7, A9 | Errores de servidor, de red y de archivo, con «Reintentar» | 01, 03 |
| CA16 | Resumen y bloqueo sin becas | 03 |
| CA17 | Validación del periodo sin llamar al servidor | 02, 03 |

## Follow-ups (no se implementan en estas UT)

- Mostrar la columna de código de planilla si la DUBSS lo pide.
- Prueba manual con el stack levantado (MinIO real) y revisión del PDF impreso.

## Qué NO incluyen estas UT

- El backend (ver `UT-BACKEND`) y cualquier cambio a las pantallas del padrón o del acta.
- La app móvil: el CU es solo web.
- El registro y el cotejo de asistencia (CU-10 y CU-11).
