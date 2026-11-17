# Unidades de trabajo — CU26-FRONTEND

**Caso de uso:** pantalla «Parámetros del modelo»: pesos vigentes y nuevos, guardar versión, reprocesar puntajes con su avance e historial de versiones.
**Fuente CU:** [`CU26 - Parametrizar modelo de preselección y reprocesar puntajes.md`](../../../CasosDeUso/CU26%20-%20Parametrizar%20modelo%20de%20preselecci%C3%B3n%20y%20reprocesar%20puntajes.md)
**Diagramas de grado:** análisis img 063 · secuencia img 133 · mockup img 146
**Recurso HTTP / contrato:** ver [UT-BACKEND](../UT-BACKEND/README.md); ruta `/parametros-modelo` (solo `ADMINISTRADOR_DUBSS`)

## Antes de implementar cualquier UT

Leer [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md),
[`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md),
[`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md),
[`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md),
[`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md) y
[`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md). Estas fichas no sustituyen esas reglas.

## Decisiones de diseño

- **Feature nueva `preseleccion`** (`src/features/preseleccion/`), con la forma de siempre (`api`, `components`, `schemas`, `screens`, `tests`, `types`).
- **Una sola pantalla** con tres bloques: formulario de pesos, panel de reprocesar e historial (el mockup los muestra juntos).
- **El «Borrador» es el estado del formulario**: no se guarda nada hasta «Guardar versión»; con cambios sin guardar, reprocesar se bloquea.
- **La suma se valida en vivo** y bloquea «Guardar»; el servidor vuelve a validar (`ERR_MOD_01`). Las variables y su orden están en `VARIABLES_MODELO` (igual que `VariablesModelo` del backend).
- **El avance se consulta cada 2 s** (`refetchInterval` solo mientras el lote está `PENDIENTE` o `EN_PROCESO`); al volver a abrir la pantalla con un lote en curso, el resumen lo trae y se sigue.
- **El historial usa el paquete `tanstack-table`** (tabla y tarjetas), 100 % del servidor, sin ordenar.

## Orden de ejecución

| Código | Nombre | Depende de |
| --- | --- | --- |
| [UT-CU26-FE-01](./UT-CU26-FE-01.md) | Tipos, API, esquema y errores | — |
| [UT-CU26-FE-02](./UT-CU26-FE-02.md) | Pantalla, formulario de pesos, historial, ruta y menú | FE-01 |
| [UT-CU26-FE-03](./UT-CU26-FE-03.md) | Panel de reprocesamiento y su confirmación | FE-01, FE-02 |

**Cadena:** `01 → 02 → 03`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
| --- | --- | --- |
| Pasos 1–2 | Abrir la pantalla: versión vigente e historial | 02 |
| Pasos 3–5 / A1 / A2 / A9 | Editar, validar y guardar | 01, 02 |
| Pasos 6–8 / A6 | Elegir convocatoria, resumen y confirmar o cancelar | 03 |
| Pasos 9–11 / A5 | Avance, fallidas y enlace a la lista priorizada | 03 |
| A3, A4, A7, A8, A11 | Avisos de bloqueo y lote interrumpido | 03 |
| CA01–CA05, CA08, CA09 | Versiones, avance y cancelación | 02, 03 |

## Follow-ups (no se implementan en estas UT)

- Conectar el puntaje mínimo para ser finalista cuando la DUBSS lo defina (es de cada convocatoria, CU13).
- Cancelar un lote en curso.

## Qué NO incluyen estas UT

- Móvil: este CU es solo web.
- El cálculo de puntajes ni la lista priorizada (CU06): solo se enlaza.
