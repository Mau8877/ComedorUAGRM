# UT-CU01-FE-07 — Pantalla de inicio de sesión (/login)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-FE-07` |
| **Nombre** | Pantalla de inicio de sesión (/login) |
| **Caso de uso** | CU01-FE |
| **Depende de** | `UT-CU01-FE-02`, `UT-CU01-FE-05` |

## Antes de implementar

- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md)
- [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)
- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)

## Solicitud de negocio

Cualquier usuario con cuenta debe poder entrar al sistema con su correo y contraseña.

## Objetivo

La ruta `/login` (hoy un placeholder) muestra el formulario real de inicio de sesión.

## Alcance

- `src/features/auth/screens/LoginScreen.tsx`: `PublicHeader` + tarjeta con correo y contraseña (TanStack Form + `loginSchema`), botón «Iniciar sesión» con estado de carga, y enlace «Crear cuenta» hacia `/registro`.
- Mensajes de la tabla del README: credenciales incorrectas (siempre el mismo texto), bloqueo temporal, cuenta desactivada, error de red con reintento. Los mensajes de error van en una alerta visible sobre el formulario (no inline por campo, para no revelar cuál dato falló).
- Al terminar bien: `navigate` a `rutaInicioPorRol(rol)` (`UT-CU01-FE-05`).
- `src/routes/login.tsx: usa `LoginScreen` en lugar del placeholder; con sesión activa redirige al módulo (se completa en `UT-CU01-FE-08`).
- No incluye el enlace «¿Olvidó su contraseña?» (CU15).
- Test de componente: errores de validación solo tras tocar el campo; credenciales incorrectas muestra el mensaje genérico; el botón se deshabilita mientras envía.

Archivos esperados (crear/tocar): `LoginScreen.tsx`, `screens/index.ts`, `routes/login.tsx` y el test.

## Fuera de alcance (no tocar)

- Recuperar contraseña (CU15).
- Recordar sesión con casilla "mantener sesión": la sesión ya persiste con el refresh token (`UT-CU01-FE-01`).
- El guard de rutas (`UT-CU01-FE-08`).

## Resultado esperado

Se puede iniciar sesión desde la web y llegar al módulo del rol.

## Validación

1. `pnpm test`, `pnpm lint` y `pnpm build` pasan.
2. Con el stack: iniciar sesión con una cuenta válida lleva a `/panel` y deja una fila `INICIO_SESION` en `bitacora`; con contraseña incorrecta muestra el mensaje genérico.
3. Una cuenta con `activo = false` muestra el mensaje de cuenta desactivada y no entra.
