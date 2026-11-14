# UT-CU15-BE-13 — Endpoints, rutas públicas y reglas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-13` |
| **Nombre** | Endpoints, rutas públicas y reglas |
| **Caso de uso** | CU15-BE |
| **Depende de** | `UT-CU15-BE-09`, `UT-CU15-BE-10`, `UT-CU15-BE-11`, `UT-CU15-BE-12` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (controllers delgados)
- [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#endpoints-públicos)

## Solicitud de negocio

Web y móvil necesitan endpoints para recuperar la contraseña y gestionar el perfil, con las dos primeras operaciones accesibles sin sesión.

## Objetivo

Los cinco endpoints del CU responden con el sobre estándar, con las rutas públicas declaradas y las reglas documentadas.

## Alcance

- `B/security/RutasPublicas.java`: constantes `RECUPERAR_CONTRASENA` (`/api/v1/auth/recuperar-contrasena`) y `RESTABLECER_CONTRASENA` (`/api/v1/auth/restablecer-contrasena`) añadidas a `POST_PUBLICOS`. `cambiar-contrasena` **no** es pública.
- `B/features/auth/controller/AuthController.java`: tres métodos que solo validan y delegan:
  - `POST /recuperar-contrasena` → `RestablecimientoContrasenaService.solicitar`, siempre `200` con el mensaje neutro «Si la cuenta existe, enviamos un enlace al correo indicado. Vence en 60 minutos.» y `data: null`.
  - `POST /restablecer-contrasena` → `restablecer`, `200` «Contraseña restablecida correctamente».
  - `POST /cambiar-contrasena` (`@CurrentUserId UUID`) → `PerfilService.cambiarContrasena`, con el refresh token elegido (cuerpo sobre cookie `refresh_token`, el mismo método `elegirRefreshToken`), `200` «Contraseña actualizada correctamente».
- `B/features/usuarios/controller/PerfilController.java` (`/api/v1/perfil`): `GET` y `PATCH`, con `@CurrentUserId UUID`, `200` con `PerfilResponse`.
- Si hay una ruta nueva pública, se documenta: `SEGURIDAD_AUTH_BACKEND.md` (tabla de endpoints de autenticación y de endpoints públicos, con su motivo) y la sección de cookies si hace falta; `ENDPOINTS_BACKEND.md` no cambia.
- Tests (`@WebMvcTest`, verificando también la forma del sobre): recuperar con correo válido y con correo mal formado (`400` `ERR_SYS_01`); restablecer correcto, con contraseña corta, sin coincidir y con enlace inválido (`ERR_AUTH_08`); sin sesión, `cambiar-contrasena` y `/perfil` dan `401` `ERR_AUTH_01`; los dos públicos funcionan sin token e ignoran un `Authorization` vencido; el refresh token del cuerpo gana sobre la cookie; el CI en el body del `PATCH` no se refleja en la respuesta.

Archivos esperados (crear): `PerfilController.java` y su test. (Tocar): `AuthController.java`, `RutasPublicas.java`, `SEGURIDAD_AUTH_BACKEND.md` y los tests de ambos.

## Fuera de alcance (no tocar)

- Lógica de negocio en los controllers.
- Cambiar el `RateLimitFilter` (limita ya por IP todo `/api/v1/**`).
- Endpoints de la bitácora o de notificaciones.

## Resultado esperado

Web y móvil pueden recorrer los flujos A a E contra la API.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Con el stack levantado, `POST /api/v1/auth/recuperar-contrasena` con un correo real deja el correo en `http://localhost:8025`.
