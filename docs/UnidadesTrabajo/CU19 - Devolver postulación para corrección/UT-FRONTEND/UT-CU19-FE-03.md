# UT-CU19-FE-03 — Botón en el expediente y estado «Devuelta»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU19-FE-03` |
| **Nombre** | Botón en el expediente y estado «Devuelta» |
| **Caso de uso** | CU19-FE |
| **Depende de** | `UT-CU19-FE-02` |

## Antes de implementar

- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md), [`ESTADO_GLOBAL_FRONTEND.md`](../../../../.claude/rules/frontend/ESTADO_GLOBAL_FRONTEND.md), [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).
- Maqueta `107` (botón «Devolver para corrección») y `F/features/expedientes/screens/ExpedienteScreen.tsx`.

## Solicitud de negocio

El administrador llega a la devolución desde el expediente y ve que la postulación quedó devuelta.

## Objetivo

El expediente ofrece «Devolver para corrección» y refleja el estado «Devuelta».

## Alcance

- `ExpedienteScreen`: botón «Devolver para corrección» junto a «Notificar»; abre `DevolverPostulacionDialog` (se monta solo mientras está abierto). Al devolver, refresca el expediente.
- Deshabilitado con el motivo visible cuando la postulación **ya no está «calificada»** («La postulación ya está devuelta» si es `devuelta`, o «La postulación ya no está calificada»). Si no hay ninguna auditoría registrada el botón también se deshabilita con «Primero registra la auditoría del expediente». Tras un reenvío con auditorías viejas el botón queda habilitado y el backend decide (`ERR_DEV_02`, con su mensaje).
- `ExpedienteResumen` (o el encabezado del expediente) muestra una insignia «Devuelta» cuando `estadoPostulacion` es `devuelta`; el formulario de auditoría ya queda bloqueado con su motivo (`motivoBloqueoAuditoria`).
- `comoFinalista(expediente)` pasa `estadoPostulacion` al `Finalista` que usan los paneles del CU-21.
- Tests de pantalla: botón habilitado, deshabilitado por estado y por falta de auditoría, flujo completo de devolución con la API simulada y la insignia «Devuelta».

Archivos esperados (tocar): `ExpedienteScreen.tsx`, `ExpedienteResumen.tsx` y sus tests.

## Fuera de alcance (no tocar)

- Cambiar el estado de la postulación desde el cliente.
- Un botón de archivar o rechazar (CU-20).
- Cambios en el formulario de auditoría (CU-07).
- La lista de finalistas (UT-04).

## Resultado esperado

Desde el expediente se devuelve la postulación y la pantalla queda en «Devuelta».

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
