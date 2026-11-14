# UT-CU08-FE-03 — Panel «Generar acta» y vista previa

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-FE-03` |
| **Nombre** | Panel «Generar acta» y vista previa |
| **Caso de uso** | CU08-FE |
| **Depende de** | `UT-CU08-FE-01`, `UT-CU08-FE-02` |

## Antes de implementar

- [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md), [`ESTADO_GLOBAL_FRONTEND.md`](../../../../.claude/rules/frontend/ESTADO_GLOBAL_FRONTEND.md) (el PDF y la convocatoria elegida son estado local de la pantalla, no de un store), [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md).
- Como referencia: la maqueta `108` y `F/utils/descargarArchivo.ts`.

## Solicitud de negocio

Antes de generar un documento oficial y definitivo, el Administrador quiere ver cómo quedaría.

## Objetivo

El Administrador elige una convocatoria con resultados publicados y sin acta, y ve la vista previa del PDF.

## Alcance

- `F/features/actas/components/GenerarActaPanel.tsx` (se monta en `ActasScreen`, encima de la tabla):
  - Selector de convocatoria con `useActasPendientes` (convocatorias publicadas sin acta); muestra el nombre y los adjudicados. Sin pendientes: mensaje «No hay convocatorias con resultados publicados pendientes de acta» (A1) y los botones deshabilitados. Se preselecciona la primera.
  - **No hay campo de número de acta** (la maqueta lo tenía; se elimina): una nota indica que el número lo asigna el sistema al generar.
  - Botón «Vista previa»: usa `useVistaPreviaActa`; mientras pide el PDF se bloquea y dice «Generando vista previa…»; al recibirlo lo muestra en un `<iframe>` (zona inferior de la maqueta) con una dirección de objeto local. La dirección se libera (`URL.revokeObjectURL`) al pedir otra vista previa, al cambiar de convocatoria y al desmontar. Sin vista previa, la zona muestra el texto de ayuda de la maqueta.
  - Botón «Generar PDF»: abre la ventana de confirmación de la UT-04 (deshabilitado sin convocatoria elegida o mientras se pide algo).
  - Errores con `mapearErrorActas` en el propio panel: `ERR_ACT_01` y `ERR_ACT_02` refrescan la lista de pendientes y avisan; `ERR_PRI_01` vuelve a la selección; error de red o `5xx` muestra un mensaje con reintento y nunca un `<iframe>` roto.
- Tests: carga y muestra los pendientes, sin pendientes deshabilita los botones, la vista previa se pide con la convocatoria elegida, se muestra el PDF y se libera la dirección al cambiar de convocatoria o desmontar, el botón se bloquea mientras genera, cada error muestra su mensaje, y no existe el campo de número.

Archivos esperados (crear): `GenerarActaPanel.tsx` y su test. (Tocar): `components/index.ts` y `ActasScreen.tsx`.

## Fuera de alcance (no tocar)

- La ventana de confirmación y la generación (UT-04).
- La tabla de actas generadas (UT-02).
- Guardar el PDF en la caché o en un store.

## Resultado esperado

El Administrador ve en pantalla cómo quedará el acta antes de generarla.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
