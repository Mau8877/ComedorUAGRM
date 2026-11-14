# UT-CU15-FE-01 — Schemas de Zod y tipos del CU

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-FE-01` |
| **Nombre** | Schemas de Zod y tipos del CU |
| **Caso de uso** | CU15-FE |
| **Depende de** | — |

## Antes de implementar

- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md)
- [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)
- Como referencia: `F/features/auth/schemas/registroSchema.ts`

## Solicitud de negocio

Los formularios deben avisar al instante, campo por campo, con las mismas reglas que aplica el backend.

## Objetivo

Existen los schemas y tipos del CU, reutilizados para validar los formularios y tipar lo que se envía a la API.

## Alcance

- `F/features/auth/schemas/recuperarContrasenaSchema.ts`: `recuperarContrasenaSchema` (`correo` obligatorio, hasta 150, formato) y su tipo.
- `F/features/auth/schemas/restablecerContrasenaSchema.ts`: `contrasena` (obligatoria, de 6 a 72) y `confirmarContrasena` (obligatoria, `refine` de que coincidan, error en `confirmarContrasena`); `aRestablecerRequest(valores, token)` que arma `{ token, contrasena, confirmacionContrasena }`.
- `F/features/auth/schemas/index.ts`: exporta los nuevos.
- `F/features/perfil/schemas/actualizarPerfilSchema.ts`: `correo`, `telefono` (opcional en el tipo; la pantalla lo exige solo si es Postulante) y `contrasenaActual` opcional, con un `superRefine` que exige `contrasenaActual` solo cuando se pasa el correo original como contexto (función fábrica `crearActualizarPerfilSchema({ correoActual, esPostulante })`). `aActualizarPerfilRequest(valores, correoActual)` envía solo lo que cambió.
- `F/features/perfil/schemas/cambiarContrasenaSchema.ts`: `contrasenaActual`, `contrasenaNueva` (6 a 72), `confirmarContrasena` (igual a la nueva); `aCambiarContrasenaRequest` renombra la confirmación a `confirmacionContrasena`.
- `F/features/perfil/types/index.ts`: `Perfil` y `PerfilPostulante` (campos de `PerfilResponse`) y `index.ts` de la feature (barrel, vacío de pantallas por ahora).
- Mensajes en español, los mismos del backend. Tests en `features/auth/tests/` y `features/perfil/tests/`: cada regla (contraseña de 5, 6 y 73 caracteres; confirmación distinta; correo vacío o inválido), que `contrasenaActual` es obligatoria solo con el correo cambiado, y que `aActualizarPerfilRequest` omite lo que no cambió y nunca incluye el CI.

Archivos esperados (crear): los cuatro schemas, `types/index.ts`, el barrel de `perfil` y los tests. (Tocar): `features/auth/schemas/index.ts`.

## Fuera de alcance (no tocar)

- Llamadas a la API y pantallas (UT-02 a UT-07).
- Cambiar `registroSchema` o `loginSchema`.

## Resultado esperado

Los formularios pueden validar y armar sus requests con una sola fuente de reglas.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
