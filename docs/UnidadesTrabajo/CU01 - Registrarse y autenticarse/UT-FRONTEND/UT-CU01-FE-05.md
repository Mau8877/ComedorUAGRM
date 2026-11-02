# UT-CU01-FE-05 — Acciones de registro, inicio y cierre de sesión con mapeo de errores

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-FE-05` |
| **Nombre** | Acciones de registro, inicio y cierre de sesión con mapeo de errores |
| **Caso de uso** | CU01-FE |
| **Depende de** | `UT-CU01-FE-02`, `UT-CU01-FE-03`, `UT-CU01-FE-04` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md)
- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)

## Solicitud de negocio

El registro, el login y el cierre de sesión combinan Firebase y el backend; esa lógica y la traducción de errores deben vivir en un solo lugar, probada.

## Objetivo

Existen `useRegistro`, `useLogin` y `useCerrarSesion` (mutations) que implementan los flujos A, B y C, devuelven errores ya traducidos a mensajes y existe `rutaInicioPorRol`.

## Alcance

- `src/features/auth/api/mapearError.ts`: función pura que recibe un error de Firebase o de la API y devuelve `{ mensaje, campo? }` según la tabla de decisiones del README (credenciales → mensaje genérico, bloqueo, cuenta desactivada, red) y para `ERR_US_01`/`02`/`03` el campo en conflicto (`ci`, `correo`, `registroUniversitario`).
- `useRegistro.ts`: flujo A del README (crear cuenta en Firebase, `POST /auth/registro`, recuperación si el correo ya existe en Firebase, no cerrar sesión ante un `409`). Evita el doble envío mientras está en curso (A7).
- `useLogin.ts`: flujo B (login en Firebase, `POST /auth/sesion`; ante `403`/`401` de la API cierra la sesión de Firebase). Al terminar invalida/precarga `authKeys.usuarioActual` y devuelve el `Usuario` para decidir el destino.
- `useCerrarSesion.ts`: `signOut` de Firebase, `queryClient.clear()` y navegación a `/login`; solo cierra este dispositivo (no revoca otras sesiones) y no muestra error si la sesión ya había expirado (A10).
- `src/features/auth/rutaInicioPorRol.ts`: `rutaInicioPorRol(rol)` → `/panel` para los 4 roles por ahora (rol desconocido → `/login`); es el único lugar a actualizar cuando cada rol tenga su módulo.
- Tests unitarios con Firebase y `apiClient` simulados: cada fila de la tabla de errores; `rutaInicioPorRol` con los 4 roles y uno desconocido; registro feliz; correo existente en Firebase con la misma contraseña → continúa; con otra contraseña → "correo ya registrado"; `409` marca el campo y no hace `signOut`; ninguna contraseña ni token aparece en `console`.

Archivos esperados (crear): `mapearError.ts`, `useRegistro.ts`, `useLogin.ts`, `useCerrarSesion.ts`, `rutaInicioPorRol.ts` y tests.

## Fuera de alcance (no tocar)

- Las pantallas (`UT-CU01-FE-06`, `07`).
- Reglas de contraseña o de bloqueo (de Firebase).
- Recuperar contraseña (CU15).

## Resultado esperado

La lógica del CU01 en el cliente está completa y probada; las pantallas solo la invocan.

## Validación

1. `pnpm test` pasa con todos los escenarios.
2. `pnpm lint` y `pnpm build` sin errores.
3. Revisar que ningún `console.log` imprime contraseñas ni tokens.
