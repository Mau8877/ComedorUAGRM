# UT-CU01-FE-02 — Schemas de Zod de registro y login

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-FE-02` |
| **Nombre** | Schemas de Zod de registro y login |
| **Caso de uso** | CU01-FE |
| **Depende de** | `UT-CU01-FE-01` |

## Antes de implementar

- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md)
- [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)

## Solicitud de negocio

Los formularios deben avisar al usuario, campo por campo y en español, qué dato está mal antes de enviarlo.

## Objetivo

Existen `registroSchema` y `loginSchema` con sus tipos y mensajes, y el tipo del request al backend derivado del schema.

## Alcance

- `src/features/auth/schemas/registroSchema.ts`: campos y reglas del contrato del README; `contrasena` mínimo 6 caracteres **sin** exigir mayúsculas ni números; `confirmarContrasena` debe coincidir (error en ese campo); `correo` con formato válido; mensajes como "El CI es obligatorio", "La contraseña debe tener al menos 6 caracteres", "Las contraseñas no coinciden". Exporta `RegistroFormValues` (`z.infer`) y una función que devuelve el `RegistroRequest` para el backend: todos los campos del formulario con `carreraCodigo`, `correo`, `contrasena` y `confirmacionContrasena` (el backend vuelve a validarlos; el campo del formulario `confirmarContrasena` se envía como `confirmacionContrasena`).
- `src/features/auth/schemas/loginSchema.ts`: `correo` y `contrasena`.
- `src/features/auth/schemas/index.ts`.
- Tests en `src/features/auth/tests/`: contraseña de 5 caracteres falla y de 6 pasa (sin mayúscula ni número); confirmación distinta falla en `confirmarContrasena`; campos vacíos fallan; el request al backend lleva `correo`, `contrasena` y `confirmacionContrasena`.

Archivos esperados (crear): los tres de `schemas/` y los tests.

## Fuera de alcance (no tocar)

- Formato del CI (por definir con la DUBSS): solo no vacío. El registro universitario sí se valida: exactamente 9 dígitos y sin cero inicial (`/^[1-9]\d{8}$/`).
- Los formularios (`UT-CU01-FE-06` y `07`).

## Resultado esperado

Las reglas de validación del cliente (CA04 y CA05) están definidas y probadas.

## Validación

1. `pnpm test` pasa con los casos indicados.
2. `pnpm lint` y `pnpm build` sin errores.
