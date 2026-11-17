# UT-CU26-FE-03 — Panel de reprocesamiento y su confirmación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-FE-03` |
| **Nombre** | Panel de reprocesamiento y su confirmación |
| **Caso de uso** | CU26-FRONTEND |
| **Depende de** | UT-CU26-FE-01, UT-CU26-FE-02 |

## Antes de implementar

[`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) y
[`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md).

## Solicitud de negocio

Después de guardar una versión, el Administrador quiere recalcular una convocatoria, saber cuánto falta y qué falló.

## Objetivo

El panel «Reprocesar puntajes» y el diálogo «¿Reprocesar los puntajes?» del mockup 146, con avance en vivo.

## Alcance

- `ReprocesarPanel`: selector de convocatoria (`useConvocatorias` del CU13), resumen (calificadas y por recalcular), aviso de la lista priorizada, motivos de bloqueo (sin versión, cambios sin guardar, resultados publicados, lote en curso, nada por recalcular), avance (`<progress>`, procesadas, con error), mensajes de terminado, con fallidas e interrumpido, y «Ver la lista priorizada».
- `ReprocesarDialog` (`ModalLayout`): confirmar o cancelar; errores de negocio que piden refrescar avisan y cierran.
- Pruebas (en `ParametrosModeloScreen.test.tsx`): resumen, confirmar, cancelar, avance, fallidas, interrumpido, publicados, lote en curso, nada por recalcular, cambios sin guardar y `ERR_MOD_04`.

Archivos esperados (crear/tocar): `ReprocesarPanel.tsx`, `ReprocesarDialog.tsx`, `components/index.ts`, el test.

## Fuera de alcance (no tocar)

- Cancelar un lote ya iniciado.
- Reprocesar una postulación suelta.
- La pantalla de la lista priorizada (CU06): solo se enlaza.

## Resultado esperado

Confirmar el diálogo arranca el lote y el panel muestra «X de Y procesadas» hasta terminar.

## Validación

1. `pnpm exec vitest run src/features/preseleccion` en verde.
2. Con el stack levantado y la IA (B3) disponible: reprocesar una convocatoria, ver la barra avanzar, y que un segundo intento inmediato quede bloqueado con el aviso.
