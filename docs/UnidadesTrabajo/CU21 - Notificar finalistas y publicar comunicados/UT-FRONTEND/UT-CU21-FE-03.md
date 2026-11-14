# UT-CU21-FE-03 — Diálogo «Notificar seleccionados»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-FE-03` |
| **Nombre** | Diálogo «Notificar seleccionados» (plazo, lugar y horario, documentos) |
| **Caso de uso** | CU21-FE |
| **Depende de** | `UT-CU21-FE-01`, `UT-CU21-FE-02` |

## Antes de implementar

- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md) (errores inline solo en campos tocados, `onBlur`).
- Como referencia: `F/features/priorizacion/components/DetallePostulacionDialog.tsx` y el uso de `AlertDialog` en el panel.

## Solicitud de negocio

El administrador define hasta cuándo, dónde y qué documentos deben entregar los finalistas elegidos, y confirma el envío.

## Objetivo

El administrador notifica a los finalistas seleccionados y ve cuántos avisos salieron y cuántos fallaron.

## Alcance

- `F/features/finalistas/components/NotificarFinalistasDialog.tsx`: formulario TanStack Form + `notificarFinalistasSchema` (plazo, lugar y horario, documentos), un resumen «Se enviará a N finalistas» y la confirmación con `AlertDialog` antes de enviar (A6: errores inline y no se envía nada si falta algo).
- Botón «Notificar seleccionados» (deshabilitado sin selección), arriba de la tabla.
- Tras enviar: resultado «N enviados, M fallidos» (`useNotificarFinalistas`); con fallidos, el resultado explica que la tabla tiene el botón «Seleccionar fallidos», que deja marcados solo a esos para reintentar (A2). Errores con el mensaje de negocio (`ERR_FIN_04`, `ERR_FIN_01`).
- Tests: no deja enviar con campos vacíos ni con plazo de hoy, pide confirmación, manda lo esperado, muestra el resultado; y, en la pantalla, «Seleccionar fallidos» deja marcados solo a los fallidos.

Archivos esperados (crear): `NotificarFinalistasDialog.tsx` y su test.

## Fuera de alcance (no tocar)

- El envío real (push, correo): lo hace el backend con CU-EXTRA-01.
- El estado «Fallida» se calcula en el servidor.

## Resultado esperado

El administrador avisa a los finalistas elegidos y puede reintentar a los fallidos.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
