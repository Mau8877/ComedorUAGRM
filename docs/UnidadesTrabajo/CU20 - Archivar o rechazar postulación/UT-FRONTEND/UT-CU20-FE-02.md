# UT-CU20-FE-02 — Ventana «Archivar» / «Rechazar»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU20-FE-02` |
| **Nombre** | Ventana «Archivar» / «Rechazar» |
| **Caso de uso** | CU20-FE |
| **Depende de** | `UT-CU20-FE-01` |

## Antes de implementar

- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md), [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).
- Como referencia: `F/features/expedientes/components/DevolverPostulacionDialog.tsx` (CU-19).

## Solicitud de negocio

El administrador escribe por qué cierra la postulación y confirma.

## Objetivo

El administrador puede escribir el motivo y confirmar o cancelar, tanto para archivar como para rechazar.

## Alcance

- `F/features/expedientes/components/CerrarPostulacionDialog.tsx` con la prop `accion` (`'archivar' | 'rechazar'`): título y botón según la acción («Archivar postulación» / «Rechazar postulación»), el nombre del postulante y la convocatoria, un aviso de que la decisión es definitiva para esa gestión, el campo **Motivo** (obligatorio, `Textarea`, **contador de 500 caracteres**) con TanStack Form + el schema de UT-01 (error inline solo si el campo fue tocado) y los botones «Cancelar» y «Archivar» / «Rechazar».
- El botón de confirmar llama al hook de su acción; **se deshabilita mientras la petición está en curso** y mientras el motivo sea inválido. «Cancelar» o cerrar la ventana descarta lo escrito y no cambia nada; se monta solo mientras está abierta.
- Éxito: toast «Postulación archivada correctamente» / «Postulación rechazada correctamente», cierra la ventana y avisa a la pantalla (`onCerrada`). Error: se muestra con `mapearErrorExpedientes` dentro de la ventana; `ERR_ARC_01` pide refrescar el expediente; `ERR_FIN_01` avisa a la pantalla para volver a la lista de finalistas.
- **No** hay opción de no notificar.
- Tests (para las dos acciones): motivo obligatorio y contador, no deja pasar de 500, botón en curso, cancelar sin llamar a la API, éxito y cada error.

Archivos esperados (crear): `CerrarPostulacionDialog.tsx` y su test. (Tocar): `components/index.ts`.

## Fuera de alcance (no tocar)

- Los botones que la abren y la pantalla (UT-03).
- Un checkbox de notificar.
- Devolver para corrección (CU-19).

## Resultado esperado

La ventana registra un cierre válido y rechaza los inválidos con mensajes claros.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
