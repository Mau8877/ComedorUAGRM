# UT-CU23-FE-03 — Diálogos de baja, suspensión y reemplazo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU23-FE-03` |
| **Nombre** | Diálogos de baja, suspensión y reemplazo |
| **Caso de uso** | CU23-FRONTEND |
| **Depende de** | UT-CU23-FE-01, UT-CU23-FE-02 |

## Antes de implementar

[`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md)
(un solo schema, error inline solo en campos tocados).

## Solicitud de negocio

Cambiar el estado de un becado exige pedir el motivo y confirmar antes de modificar el padrón.

## Objetivo

Los diálogos que registran una baja, una suspensión o un reemplazo desde la tabla del padrón.

## Alcance

- `schemas/motivoSchema.ts` (Zod): motivo obligatorio, máx. 500 caracteres, en español.
- `components/MotivoBecaDialog.tsx`: motivo con error inline; sirve para «Dar de baja» y «Suspender»; cancelar no
  cambia nada (A5); muestra el error del backend si falla.
- `components/ReemplazoDialog.tsx`: pide el candidato al abrir, muestra a quién se propone (nombre, CI, carrera,
  posición y puntaje) y confirma; si no hay candidato informa y no ofrece confirmar (A4).
- Acciones por fila en `PadronScreen`: baja y suspender solo en vigentes; reemplazar solo en bajas.

Archivos esperados (crear/tocar): `motivoSchema.ts`, `MotivoBecaDialog.tsx`, `ReemplazoDialog.tsx`, `PadronScreen.tsx`.

## Fuera de alcance (no tocar)

- Reactivar una suspensión y el historial del becado.

## Resultado esperado

El Administrador da de baja, suspende o reemplaza con confirmación y ve el padrón actualizado.

## Validación

1. `pnpm exec tsc -b`, `pnpm exec eslint src/features/becas` sin errores.
2. Manual: dar de baja con y sin motivo; reemplazar una baja y ver al nuevo becado vigente.
