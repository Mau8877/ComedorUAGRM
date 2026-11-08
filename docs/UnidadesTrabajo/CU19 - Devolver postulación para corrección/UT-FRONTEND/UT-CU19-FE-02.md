# UT-CU19-FE-02 — Ventana «Devolver para corrección»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU19-FE-02` |
| **Nombre** | Ventana «Devolver para corrección» |
| **Caso de uso** | CU19-FE |
| **Depende de** | `UT-CU19-FE-01` |

## Antes de implementar

- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md), [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).
- Maqueta `113` (sin el cuadro «Notificar por push» ni la lista «Postulaciones» de fondo) y `F/features/finalistas/components/NotificarFinalistasDialog.tsx` como referencia de un diálogo con formulario.

## Solicitud de negocio

El administrador escribe qué debe corregir el postulante y confirma.

## Objetivo

El administrador puede escribir el motivo y confirmar o cancelar.

## Alcance

- `F/features/expedientes/components/DevolverPostulacionDialog.tsx`: ventana con el nombre del postulante y la convocatoria, el campo **Motivo** (obligatorio, `Textarea` de shadcn, **contador de 500 caracteres**) con TanStack Form + el schema de UT-01 (error inline solo si el campo fue tocado), y los botones «Cancelar» y «Devolver».
- «Devolver» llama a `useDevolverPostulacion`; **se deshabilita mientras la petición está en curso** (así se evita el doble clic) y también mientras el motivo sea inválido. «Cancelar» o cerrar la ventana descarta lo escrito y no cambia nada; se monta solo mientras está abierta.
- Éxito: toast «Postulación devuelta correctamente», cierra la ventana y avisa a la pantalla (`onDevuelta`). Error: se muestra con `mapearErrorExpedientes` dentro de la ventana; `ERR_DEV_01` pide refrescar el expediente; `ERR_FIN_01` avisa a la pantalla (el finalista desapareció).
- **No** hay opción de no notificar: se avisa siempre.
- Tests: motivo obligatorio y contador, no deja pasar de 500, el botón en curso, cancelar sin llamar a la API, éxito y cada error.

Archivos esperados (crear): `DevolverPostulacionDialog.tsx` y su test. (Tocar): `components/index.ts`.

## Fuera de alcance (no tocar)

- El botón que la abre y la pantalla (UT-03).
- Un checkbox «Notificar al postulante por push» (siempre se avisa).
- Elegir entre archivar o devolver (CU-20).

## Resultado esperado

La ventana registra una devolución válida y rechaza las inválidas con mensajes claros.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
