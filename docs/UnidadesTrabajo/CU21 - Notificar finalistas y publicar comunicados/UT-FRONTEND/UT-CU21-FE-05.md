# UT-CU21-FE-05 — Formulario de comunicado con vista previa y confirmación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-FE-05` |
| **Nombre** | Formulario de comunicado con vista previa y confirmación |
| **Caso de uso** | CU21-FE |
| **Depende de** | `UT-CU21-FE-01` |

## Antes de implementar

- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md) y [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) (barrel de la feature).

## Solicitud de negocio

La DUBSS escribe un comunicado, revisa cómo queda y lo publica a todos los postulantes o a los finalistas.

## Objetivo

El administrador publica un comunicado y ve a cuántos llegó.

## Alcance

- `F/features/comunicados/components/ComunicadoForm.tsx`: título, contenido y «Dirigido a» (`Todos los postulantes` / `Finalistas`) con `comunicadoSchema`; la convocatoria elegida en la pantalla se manda como `convocatoriaId` (obligatoria para `Finalistas`). Errores inline (A6).
- Vista previa («Así lo verán los destinatarios») y confirmación con `AlertDialog` antes de publicar; al terminar, «Se envió a N destinatarios» y el formulario se limpia. Errores de negocio con su mensaje (`ERR_COM_01`, `ERR_COM_02`).
- Sin «Guardar borrador» ni «Publicación programada».
- Tests: validaciones, vista previa, confirmación, `POST` con lo esperado, mensaje con el total y errores del backend.

Archivos esperados (crear): `ComunicadoForm.tsx` y su test.

## Fuera de alcance (no tocar)

- Opción `Adjudicados`, borrador y publicación programada.
- Editar o borrar un comunicado publicado.

## Resultado esperado

El administrador publica un comunicado tras revisarlo y confirma.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
