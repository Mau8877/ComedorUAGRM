# UT-CU21-FE-04 — Panel «Registrar entrega física»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-FE-04` |
| **Nombre** | Panel «Registrar entrega física» |
| **Caso de uso** | CU21-FE |
| **Depende de** | `UT-CU21-FE-01`, `UT-CU21-FE-02` |

## Antes de implementar

- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md) y [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).

## Solicitud de negocio

Cuando el finalista lleva sus documentos, el administrador deja constancia de la fecha y de lo que observó.

## Objetivo

El administrador registra o corrige la entrega del finalista seleccionado.

## Alcance

- `F/features/finalistas/components/RegistrarEntregaPanel.tsx`: con un finalista activo en la tabla muestra su estado («Pendiente» / «Recibida»), un campo de fecha de recepción (hoy por defecto, no futura) y observaciones opcionales, y el botón «Marcar como recibida». Sin finalista activo, un texto que pide elegirlo.
- Si ya hay una fecha registrada, la muestra y avisa que corregirla deja la anterior en la bitácora (A5). Errores inline (`ERR_FIN_03`) y mensaje de éxito; la tabla se actualiza al terminar (`useRegistrarEntrega`).
- Tests: fecha por defecto hoy, no deja guardar fecha futura, manda `PATCH` con lo esperado, muestra la fecha registrada y el aviso de corrección, sin finalista activo.

Archivos esperados (crear): `RegistrarEntregaPanel.tsx` y su test.

## Fuera de alcance (no tocar)

- El checklist de documentos de la maqueta.
- Devolver o archivar la postulación (CU-19, CU-20) y continuar la auditoría (CU-07).

## Resultado esperado

La entrega queda registrada y la tabla muestra «Recibida».

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
