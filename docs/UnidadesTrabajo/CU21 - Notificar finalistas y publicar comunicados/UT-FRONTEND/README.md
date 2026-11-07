# Unidades de trabajo — CU21-FE

**Caso de uso:** Pantalla web «Finalistas: notificación y documentación física» del Administrador DUBSS (tabla de finalistas, notificar, registrar entrega y nuevo comunicado) y bandeja real de avisos del usuario.
**Fuente CU:** [`CU21 - Notificar finalistas y publicar comunicados`](../../../CasosDeUso/CU21%20-%20Notificar%20finalistas%20y%20publicar%20comunicados.md)
**Diagramas de grado:** maqueta `144` (`Mockup_CU21_Notificar_finalistas_avisos`), análisis `061`, secuencia `131`.
**Recurso HTTP / contrato:** el de [`UT-BACKEND`](../UT-BACKEND/README.md) y [`NOTIFICACIONES_WEB.md`](../../../pendientes/NOTIFICACIONES_WEB.md).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): `ARQUITECTURA_FRONTEND.md`, `TANSTACK_QUERY_FRONTEND.md`, `TANSTACK_TABLE_FRONTEND.md`, `FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`, `RUTAS_NAVEGACION_FRONTEND.md`, `TAILWIND_STYLES_FRONTEND.md`, `ESTADO_GLOBAL_FRONTEND.md` y `TESTING_FRONTEND.md`. Ganan sobre estas fichas.

## Decisiones de diseño

Todo el código va en `projects/frontend/comedoruagrm/src/` (en estas fichas, `F/`).

- **Ruta:** como las rutas del administrador de hoy (`lista-priorizada.tsx`) viven en `routes/_authenticated/`, la pantalla va en `routes/_authenticated/finalistas.tsx`, no bajo un `_admin/` que todavía no existe. *(Ajuste a lo escrito más abajo.)*

- **Una sola pantalla**, como la maqueta: tabla de finalistas a la izquierda; a la derecha, «Registrar entrega física» (del finalista seleccionado) y «Nuevo aviso o comunicado». Solo la ve el `ADMINISTRADOR_DUBSS` (`exigirRol` en la ruta) con entrada «Finalistas» en el sidebar *(decisión del equipo: la maqueta la ubica bajo «Adjudicación», que todavía no existe)*.
- **Features:** `features/finalistas/` (api, schemas, tabla, paneles y la pantalla) y `features/comunicados/` (api, schema y formulario con vista previa), usados por la pantalla a través de su barrel. La convocatoria se elige con el hook de `features/priorizacion` (CU-06), importado por su barrel.
- **Lo de la maqueta que el CU no pide** (checklist de documentos recibidos, «Guardar borrador», «Publicación: Inmediata») **no se construye**; el campo de observaciones sí.
- **Formularios** con TanStack Form + Zod, errores inline solo en campos tocados; el plazo debe ser posterior a hoy y la fecha de recepción no puede ser futura. La confirmación del envío y de la publicación usa `AlertDialog`.
- **Tabla** con el paquete `tanstack-table` (`useDataTable`, `DataTable`, `DataCards`, `DataTableToolbar`), paginación server-side y selección de filas; sin `sort` hasta que el backend lo documente.
- **Bandeja real:** se reemplaza `useMockNotifications` por un hook de TanStack Query (`features/notificaciones/api`) que arma el `NotificationsController` que ya espera `NotificationsMenu`, tanto en `AdminLayout` como en `EstudianteLayout`. El componente visual no cambia.
- **Mensajes** en español y errores vía `getErrorMessage`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| UT-CU21-FE-01 | API: hooks, `keys.ts` y schemas de finalistas y comunicados | UT-CU21-BE-10 |
| UT-CU21-FE-02 | Tabla de finalistas con estados y selección | 01 |
| UT-CU21-FE-03 | Diálogo «Notificar seleccionados» (plazo, lugar y horario, documentos) | 01, 02 |
| UT-CU21-FE-04 | Panel «Registrar entrega física» | 01, 02 |
| UT-CU21-FE-05 | Formulario de comunicado con vista previa y confirmación | 01 |
| UT-CU21-FE-06 | Pantalla, ruta y entrada del sidebar | 02, 03, 04, 05 |
| UT-CU21-FE-07 | Bandeja real de avisos en los layouts (sin mocks) | UT-CU21-BE-09 |

**Cadena:** `01 → 02 → 03, 04`; `01 → 05`; `02–05 → 06`; `07` independiente de `02–06`.

## Alcance de cada UT

- **01:** `useFinalistas`, `useNotificarFinalistas`, `useRegistrarEntrega`, `usePublicarComunicado`, `useComunicados`, `keys.ts` por feature, schemas Zod y tipos; invalidación `['finalistas','list']` tras cada mutación.
- **02:** columnas Pos., Finalista, Puntaje, Notificación («Sin enviar» / «Enviada» / «Fallida»), Doc. física («Pendiente» / «Recibida»); búsqueda por nombre o CI; selección múltiple; estado vacío (A10) y de error.
- **03:** diálogo con plazo (fecha), lugar y horario y documentos; resumen de a cuántos se enviará; muestra el resultado (enviados y fallidos) y deja reseleccionar a los fallidos (A2).
- **04:** panel del finalista seleccionado: fecha de recepción (hoy por defecto) y observaciones; muestra la fecha ya registrada si existe y avisa que corregirla deja la anterior en la bitácora (A5).
- **05:** título, contenido y «Dirigido a» (Todos los postulantes / Finalistas); vista previa, confirmación y mensaje de a cuántos llegó.
- **06:** `routes/_authenticated/finalistas.tsx`, `FinalistasScreen`, entrada en `adminNavItems.ts`, estados de carga y de error, y selección de convocatoria.
- **07:** hook `useNotificaciones` con TanStack Query (paginación de a 10, contador aparte, marcar leída, leer todo, eliminar todas) montado en ambos layouts; se retira el mock de `pruebas_layout`.

## Cobertura del caso de uso

| Paso / flujo | UTs |
|---|---|
| Flujo A (pasos 1–7), A1, A2, A6, A10 | 01, 02, 03, 06 |
| Flujo B (pasos 8–11), A3, A4, A5, A7 | 01, 02, 04 |
| Flujo C (pasos 12–16), A8 | 01, 05 |
| Flujo D (paso 17) | 07 |
| A9, A11, A12, A14 | 06 (`401` lleva al login por el cliente HTTP; el menú oculta la opción a otros roles) |
| CA01–CA20 | 02–07 |

## Follow-ups (no se implementan en estas UT)

- Opción `Adjudicados` en el comunicado, borrador y publicación programada.
- Atajo «Notificar» desde la auditoría de expedientes (CU-07).

## Qué NO incluyen estas UT

- Cambios en el backend (ver [`UT-BACKEND`](../UT-BACKEND/README.md)).
- Bandeja y push en la app móvil.
- Seleccionar finalistas, auditar, devolver o archivar (CU-05, CU-07, CU-19, CU-20).
