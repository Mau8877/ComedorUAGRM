# Unidades de trabajo — CU05-FE

**Caso de uso:** Pantalla «Auditoría y adjudicación» del Administrador DUBSS (panel de la convocatoria y selección de finalistas) y ventanas «Adjudicar» / «No adjudicar» en el expediente del finalista, con el estado final visible en el expediente y en la lista de finalistas.
**Fuente CU:** [`CU05 - Auditar y adjudicar becas`](../../../CasosDeUso/CU05%20-%20Auditar%20y%20adjudicar%20becas.md)
**Diagramas de grado:** CU-05 (maqueta, análisis y secuencia; números de figura por confirmar).
**Recurso HTTP / contrato:** el de [`UT-BACKEND`](../UT-BACKEND/README.md).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): `ARQUITECTURA_FRONTEND.md`, `TANSTACK_QUERY_FRONTEND.md`, `TANSTACK_TABLE_FRONTEND.md`, `FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`, `RUTAS_NAVEGACION_FRONTEND.md`, `TAILWIND_STYLES_FRONTEND.md`, `ESTADO_GLOBAL_FRONTEND.md` y `TESTING_FRONTEND.md`. Ganan sobre estas fichas.

## Decisiones de diseño

Todo el código va en `projects/frontend/comedoruagrm/src/` (en estas fichas, `F/`). Lo marcado *(por revisar)* es propuesta del CU-05 aún sin confirmar.

- **Una feature nueva y dos piezas en una existente.** El panel y la selección son de la convocatoria, no de un expediente: viven en la feature nueva `features/adjudicacion/` (con su ruta `/adjudicacion` y su opción de menú). Adjudicar y no adjudicar se hacen **desde el expediente del finalista (CU-07)**, igual que devolver (CU-19) y archivar o rechazar (CU-20): viven en `features/expedientes/`. La insignia «Adjudicada» de la lista toca `features/finalistas/`.
- **Una sola ventana para las dos decisiones:** `DecidirAdjudicacionDialog` recibe `accion` (`adjudicar` | `no-adjudicar`) y cambia título, texto y botón. Muestra el nombre del postulante, su puntaje total y los cupos disponibles; el motivo (obligatorio, contador de 500 caracteres) solo aparece al no adjudicar.
- **Los cupos y el mínimo salen del panel.** El expediente ya trae `convocatoria.codigo`, `puntajeTotal`, `estadoPostulacion` y las auditorías; los cupos disponibles y el puntaje mínimo salen de `usePanelAdjudicacion(convocatoria.codigo)`, que `features/expedientes/` importa del barrel de `features/adjudicacion/` (no de un archivo interno, como exige la regla de dependencia).
- **Sin idempotencia por clave:** el doble clic se evita deshabilitando el botón mientras se envía; si aun así llega una segunda petición, el backend responde `ERR_ADJ_01`.
- **Sin aviso al postulante:** no hay cuadro de «notificar» y el mensaje de éxito no habla de notificación. El aviso es del CU-22.
- **La cantidad de finalistas solo la ve el Administrador:** se muestra únicamente dentro de esta pantalla (la ruta exige el rol) y la ventana de selección la propone editable; no se guarda ni se cambia la configurada.
- **Reutilizar el selector de convocatoria del CU-06:** `useConvocatoriasPriorizables` ya lo exporta el barrel de `features/priorizacion/` (el CU-21 lo reutiliza igual); esta feature lo usa de ahí y **no** lo duplica. Mover ese hook a `shared/api/` queda como decisión del equipo si una tercera feature lo necesita *(por revisar)*.
- **Botones deshabilitados con su motivo visible** (sin auditoría, última auditoría no «Validado» solo al adjudicar, puntaje insuficiente, cupos agotados, convocatoria no lista, ya decidida o resultados publicados). El backend sigue siendo quien decide de verdad; el frontend solo evita intentos inútiles. «No adjudicar» queda habilitado aunque los cupos estén agotados o el puntaje sea bajo, pero exige auditoría.
- **Decisiones finales:** no hay botón para reabrir ni cambiar una decisión.
- **Lista de finalistas:** el finalista decidido sigue en la lista con la insignia «Adjudicada» o «No adjudicada» *(por revisar)*.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU05-FE-01](./UT-CU05-FE-01.md) | API del panel y la selección: hooks, schema, tipos y mensajes de error | UT-CU05-BE-08 |
| [UT-CU05-FE-02](./UT-CU05-FE-02.md) | Pantalla «Auditoría y adjudicación» con el panel de cupos | 01 |
| [UT-CU05-FE-03](./UT-CU05-FE-03.md) | Ventana «Seleccionar finalistas» | 02 |
| [UT-CU05-FE-04](./UT-CU05-FE-04.md) | API de la decisión: hooks, schema, tipos y mensajes de error | UT-CU05-BE-08 |
| [UT-CU05-FE-05](./UT-CU05-FE-05.md) | Ventana «Adjudicar» / «No adjudicar» | 01, 04 |
| [UT-CU05-FE-06](./UT-CU05-FE-06.md) | Botones en el expediente y estado final | 05 |
| [UT-CU05-FE-07](./UT-CU05-FE-07.md) | Insignias «Adjudicada» y «No adjudicada» en la lista de finalistas | UT-CU05-BE-09, 06 |

**Cadena:** `01 → 02 → 03`; `04` y `01 → 05 → 06 → 07`.

## Alcance de cada UT

- **01:** tipos del panel y la selección, `seleccionSchema`, `adjudicacionKeys`, `obtenerPanelApi`/`seleccionarFinalistasApi`, `usePanelAdjudicacion`/`useSeleccionarFinalistas` (la selección invalida el panel y `['finalistas','list']`) y mapeo de `ERR_PRI_01`, `ERR_ADJ_01` y `ERR_ADJ_05`.
- **02:** ruta `/adjudicacion` (rol Administrador), opción de menú, `AdjudicacionScreen` con selector de convocatoria, panel de cupos y accesos a la lista priorizada y a la de finalistas.
- **03:** `SeleccionarFinalistasDialog` que propone la cantidad configurada, deja ajustarla y confirma.
- **04:** `noAdjudicarSchema`, `adjudicarFinalistaApi`/`noAdjudicarFinalistaApi`, `useAdjudicarFinalista`/`useNoAdjudicarFinalista` (invalidan expediente, finalistas y panel) y mapeo de `ERR_ADJ_01` a `ERR_ADJ_04`.
- **05:** `DecidirAdjudicacionDialog` con nombre, puntaje total, cupos disponibles, motivo solo al no adjudicar, confirmación, estado en curso y errores.
- **06:** botones «Adjudicar» y «No adjudicar» en `ExpedienteScreen`, motivos de deshabilitado, e insignia de estado final en el resumen.
- **07:** insignia «Adjudicada» (y reutilización de «No adjudicada») en la columna «Auditoría» de la lista.

## Cobertura del caso de uso

| Paso / flujo | UTs |
|---|---|
| Pasos 1–2 (menú, convocatoria, panel y lista) | 02 |
| Pasos 3–4, A1, A2 (selección, cantidad, suplentes) | 01, 03 |
| Pasos 5–6 (auditar y ver finalistas) | 06, 07 (reutiliza CU-07 y CU-21) |
| Pasos 7–9, A11 (ventana de decisión, motivo, cancelar) | 05, 06 |
| Paso 10, A3–A7, A10, A12 (`404`, `409`) | 01, 04, 05, 06 |
| Paso 12 (confirmación y estado final) | 05, 06 |
| Paso 14, A8 (acta y publicar: fuera de este CU) | — |
| A13, A14 (`401` lleva al login por el cliente HTTP; la ruta exige el rol) | 02, 06 |
| CA01–CA03, CA22 | 01–03 |
| CA04–CA14, CA18, CA19 | 04–07 |

## Follow-ups (no se implementan en estas UT)

- Pantalla del CU-13 con el campo «Cantidad de finalistas» (visible solo para el Administrador).
- Que el postulante vea «adjudicada» o «no adjudicada» solo después de publicar (CU-04 y CU-22).
- Botones de acta (CU-08) y de publicar resultados (CU-22), y el aviso de «faltan N finalistas por decidir» (A8), que depende de lo que exija el CU-22.

## Qué NO incluyen estas UT

- Cambios en el backend (ver [`UT-BACKEND`](../UT-BACKEND/README.md)).
- Devolver (CU-19), archivar o rechazar (CU-20), acta (CU-08), publicar resultados (CU-22) y padrón (CU-23).
- Editar la cantidad de finalistas de la convocatoria (CU-13).
- La app móvil.
