# UT-CU05-FE-03 — Ventana «Seleccionar finalistas»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-FE-03` |
| **Nombre** | Ventana «Seleccionar finalistas» |
| **Caso de uso** | CU05-FE |
| **Depende de** | `UT-CU05-FE-02` |

## Antes de implementar

- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md), [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).
- Como referencia: `F/features/finalistas/components/NotificarFinalistasDialog.tsx` (CU-21) y `RegistrarEntregaPanel.tsx`.

## Solicitud de negocio

El Administrador elige cuántos de los mejor puntuados pasan a ser finalistas; la convocatoria ya sugiere un número.

## Objetivo

El Administrador puede confirmar o ajustar la cantidad de finalistas y seleccionarlos.

## Alcance

- `F/features/adjudicacion/components/SeleccionarFinalistasDialog.tsx`: campo **Cantidad** con TanStack Form + `seleccionSchema` de UT-01 (error inline solo si el campo fue tocado), que **propone** `cantidadFinalistas` del panel (si no hay, queda vacío y el Administrador debe escribirla); no se propone un número mayor que las postulaciones sin seleccionar (`calificadasSinSeleccionar`). Texto de apoyo con los cupos disponibles y una nota de que elegir **más finalistas que cupos** es válido (los extra son suplentes) y de que ajustar la cantidad **no cambia** la configurada en la convocatoria.
- Botones «Cancelar» y «Seleccionar». Confirmar llama a `useSeleccionarFinalistas`; **se deshabilita mientras la petición está en curso** y mientras la cantidad sea inválida. «Cancelar» o cerrar descarta lo escrito y no cambia nada; se monta solo mientras está abierta.
- Éxito: toast «N finalistas seleccionados correctamente», cierra la ventana y la pantalla actualiza el panel. Error: se muestra con `mapearErrorAdjudicacion` dentro de la ventana; `ERR_ADJ_01` o `ERR_ADJ_05` piden refrescar el panel.
- `AdjudicacionScreen` gana el botón «Seleccionar finalistas» (el espacio de UT-02), **deshabilitado con su motivo visible** cuando `decisionesHabilitadas` es falso o no hay postulaciones sin seleccionar. Tras seleccionar, muestra el acceso a «Ver finalistas» para auditar.
- Tests (ventana): propone la configurada, vacío sin configurada, no propone más que las disponibles, obligatoria/entera/positiva, ajustar sin cambiar la configurada, en curso, cancelar sin llamar a la API, éxito y cada error. Tests (pantalla): botón habilitado y deshabilitado con motivo.

Archivos esperados (crear): `SeleccionarFinalistasDialog.tsx` y su test. (Tocar): `AdjudicacionScreen.tsx`, `components/index.ts` y el test de la pantalla.

## Fuera de alcance (no tocar)

- Auditar, notificar o registrar la entrega física de los seleccionados (CU-07 y CU-21).
- Editar la cantidad de finalistas **de la convocatoria** (CU-13).
- Elegir finalistas a mano, uno por uno, de la lista priorizada (el CU selecciona por orden de puntaje).
- Mostrar la cantidad configurada fuera de esta pantalla del Administrador.

## Resultado esperado

El Administrador selecciona a los mejores postulantes en orden, con la cantidad sugerida o la que ajuste, y los ve listos para auditar.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
