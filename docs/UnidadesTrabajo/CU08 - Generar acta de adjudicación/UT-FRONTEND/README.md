# Unidades de trabajo — CU08-FE

**Caso de uso:** Pantalla «Actas» del Administrador DUBSS: tabla de actas generadas, panel para generar el acta de una convocatoria con resultados publicados (con vista previa y ventana de confirmación) y descarga del PDF.
**Fuente CU:** [`CU08 - Generar acta de adjudicación`](../../../CasosDeUso/CU08%20-%20Generar%20acta%20de%20adjudicación.md)
**Diagramas de grado:** CU-08 (maqueta `108`, análisis `080` y secuencia `095`). La maqueta tiene un campo «Número de acta» editable que **se elimina**: el número es automático.
**Recurso HTTP / contrato:** el de [`UT-BACKEND`](../UT-BACKEND/README.md).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): `ARQUITECTURA_FRONTEND.md`, `TANSTACK_QUERY_FRONTEND.md`, `TANSTACK_TABLE_FRONTEND.md`, `FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`, `RUTAS_NAVEGACION_FRONTEND.md`, `TAILWIND_STYLES_FRONTEND.md`, `ESTADO_GLOBAL_FRONTEND.md` y `TESTING_FRONTEND.md`. Ganan sobre estas fichas.

## Decisiones de diseño

Todo el código va en `projects/frontend/comedoruagrm/src/` (en estas fichas, `F/`). Las decisiones de negocio están confirmadas por Mau (2026-11-13); las técnicas son propuesta del equipo, por revisar.

- **Una feature nueva, `features/actas/`**, con una sola pantalla, `ActasScreen`, que tiene dos zonas como en la maqueta: arriba, el panel «Generar acta» y la tabla «Actas generadas»; abajo, la vista previa del PDF.
- **Ruta y menú:** `routes/_authenticated/actas.tsx` con `beforeLoad: exigirRol('ADMINISTRADOR_DUBSS')` (misma forma que `publicar-resultados.tsx`) y la opción «Actas» en `layouts/admin/adminNavItems.ts`. Solo el Administrador; la decisión de rol es estructural, no un `if`.
- **Sin campo de número:** el panel solo tiene el selector de convocatoria y los botones «Vista previa» y «Generar PDF». El número lo asigna el backend y se ve después en la tabla.
- **Selector de convocatoria:** muestra solo las convocatorias con resultados publicados y sin acta (`GET /api/v1/actas/pendientes`); no reutiliza el selector del CU-06, porque aquí el criterio es otro. Sin pendientes, el panel lo informa.
- **Vista previa en la misma pantalla:** el PDF borrador se pide como `Blob` (`GET .../acta/vista-previa`), se muestra en un `<iframe>` con una dirección de objeto local y se libera (`URL.revokeObjectURL`) al cambiar de convocatoria, volver a pedirla o salir. No se guarda en la caché de TanStack Query ni en el estado global.
- **Confirmación irreversible:** `GenerarActaDialog` avisa que el acta es definitiva (no se regenera ni se elimina). Es una propuesta del equipo que sigue el patrón de publicar resultados.
- **Descarga:** el botón «Descargar» de la tabla pide la dirección temporal (`GET .../acta/descarga`) y la abre; si el backend responde `ERR_ACT_04` muestra el mensaje y no abre nada. Después de generar, la ventana ofrece «Descargar» con el mismo mecanismo.
- **Tablas:** la de actas es un listado paginado del backend, así que usa el paquete `tanstack-table` (`useDataTable` + `DataTable` en pantallas anchas y `DataCards` en móvil), con `enableSorting` solo en `numero` y `generadaEn`, el buscador del toolbar sobre número y convocatoria, y `pageSizeOptions` por defecto (10/20/50).
- **Consistencia de keys:** `actasKeys` por módulo (`['actas', 'list', ...]`, `['actas', 'pendientes', ...]`, `['actas', 'detail', codigo]`). Generar invalida `['actas']` y `['resultados']`.
- **Doble clic:** se evita deshabilitando los botones mientras se envía; si aun así llega una segunda petición, el backend responde `ERR_ACT_02` y la pantalla muestra el acta existente.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU08-FE-01](./UT-CU08-FE-01.md) | API: hooks, tipos, keys y mensajes de error | UT-CU08-BE-09 |
| [UT-CU08-FE-02](./UT-CU08-FE-02.md) | Pantalla «Actas»: ruta, menú, tabla y descarga | 01 |
| [UT-CU08-FE-03](./UT-CU08-FE-03.md) | Panel «Generar acta» y vista previa | 01, 02 |
| [UT-CU08-FE-04](./UT-CU08-FE-04.md) | Ventana de confirmación y generación | 01, 02, 03 |

**Cadena:** `01 → 02 → 03 → 04`.

## Alcance de cada UT

- **01:** tipos y respuestas, `actasKeys`, funciones de API (incluida la vista previa como `Blob`), `useActas`, `useActasPendientes`, `useActa`, `useGenerarActa` (invalida `['actas']` y `['resultados']`), `useVistaPreviaActa`, `useAccesoActa` y `mapearErrorActas` (`ERR_PRI_01`, `ERR_ACT_01` a `ERR_ACT_04`).
- **02:** ruta del Administrador, opción de menú, `ActasScreen` con la tabla «Actas generadas» (buscador, orden, paginación, vacío y error) y la acción «Descargar».
- **03:** panel «Generar acta»: selector de pendientes, vista previa en `<iframe>` y los botones «Vista previa» y «Generar PDF».
- **04:** `GenerarActaDialog`: advertencia de que es definitiva, confirmar o cancelar, estado en curso, errores y éxito con «Descargar».

## Cobertura del caso de uso

| Paso / flujo | UTs |
|---|---|
| Flujo A, paso 1 (entrar a «Actas», tabla y panel) | 02, 03 |
| Flujo A, pasos 2–3 (elegir convocatoria, vista previa) | 03 |
| Flujo A, pasos 4–6, A6 (generar, confirmar, cancelar) | 04 |
| Flujo A, pasos 9–11 (confirmación y descarga) | 04 |
| Flujo B (consultar y descargar) | 02 |
| A1 (sin pendientes) | 03 |
| A3, A4, A5, A9 (`ERR_ACT_01` a `ERR_ACT_04`) | 01, 03, 04, 02 |
| `401` lleva al login por el cliente HTTP; `403` la ruta exige el rol | 02 |
| CA10, CA11, CA13, CA14, CA15, CA17, CA19 | 01–04 |

## Follow-ups (no se implementan en estas UT)

- Mostrar el enlace al acta desde «Publicar resultados» (CU-22) una vez publicada.
- Mostrar el «Número de acta» definitivo cuando la DUBSS confirme el formato oficial.

## Qué NO incluyen estas UT

- Cambios en el backend (ver [`UT-BACKEND`](../UT-BACKEND/README.md)).
- Publicar resultados (CU-22), adjudicar (CU-05), padrón de becados (CU-23) y comprobantes individuales.
- Regenerar o eliminar un acta.
- La app móvil.
