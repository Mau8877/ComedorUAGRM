# UT-CU14-FE-01 — Tipos, roles del personal y schemas de Zod

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-FE-01` |
| **Nombre** | Tipos, roles del personal y schemas de Zod |
| **Caso de uso** | CU14-FE |
| **Depende de** | — |

## Antes de implementar

- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md)
- [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)

## Solicitud de negocio

Los formularios de la gestión de cuentas tienen que validar en el cliente las mismas reglas que el backend, y todos deben hablar de los mismos tres roles del personal.

## Objetivo

Existen los tipos de las respuestas, la lista de los roles del personal y los tres schemas de Zod de los formularios.

## Alcance

- `F/features/usuarios/types/index.ts`: `UsuarioPersonal` (`codigo`, `nombres`, `apellidos`, `ci`, `correo`, `rol`, `activo`) y `UsuarioCreado` (lo mismo más `contrasenaTemporal`), con los nombres exactos del backend.
- `F/features/usuarios/roles.ts`: `ROLES_PERSONAL` (`ADMINISTRADOR_DUBSS`, `DIRECTIVO`, `PERSONAL_COMEDOR`) y el tipo `RolPersonal`. Las etiquetas se toman de `etiquetaRol` de `features/auth` (no se duplican).
- `F/features/usuarios/schemas/usuarioPersonalSchema.ts`: `crearUsuarioPersonalSchema` (`nombres` y `apellidos` obligatorios, hasta 100 caracteres; `ci` obligatorio, hasta 20; `correo` obligatorio, hasta 150 y con formato de correo; `rol` uno de `ROLES_PERSONAL`), `editarUsuarioPersonalSchema` (`nombres`, `apellidos`, `correo`) y `cambiarRolSchema` (`rol`). Mensajes en español, sin duplicar los tipos de request (se infieren con `z.infer`). El formato del CI sigue «por definir con la DUBSS»: solo no vacío y largo máximo, igual que el registro.
- `F/features/usuarios/schemas/index.ts` y `F/features/usuarios/index.ts` (barrel) exportan lo anterior.
- Tests en `F/features/usuarios/tests/`: cada regla de cada schema (campo vacío, correo inválido, largos máximos, rol fuera del personal, `POSTULANTE` rechazado).

Archivos esperados (crear): los de arriba y su test.

## Fuera de alcance (no tocar)

- Los formularios que usan los schemas (UT 05 y 06).
- Las llamadas a la API (`UT-CU14-FE-02`).
- Los schemas de `features/auth`.

## Resultado esperado

Las UT siguientes importan tipos, roles y schemas sin redefinirlos.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
