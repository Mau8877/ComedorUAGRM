# UT-CU13-FE-06 — Abrir, cerrar y eliminar una convocatoria

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU13-FE-06` |
| **Nombre** | Abrir, cerrar y eliminar una convocatoria |
| **Caso de uso** | CU13-FE |
| **Depende de** | `UT-CU13-FE-02`, `UT-CU13-FE-04` |

## Antes de implementar

- `F/features/usuarios/components/CambiarEstadoUsuarioDialog.tsx` y `F/components/ui/modal-destructive.tsx` (patrón de confirmación)
- `F/features/usuarios/components/AlertaUsuarios.tsx` (patrón de aviso de error)
- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md#invalidación)

## Solicitud de negocio

El Administrador abre la convocatoria para que reciba postulaciones, la cierra cuando termina el periodo, o quita una creada por error (A1, A3, A4, A7; CA05 a CA07 y CA17).

## Objetivo

Desde la lista se puede abrir, cerrar y eliminar una convocatoria, con confirmación y con el mensaje correcto cuando el backend rechaza la operación.

## Alcance

- `F/features/convocatorias/components/CambiarEstadoConvocatoriaDialog.tsx`: confirmación para **abrir** («Al abrirla, empezará a recibir postulaciones») y para **cerrar** («Dejará de recibir postulaciones; los borradores no enviados quedarán bloqueados»; usa `modal-destructive`). Muestra el nombre y la gestión de la convocatoria. Llama a `useAbrirConvocatoria` o `useCerrarConvocatoria`.
- `F/features/convocatorias/components/EliminarConvocatoriaDialog.tsx`: confirmación destructiva que explica que solo se quitan convocatorias en borrador y sin postulaciones. Llama a `useEliminarConvocatoria`.
- Errores con `mapearErrorConvocatorias`: `ERR_CV_07` se muestra como aviso «Ya hay una convocatoria abierta en esa gestión. Ciérrala antes de abrir esta» (A1); `ERR_CV_08`, `ERR_CV_09` y `ERR_CV_10` («ya cambió de estado» / «tiene postulaciones: solo se puede cerrar», A7) y los `ERR_CV_01` se muestran como aviso y refrescan la lista (la convocatoria puede haber cambiado desde otra sesión o por el cierre automático); error de red con reintentar. El diálogo no se cierra si falla.
- Éxito: cierra el diálogo, toast («Convocatoria abierta» / «Convocatoria cerrada» / «Convocatoria eliminada») y la lista se refresca por la invalidación del hook.
- Se conecta en `ConvocatoriasScreen`: los callbacks «Abrir», «Cerrar» y «Eliminar» de las acciones de fila.
- Se exportan los dos componentes desde el barrel de `components/`.
- Tests: cada acción pide confirmación antes de llamar al hook; confirmar llama a la mutación correcta con el código de la convocatoria; cancelar no llama a nada; `ERR_CV_07` muestra su mensaje; `ERR_CV_10` y `ERR_CV_08` refrescan la lista; éxito cierra el diálogo y avisa; la fila de una `CERRADA` no ofrece ninguna de las tres acciones.

Archivos esperados (crear/tocar): `CambiarEstadoConvocatoriaDialog.tsx`, `EliminarConvocatoriaDialog.tsx`, `ConvocatoriasScreen.tsx`, barrel y tests.

## Fuera de alcance (no tocar)

- Reabrir una convocatoria cerrada.
- El cierre automático (lo hace el backend; la lista solo lo refleja al refrescar).
- Notificar a los postulantes de la apertura o el cierre.
- El formulario de edición (`UT-CU13-FE-05`).

## Resultado esperado

El Administrador abre, cierra y elimina convocatorias con confirmación y entiende por qué una operación no se puede hacer.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
4. Con el stack levantado: abrir una convocatoria en borrador la pasa a «Abierta»; abrir otra de la misma gestión muestra el aviso de que ya hay una abierta; eliminar una en borrador la quita de la lista.
