# UT-CU08-FE-02 — Pantalla «Actas»: ruta, menú, tabla y descarga

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-FE-02` |
| **Nombre** | Pantalla «Actas»: ruta, menú, tabla y descarga |
| **Caso de uso** | CU08-FE |
| **Depende de** | `UT-CU08-FE-01` |

## Antes de implementar

- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md), [`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md) (verificar el contrato contra `ENDPOINTS_BACKEND.md`), [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).
- Como referencia: `F/routes/_authenticated/publicar-resultados.tsx`, `PublicarResultadosScreen` y `columnasDecisiones` (CU-22), y `DocumentosDigitales` (CU-07, abre una dirección temporal).

## Solicitud de negocio

El Administrador necesita encontrar las actas ya generadas y bajar el PDF de cualquiera.

## Objetivo

El Administrador entra a «Actas», ve la tabla de actas generadas y descarga cada una.

## Alcance

- `F/routes/_authenticated/actas.tsx`: `createFileRoute('/_authenticated/actas')` con `beforeLoad: exigirRol('ADMINISTRADOR_DUBSS')`, que monta `ActasScreen` con `titulo="Actas de adjudicación"`, `activeHref="/actas"` y `usuarioActual`, igual que `publicar-resultados.tsx`. Opción «Actas» (ícono de documento) en `F/layouts/admin/adminNavItems.ts`, junto a «Publicar resultados».
- `F/features/actas/screens/ActasScreen.tsx` (la zona del panel de generar la agrega la UT-03; esta UT deja su lugar):
  - Tabla «Actas generadas» con `useDataTable` + `DataTable` (y `DataCards` en móvil): **N.º**, **Convocatoria**, **Gestión**, **Adjudicados**, **Fecha** (formato local de La Paz) y **Generada por**; `enableSorting` solo en `numero` y `generadaEn`; buscador del toolbar (número o convocatoria, el único `search` que el backend documenta); sin `filter[...]`; `errorMessage` con `getErrorMessage`.
  - Acción de fila «Descargar» (`RowActionButton`, ícono + tooltip) que usa `useAccesoActa`: mientras pide la dirección el botón se bloquea; al recibirla la abre (descarga o pestaña nueva) y no la guarda; `ERR_ACT_04` y `ERR_ACT_03` muestran su mensaje en un aviso y no abren nada.
  - Estados de carga, vacío («Todavía no hay actas generadas») y error con reintento.
- `F/features/actas/components/columnasActas.tsx`: la definición de columnas (una sola para tabla y cards).
- Tests: muestra las actas con su número y datos, paginación, orden y búsqueda mandan los parámetros esperados, vacío y error con reintento, «Descargar» abre la dirección una vez y no se queda cacheada, `ERR_ACT_04` muestra el mensaje sin abrir nada, y sin rol de Administrador la ruta no se muestra.

Archivos esperados (crear): `actas.tsx` (ruta), `ActasScreen.tsx`, `columnasActas.tsx`, `screens/index.ts`, `components/index.ts` y sus tests. (Tocar): `adminNavItems.ts` y el barrel `features/actas/index.ts`.

## Fuera de alcance (no tocar)

- El panel «Generar acta», la vista previa y la ventana de confirmación (UT-03 y UT-04).
- Mostrar la ruta del archivo o la dirección firmada en pantalla.
- Regenerar o eliminar un acta.

## Resultado esperado

El Administrador ve todas las actas y descarga la que necesite.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
