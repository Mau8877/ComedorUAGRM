# UT-CU17-FE-01 — «Guardar borrador» en el asistente y tarjeta «Mi borrador»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU17-FE-01` |
| **Nombre** | «Guardar borrador» en el asistente y tarjeta «Mi borrador» |
| **Caso de uso** | CU17-FE |
| **Depende de** | UT-CU17-BE-02 |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (keys y mutaciones), [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md) (error inline solo si el campo se tocó) y [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)

## Solicitud de negocio

El postulante quiere salir y volver a su postulación sin perder lo escrito, y ver cuánto le falta.

## Objetivo

El asistente de «Nueva postulación» guarda el borrador y avisa la hora; «Mis postulaciones» muestra el borrador con lo que falta.

## Alcance

- `types/envio.ts`: `GuardarBorradorRequest`, `BorradorGuardado`, `BorradorResumen` y `guardadoEn` en `RespuestaSocioeconomica`.
- `schemas/respuestasSocioeconomicasSchema.ts`: `aBorrador(valores)` (vacío → `null`, escrito → misma regla por campo) y `CAMPOS_SOCIOECONOMICOS`.
- `api/postulacionesApi.ts`, `api/hooks.ts`, `api/keys.ts`: `guardarBorradorApi`, `miBorradorApi`, `useGuardarBorrador`, `useMiBorrador`, `miBorrador()`.
- `components/formatos.ts`: `formatearHora`.
- `components/FormularioSocioeconomico.tsx`: botón «Guardar borrador» (props opcionales `onGuardarBorrador`, `guardando`, `guardadoEn`), aviso «✓ Borrador guardado · 14:32», lista de datos a corregir y el texto «Puedes salir y continuar después…».
- `screens/PostularScreen.tsx`: guarda desde el paso 2 y el 3, mantiene `guardadoEn` y muestra el error del backend.
- `components/BorradorPendienteCard.tsx` (nuevo) y `screens/MisPostulacionesScreen.tsx`: tarjeta «Mi borrador» con «Continuar» a `/postular`.
- Tests: `PostularScreen.test.tsx` (4 nuevos), `BorradorPendienteCard.test.tsx`, `respuestasSocioeconomicasSchema.test.ts` (`aBorrador`) y `envioApi.test.tsx`.

Archivos esperados (crear/tocar): los listados arriba.

## Fuera de alcance (no tocar)

- El botón «Eliminar» del borrador, el guardado automático y la advertencia al salir con cambios.
- El esquema estricto del envío (`respuestasSocioeconomicasSchema`) y el panel de documentos (CU16).
- La app móvil.

## Resultado esperado

Guardar con el formulario a medias responde con la hora; al volver a «Nueva postulación» aparecen los datos y la hora; «Mis postulaciones» muestra la tarjeta mientras haya un borrador en la convocatoria abierta.

## Validación

1. `pnpm exec vitest run src/features/postulaciones`, `pnpm exec tsc -b` y `pnpm exec eslint src/features/postulaciones` en verde.
2. Con el stack: abrir `/postular`, escribir solo los ingresos, «Guardar borrador» → «Borrador guardado · hh:mm»; recargar y volver a `/postular` → los ingresos siguen ahí.
3. En `/mis-postulaciones` aparece la tarjeta «Borrador» con «Faltan 5 campos…»; tras enviar la postulación desaparece.
