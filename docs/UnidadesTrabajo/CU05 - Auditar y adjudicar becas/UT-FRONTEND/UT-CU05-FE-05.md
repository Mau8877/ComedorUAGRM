# UT-CU05-FE-05 — Ventana «Adjudicar» / «No adjudicar»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-FE-05` |
| **Nombre** | Ventana «Adjudicar» / «No adjudicar» |
| **Caso de uso** | CU05-FE |
| **Depende de** | `UT-CU05-FE-01`, `UT-CU05-FE-04` |

## Antes de implementar

- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md), [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).
- Como referencia: `F/features/expedientes/components/CerrarPostulacionDialog.tsx` (CU-20) y `DevolverPostulacionDialog.tsx` (CU-19).

## Solicitud de negocio

El Administrador confirma la decisión final viendo a quién afecta, con qué puntaje y cuántos cupos quedan.

## Objetivo

El Administrador puede confirmar o cancelar «Adjudicar» o «No adjudicar» (esta con motivo) con la información necesaria a la vista.

## Alcance

- `F/features/expedientes/components/DecidirAdjudicacionDialog.tsx` con la prop `accion` (`'adjudicar' | 'no-adjudicar'`), el expediente y el panel de la convocatoria (`usePanelAdjudicacion(convocatoria.codigo)` del barrel de `features/adjudicacion`). Muestra: título y botón según la acción («Adjudicar beca» / «No adjudicar»), **nombre del postulante**, **puntaje total** (o «No disponible») y **cupos disponibles** y puntaje mínimo (o «Sin definir»); un aviso de que **la decisión es definitiva** y de que **el postulante la conocerá al publicarse los resultados** (no se le avisa ahora).
- **Adjudicar:** sin campos; botones «Cancelar» y «Adjudicar».
- **No adjudicar:** campo **Motivo** (obligatorio, `Textarea`, **contador de 500 caracteres**) con TanStack Form + `noAdjudicarSchema` de UT-04 (error inline solo si el campo fue tocado); botones «Cancelar» y «No adjudicar».
- El botón de confirmar llama al hook de su acción; **se deshabilita mientras la petición está en curso** y, al no adjudicar, mientras el motivo sea inválido. «Cancelar» o cerrar la ventana descarta lo escrito y no cambia nada (CA18); se monta solo mientras está abierta. No hay opción de notificar.
- Éxito: toast «Beca adjudicada correctamente» / «Postulación marcada como no adjudicada correctamente», cierra la ventana y avisa a la pantalla (`onDecidida`). Error: se muestra con `mapearErrorExpedientes` dentro de la ventana; `ERR_ADJ_01` y `ERR_ADJ_04` piden refrescar el expediente y los cupos (la pantalla vuelve a cargar); `ERR_FIN_01` avisa a la pantalla para volver a la lista de finalistas.
- Tests (para las dos acciones): se ve el nombre, el puntaje y los cupos; valores sin definir; motivo obligatorio y contador solo en «No adjudicar»; no deja pasar de 500; botón en curso; cancelar sin llamar a la API; éxito y cada error.

Archivos esperados (crear): `DecidirAdjudicacionDialog.tsx` y su test. (Tocar): `components/index.ts`.

## Fuera de alcance (no tocar)

- Los botones que la abren y la pantalla (UT-06).
- Un checkbox de notificar al postulante.
- Reabrir o cambiar una decisión.
- Archivar o rechazar (CU-20) y devolver (CU-19).

## Resultado esperado

La ventana registra una decisión válida y rechaza las inválidas con mensajes claros.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
