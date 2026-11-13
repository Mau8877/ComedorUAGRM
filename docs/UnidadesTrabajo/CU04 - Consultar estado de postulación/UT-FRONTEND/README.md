# Unidades de trabajo — CU04-FE

**Caso de uso:** Pantallas del Postulante «Mis postulaciones» (lista con filtro por gestión) y «Detalle de postulación» (estado visible, línea de tiempo e información asociada), y apertura del detalle desde un aviso de la bandeja.
**Fuente CU:** [`CU04 - Consultar estado de postulación`](../../../CasosDeUso/CU04%20-%20Consultar%20estado%20de%20postulación.md)
**Diagramas de grado:** CU-04 (maquetas de `images/134`–`146`, análisis y secuencia de `images/051`–`063` y `121`–`133`).
**Recurso HTTP / contrato:** el de [`UT-BACKEND`](../UT-BACKEND/README.md).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): `ARQUITECTURA_FRONTEND.md`, `TANSTACK_QUERY_FRONTEND.md`, `TANSTACK_TABLE_FRONTEND.md`, `RUTAS_NAVEGACION_FRONTEND.md`, `TAILWIND_STYLES_FRONTEND.md`, `ESTADO_GLOBAL_FRONTEND.md` y `TESTING_FRONTEND.md`. Ganan sobre estas fichas.

## Decisiones de diseño

Todo el código va en `projects/frontend/comedoruagrm/src/` (en estas fichas, `F/`).

- **Feature nueva `features/postulaciones/`** con dos pantallas: `MisPostulacionesScreen` y `DetallePostulacionScreen`, ambas del layout del postulante (`EstudianteLayout`, rol `POSTULANTE`), con la guarda `exigirRol('POSTULANTE')` como las demás rutas de ese rol.
- **Rutas:** `/mis-postulaciones` (lista) y `/mis-postulaciones/$codigo` (detalle, segmento dinámico), dentro de `_authenticated/`. Una opción nueva «Mis postulaciones» en `estudianteNavItems.ts`. El test `estudianteNavItems.test.ts` se actualiza.
- **Lista = listado paginado del backend:** usa el paquete `tanstack-table` (`useDataTable` + `DataTable` en pantallas anchas y `DataCards` en móvil), 100 % server-side, con `meta.grow` en la columna de la convocatoria, `enableSorting` solo en `fechaEnvio` (único campo que el backend documenta) y `pageSizeOptions` por defecto. El filtro por gestión usa `DataTableToolbarFilter` con un `Select`.
- **Gestiones del filtro:** las opciones salen de las gestiones de las postulaciones del propio usuario; si no hay un endpoint de gestiones disponible para el postulante, el filtro ofrece las gestiones de la primera página sin inventar un endpoint nuevo (queda como follow-up pedirlo al backend). A confirmar al implementar.
- **Detalle = pantalla propia**, no un modal: la línea de tiempo (pasos `COMPLETADO` y `PENDIENTE`) más bloques condicionales: observaciones (con el botón «Corregir y reenviar»), motivo, «Eres finalista» y resultado con el botón «Ver resultados de la convocatoria» (lleva a `/resultados`).
- **Botón «Corregir y reenviar»:** solo aparece si `puedeCorregir` es verdadero **y** existe la pantalla del CU-02; mientras no exista se oculta (no se muestra deshabilitado sin explicación). El enlace queda en un solo lugar para engancharlo cuando exista.
- **Estado visible:** se muestra tal cual lo devuelve el backend (nunca se recalcula en el cliente). Un `Badge` por estado con tokens del tema (`success`, `warning`, `info`, `destructive`, `muted`), sin colores sueltos.
- **Estado vacío (A1):** mensaje informativo; la invitación a postular se muestra solo si existe la pantalla de postulación del CU-02 (misma regla que el botón de corregir).
- **Errores:** `mapearErrorPostulaciones` traduce `ERR_POS_01` y el resto usa `getErrorMessage`; una falla de conexión muestra un mensaje claro con botón «Reintentar» (A10). Un `404` del detalle muestra «Postulación no encontrada» y un enlace a la lista. El `401` lo resuelve el cliente HTTP.
- **Keys:** `postulacionesKeys` (`['postulaciones', 'mias', params]` y `['postulaciones', 'detalle', codigo]`). No hay mutaciones, no se invalida nada.
- **Aviso → detalle (A6):** la bandeja del layout (`EstudianteLayout`) navega a `/mis-postulaciones/{postulacionCodigo}` cuando el aviso trae ese código; sin código, comportamiento actual.
- **Sin estado global:** los filtros y la página viven en `useState` de la pantalla.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU04-FE-01](./UT-CU04-FE-01.md) | API: tipos, keys, hooks y mensajes de error | UT-CU04-BE-04 |
| [UT-CU04-FE-02](./UT-CU04-FE-02.md) | Pantalla «Mis postulaciones» | 01 |
| [UT-CU04-FE-03](./UT-CU04-FE-03.md) | Pantalla de detalle con línea de tiempo | 01 |
| [UT-CU04-FE-04](./UT-CU04-FE-04.md) | Rutas, menú y apertura desde un aviso | 02, 03, UT-CU04-BE-05 |

**Cadena:** `01 → 02, 03 → 04`.

## Cobertura del caso de uso

| Paso / flujo | UTs |
|---|---|
| Pasos 1–2 (lista) | 02, 04 |
| Pasos 3–6 (detalle) | 03, 04 |
| A1 (sin postulaciones), A2 (filtro por gestión) | 02 |
| A3, A4, A5 (observaciones, motivo, finalista, resultado) | 03 |
| A6 (aviso abre el detalle) | 04 |
| A7, A8 (`401`/`403`) | 04 (guarda de rol y cliente HTTP) |
| A9 (`404`) | 01, 03 |
| A10 (falla de conexión) | 02, 03 |
| CA01–CA13 (presentación) | 02, 03, 04 |

## Follow-ups (no se implementan en estas UT)

- Habilitar «Corregir y reenviar» y la invitación a postular cuando exista el CU-02.
- Endpoint de gestiones académicas para el filtro, si hace falta.

## Qué NO incluyen estas UT

- Cambios en el backend (ver [`UT-BACKEND`](../UT-BACKEND/README.md)).
- Enviar, corregir o reenviar postulaciones (CU-02) y guardar borrador (CU-17).
- Marcar avisos como leídos ni la bandeja en sí (CU-EXTRA-01).
- La app móvil (ver [`UT-MOBILE`](../UT-MOBILE/README.md)).
