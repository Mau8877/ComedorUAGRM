# Unidades de trabajo — CU24-FRONTEND

**Caso de uso:** pantalla «Mi beca» del Postulante becado: estado de la beca y historial de asistencia con filtro por fechas.
**Fuente CU:** [`CU24 - Consultar historial de asistencia y estado de beca.md`](../../../CasosDeUso/CU24%20-%20Consultar%20historial%20de%20asistencia%20y%20estado%20de%20beca.md)
**Diagramas de grado:** mockup img 145 · secuencia img 132 · análisis img 062
**Recurso HTTP / contrato:** ver [UT-BACKEND](../UT-BACKEND/README.md#contrato-de-datos--recurso-http)

## Antes de implementar cualquier UT

Leer [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md),
[`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md),
[`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md),
[`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md),
[`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md) y
[`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md). Molde: la feature `bitacora` (CU25).

## Decisiones de diseño

- **Feature nueva `features/mibeca/`**, ruta `/mi-beca` (dentro de `_authenticated/`, `exigirRol('POSTULANTE')`) y ítem «Mi beca» en el menú del Postulante (`estudianteNavItems`).
- **Una sola pantalla** con dos secciones: «Estado de la beca» (tarjeta) y «Historial de asistencia» (tabla). El mockup (img 145) es móvil; en la web se usa el mismo contenido con el paquete `tanstack-table` (`DataTable` en escritorio, `DataCards` en pantallas angostas).
- **Historial 100 % del servidor:** `page`, `pageSize` y `sort` por `fecha` (la única columna ordenable, ERR_MBE_03 con otra) y rango con `filter[desde]` / `filter[hasta]` (dos `<input type="date">`). Un rango invertido se avisa en pantalla y no se manda (evita `ERR_MBE_02`).
- **A1:** `ERR_MBE_01` (`ApiError.code`) se muestra como aviso «No cuentas con una beca» con el botón «Ir a Mis postulaciones» (CU04); no se pide el historial.
- **A3:** con la beca suspendida o dada de baja se muestra el estado y el motivo; el historial sigue visible.
- **Solo lectura:** ninguna mutación; no hay botones de edición.
- **Fuera del mockup:** calendario mensual, «Faltas» y «Cumplimiento» (el sistema no tiene calendario de días de comedor; ver follow-ups de BACKEND), «Posición en la lista» y turno.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU24-FE-01](./UT-CU24-FE-01.md) | Tipos, API y hooks de Mi beca | — |
| [UT-CU24-FE-02](./UT-CU24-FE-02.md) | Pantalla «Mi beca», ruta y menú | FE-01 |
| [UT-CU24-FE-03](./UT-CU24-FE-03.md) | Pruebas | FE-02 |

**Cadena:** `01 → 02 → 03`. Requiere el backend (UT-BACKEND) para probar contra la API real.

## Cobertura del caso de uso

| Paso / flujo | Descripción | UTs |
|---|---|---|
| Flujo principal 1 a 5 | Estado, resumen, historial y filtro | 01, 02, 03 |
| A1 | Sin beca → aviso y enlace a CU-04 | 02, 03 |
| A2, A3 | Historial vacío, beca no vigente con motivo | 02, 03 |
| A5, A6 | Rango inválido, error con reintento | 02, 03 |
| CA11 | Pantalla completa | 02, 03 |

## Follow-ups (no se implementan en estas UT)

- Calendario mensual con asistió / falta del mockup.

## Qué NO incluyen estas UT

- La app móvil (ver `UT-MOBILE`) ni cambios de backend.
