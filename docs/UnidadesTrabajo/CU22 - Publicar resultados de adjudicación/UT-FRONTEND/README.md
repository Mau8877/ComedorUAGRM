# Unidades de trabajo — CU22-FE

**Caso de uso:** Pantalla «Publicar resultados» del Administrador DUBSS (resumen, decisiones y confirmación de la publicación) y pantalla «Resultados» del postulante (su resultado y la lista de adjudicados), además de habilitar el destinatario «Adjudicados» en los comunicados.
**Fuente CU:** [`CU22 - Publicar resultados de adjudicación`](../../../CasosDeUso/CU22%20-%20Publicar%20resultados%20de%20adjudicación.md)
**Diagramas de grado:** CU-22 (maqueta `115`, análisis `087` y secuencia `102`).
**Recurso HTTP / contrato:** el de [`UT-BACKEND`](../UT-BACKEND/README.md).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): `ARQUITECTURA_FRONTEND.md`, `TANSTACK_QUERY_FRONTEND.md`, `TANSTACK_TABLE_FRONTEND.md`, `FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`, `RUTAS_NAVEGACION_FRONTEND.md`, `TAILWIND_STYLES_FRONTEND.md`, `ESTADO_GLOBAL_FRONTEND.md` y `TESTING_FRONTEND.md`. Ganan sobre estas fichas.

## Decisiones de diseño

Todo el código va en `projects/frontend/comedoruagrm/src/` (en estas fichas, `F/`).

- **Una feature nueva, `features/resultados/`**, con dos pantallas: `PublicarResultadosScreen` (Administrador) y `ResultadosScreen` (postulante). Cada una cuelga del layout de su rol (`_authenticated/_admin` y `_authenticated/_estudiante`), siguiendo la forma de las rutas ya existentes; la decisión de rol es estructural, no un `if`.
- **Una ventana de confirmación irreversible:** `PublicarResultadosDialog` advierte que la publicación no se puede deshacer y que se avisará a los finalistas, con la cantidad (`aAvisar`).
- **Reutilizar el selector de convocatoria del CU-06** (`useConvocatoriasPriorizables`, ya exportado por el barrel de `features/priorizacion/`); no se duplica. Si una tercera feature lo necesita, se mueve a `shared/api/` en ese momento, no antes.
- **Tablas:** las decisiones del Administrador y los adjudicados del postulante son listados paginados del backend, así que usan el paquete `tanstack-table` (`useDataTable` + `DataTable` en pantallas anchas y `DataCards` en móvil), con `enableSorting` solo en los campos que el backend documenta (`puntajeFinal`, `apellidos`, `nombres` y, en la lista pública, `nota`). `pageSizeOptions` por defecto (10/20/50).
- **Sin adjudicados también se publica:** si el resumen indica cero adjudicados, la ventana lo avisa pero no bloquea.
- **Sin idempotencia por clave:** el doble clic se evita deshabilitando el botón mientras se envía; si aun así llega una segunda petición, el backend responde `ERR_PUB_02`.
- **El botón «Publicar» está deshabilitado con su motivo visible** cuando `puedePublicar` es falso (faltan decisiones, ya publicada, convocatoria no cerrada). El backend sigue siendo quien decide de verdad.
- **Lista pública:** muestra nombre y apellido, nota, carrera y facultad. La columna se llama «Nota» y es el puntaje final de la adjudicación; no se muestra nada más (ni CI, correo ni ingresos). **No** aparecen los no adjudicados ni se muestra el motivo; cada postulante ve solo su propio resultado en un recuadro arriba.
- **Entrada del postulante:** la pantalla lista las convocatorias con resultados publicados y abre por defecto la más reciente. El acceso desde el menú del layout del postulante; el acceso desde el CU-04 queda como follow-up.
- **Consistencia de keys:** `resultadosKeys` por módulo (`['resultados', 'resumen', codigo]`, `['resultados', 'decisiones', ...]`, `['resultados', 'publicados', ...]`, `['resultados', 'mio', codigo]`). Publicar invalida `['resultados']`, `['adjudicacion']` y `['finalistas', 'list']`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU22-FE-01](./UT-CU22-FE-01.md) | API: hooks, tipos, keys y mensajes de error | UT-CU22-BE-08 |
| [UT-CU22-FE-02](./UT-CU22-FE-02.md) | Pantalla «Publicar resultados» del Administrador | 01 |
| [UT-CU22-FE-03](./UT-CU22-FE-03.md) | Ventana de confirmación y publicación | 01, 02 |
| [UT-CU22-FE-04](./UT-CU22-FE-04.md) | Pantalla «Resultados» del postulante | 01 |
| [UT-CU22-FE-05](./UT-CU22-FE-05.md) | Destinatario «Adjudicados» en los comunicados | UT-CU22-BE-09 |

**Cadena:** `01 → 02 → 03`; `01 → 04`; `05` independiente de las demás pantallas.

## Alcance de cada UT

- **01:** tipos y respuestas, `resultadosKeys`, funciones de API, `useResumenPublicacion`, `useDecisionesResultado`, `usePublicarResultados` (invalida `['resultados']`, `['adjudicacion']` y `['finalistas','list']`), `useConvocatoriasPublicadas`, `useAdjudicadosPublicos`, `useMiResultado` y `mapearErrorResultados` (`ERR_PRI_01`, `ERR_PUB_01` a `ERR_PUB_04`).
- **02:** ruta del Administrador (rol `ADMINISTRADOR_DUBSS`), opción de menú, selector de convocatoria, tarjetas del resumen, tabla de decisiones con filtro por resultado y botón «Publicar».
- **03:** `PublicarResultadosDialog`: advertencia, cantidad a avisar, confirmar o cancelar, estado en curso, errores y éxito.
- **04:** ruta del postulante, opción de menú, selector de convocatoria publicada, recuadro «Mi resultado» y lista de adjudicados con `DataTable` y `DataCards`.
- **05:** el selector de destinatarios del comunicado ofrece «Adjudicados» y exige convocatoria.

## Cobertura del caso de uso

| Paso / flujo | UTs |
|---|---|
| Paso 1 (opción «Publicar resultados» y convocatoria) | 02 |
| Paso 2 (resumen de adjudicados y no adjudicados) | 02 |
| Pasos 3–5, A4 (revisar, confirmar, cancelar) | 03 |
| A1 (pendientes), A2 (ya publicada) | 01, 02, 03 |
| Pasos 6–7 (avisos y confirmación) | 03 |
| Consulta del postulante (CA10) | 04 |
| `401` lleva al login por el cliente HTTP; `403` la ruta exige el rol | 02, 04 |
| Comunicados a `ADJUDICADOS` | 05 |
| CA01–CA03, CA08, CA10, CA13 | 01–04 |

## Follow-ups (no se implementan en estas UT)

- Que el CU-04 muestre «adjudicada» o «no adjudicada» solo después de publicar y enlace a esta pantalla.

## Qué NO incluyen estas UT

- Cambios en el backend (ver [`UT-BACKEND`](../UT-BACKEND/README.md)).
- Adjudicar o no adjudicar (CU-05), devolver (CU-19), archivar o rechazar (CU-20), el acta (CU-08) y el padrón (CU-23).
- La app móvil.
