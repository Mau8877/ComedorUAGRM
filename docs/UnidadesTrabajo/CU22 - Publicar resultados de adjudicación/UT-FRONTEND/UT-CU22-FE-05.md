# UT-CU22-FE-05 — Destinatario «Adjudicados» en los comunicados

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-FE-05` |
| **Nombre** | Destinatario «Adjudicados» en los comunicados |
| **Caso de uso** | CU22-FE |
| **Depende de** | `UT-CU22-BE-09` |

## Antes de implementar

- La ficha `UT-CU21-FE-06` (formulario de comunicados), [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md) y [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md).

## Solicitud de negocio

Después de publicar, la DUBSS necesita escribirle un comunicado solo a los adjudicados.

## Objetivo

El formulario de comunicados ofrece «Adjudicados» como destinatario.

## Alcance

- En `features/comunicados/` (CU-21): el selector de destinatarios agrega «Adjudicados» (`ADJUDICADOS`) junto a «Todos» y «Finalistas».
- El schema del formulario exige una convocatoria cuando el destinatario es `FINALISTAS` **o** `ADJUDICADOS` (mismo mensaje inline en español que ya existe para finalistas).
- Mapeo de errores: el texto de `ERR_COM_02` ya cubre ambos destinatarios; se ajusta si nombra solo a «finalistas».
- Tests: el selector muestra la opción, sin convocatoria el campo marca error, y el cuerpo enviado lleva `destinatarios: 'ADJUDICADOS'` con la convocatoria.

Archivos esperados (tocar): el formulario, su schema, el mapeo de errores de comunicados y sus tests.

## Fuera de alcance (no tocar)

- Rediseñar la pantalla de comunicados.
- Cambiar los demás destinatarios.
- Cualquier cambio en `features/resultados/`.

## Resultado esperado

La DUBSS puede enviar un comunicado solo a los adjudicados.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
