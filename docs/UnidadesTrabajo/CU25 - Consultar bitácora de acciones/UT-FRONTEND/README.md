# Unidades de trabajo — CU25-FRONTEND

**Caso de uso:** pantalla «Bitácora» de solo lectura, con filtros, orden y paginación.
**Fuente CU:** [`CU25 - Consultar bitácora de acciones.md`](../../../CasosDeUso/CU25%20-%20Consultar%20bit%C3%A1cora%20de%20acciones.md)
**Diagramas de grado:** mockup img 117 · secuencia img 104
**Recurso HTTP / contrato:** ver [UT-BACKEND](../UT-BACKEND/README.md#contrato-de-datos--recurso-http)

## Antes de implementar cualquier UT

Leer [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md),
[`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) y
[`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md).

## Decisiones de diseño

- **Sin botón «Buscar»** (el mockup lo tiene): la búsqueda por usuario se aplica con debounce y los demás filtros
  al cambiar, igual que el resto de los listados del panel.
- **Entidad sin `#id`**: el backend no expone el id interno.
- **Rango invertido**: la pantalla avisa y no manda la consulta (evita `ERR_BIT_02`).
- **`enableSorting`** solo en fecha, acción y entidad: lo que acepta el backend.

## Orden de ejecución

| Código | Nombre | Depende de |
| --- | --- | --- |
| [UT-CU25-FE-01](./UT-CU25-FE-01.md) | Tipos, API y hooks de la bitácora | UT-CU25-BE-02 |
| [UT-CU25-FE-02](./UT-CU25-FE-02.md) | Pantalla, ruta y menú | FE-01 |
| [UT-CU25-FE-03](./UT-CU25-FE-03.md) | Pruebas | FE-01, FE-02 |

**Cadena:** `01 → 02 → 03`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
| --- | --- | --- |
| Flujo principal | Ver y filtrar | 01, 02 |
| A1 | Lista vacía: «Sin resultados» | 02 |
| A2 / CA03 | Rango invertido | 02 |
| CA05 | Sin acciones de fila | 02 |

## Follow-ups (no se implementan en estas UT)

- Ver el `detalle` de una fila (hoy no se muestra).

## Qué NO incluyen estas UT

- Exportar la bitácora ni editar/eliminar filas.
