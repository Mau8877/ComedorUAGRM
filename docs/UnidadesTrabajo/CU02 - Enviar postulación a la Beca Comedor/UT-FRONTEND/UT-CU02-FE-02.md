# UT-CU02-FE-02 — Pantalla «Nueva postulación» (tres pasos), comprobante y ruta `/postular`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-FE-02` |
| **Nombre** | Pantalla «Nueva postulación» (tres pasos), comprobante y ruta `/postular` |
| **Caso de uso** | CU02-FE |
| **Depende de** | UT-CU02-FE-01 |

## Antes de implementar

- [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md) y [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)

## Solicitud de negocio

El Postulante tiene que poder completar y enviar su postulación desde el panel web, y corregir y reenviar una devuelta.

## Objetivo

La ruta `/postular` muestra el asistente de tres pasos y el comprobante, y los enlaces «Postular» y «Corregir y reenviar» ya la usan.

## Alcance

- `screens/PostularScreen.tsx`: abre (o recupera) la postulación al montar (sin duplicar el POST), paso 1 perfil, paso 2 formulario, paso 3 documentos (reusa `DocumentosPostulacionPanel` del CU16) y confirmación; un envío a la vez; errores del backend inline.
- `components/FormularioSocioeconomico.tsx`, `ComprobanteEnvio.tsx` y `valoresIniciales.ts`.
- `routes/_authenticated/postular.tsx` (solo `POSTULANTE`).
- `components/enlacesCu02.ts`: `RUTA_POSTULAR` y `RUTA_CORREGIR` pasan a `/postular` (se actualizan sus pruebas).
- Prueba: `PostularScreen.test.tsx`.

Archivos esperados (crear/tocar): los listados, `screens/index.ts` y las pruebas de `DetallePostulacionScreen` y `MisPostulacionesScreen`.

## Fuera de alcance (no tocar)

- El botón «Guardar borrador» del mockup (CU17).
- Cambiar los datos del perfil desde esta pantalla (ya existe `/perfil`).

## Resultado esperado

El Postulante envía su postulación y ve su comprobante.

## Validación

1. `pnpm exec vitest run src/features/postulaciones` en verde, y `pnpm lint` y `pnpm exec tsc -b` sin errores.
2. Con el stack: «Postular» → paso 1 con perfil → paso 2 con un campo vacío marca error → paso 3 sin un documento obligatorio muestra «Falta completar documentos: …» → con todo, aparece el comprobante.
