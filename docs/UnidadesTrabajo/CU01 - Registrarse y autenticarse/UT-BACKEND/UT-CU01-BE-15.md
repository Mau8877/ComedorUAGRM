# UT-CU01-BE-15 — Sesiones por dispositivo: refresh tokens

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-15` |
| **Nombre** | Sesiones por dispositivo: refresh tokens |
| **Caso de uso** | CU01-BE |
| **Depende de** | `UT-CU01-BE-01`, `UT-CU01-BE-03`, `UT-CU01-BE-05` |

## Antes de implementar

- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)
- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md)

## Solicitud de negocio

El usuario no debe escribir su contraseña cada 15 minutos, y cerrar sesión en un dispositivo no debe afectar a los demás (CA19).

## Objetivo

Existe la sesión por dispositivo: se abre, se renueva rotando su refresh token y se cierra, guardando solo el hash del token.

## Alcance

- `B/features/auth/model/Sesion.java`: `@Table(name = "sesion")`, `@AttributeOverride(... "codigo_sesion")`; campos `usuario` (`@ManyToOne` LAZY), `tokenHash`, `expiraEn`, `revocadaEn` (único con setter); `estaVigente(ahora)`.
- `B/features/auth/repository/SesionRepository.java`: `findByTokenHash(String)`.
- `B/features/auth/service/SesionService.java`: `abrir(Usuario)` (refresh token aleatorio de 256 bits en base64url; guarda su hash SHA-256 y devuelve el token en claro una sola vez junto al token de acceso de `TokenService`), `renovar(refreshToken)` (vigente o `401` `ERR_AUTH_06`; cuenta desactivada → `403` `ERR_AUTH_03`; revoca el usado y abre uno nuevo) y `cerrar(refreshToken)` (idempotente: un token desconocido, vencido o ya cerrado se ignora).
- La migración de la tabla `sesion` está en `UT-CU01-BE-01`.
- Ni el refresh token ni su hash se escriben en los logs.
- Tests de service con mocks: el hash guardado no es el token, dos sesiones tienen tokens distintos, renovar revoca el usado, vencido/revocado/desconocido → `ERR_AUTH_06`, cuenta desactivada → `ERR_AUTH_03`, cerrar es idempotente.

Archivos esperados (crear): `Sesion.java`, `SesionRepository.java`, `SesionService.java` y `SesionServiceTest.java`.

## Fuera de alcance (no tocar)

- Los endpoints HTTP (`UT-CU01-BE-13`).
- Limpieza periódica de sesiones vencidas o revocadas (follow-up).
- Detección de reutilización de un refresh token ya rotado (follow-up).

## Resultado esperado

Cada dispositivo tiene su propio refresh token, que se renueva rotándolo y se revoca al cerrar sesión sin tocar los de otros dispositivos.

## Validación

1. `./mvnw test` pasa.
2. En la base, `sesion.token_hash` tiene 64 caracteres hexadecimales y nunca coincide con el token devuelto al cliente.
3. `./mvnw checkstyle:check` sin errores.
