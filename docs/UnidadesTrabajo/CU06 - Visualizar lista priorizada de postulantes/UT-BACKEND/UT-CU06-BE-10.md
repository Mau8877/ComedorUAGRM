# UT-CU06-BE-10 — Cuenta inicial de Administrador DUBSS para desarrollo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-10` |
| **Nombre** | Cuenta inicial de Administrador DUBSS para desarrollo |
| **Caso de uso** | CU06-BE |
| **Depende de** | — |

## Antes de implementar

- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (contraseñas solo como hash BCrypt, roles) y [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md).
- Como referencia: `UsuarioPersonalService.crear` (cómo se arma una cuenta del personal) y `UT-CU06-BE-03` (mismo mecanismo de cargador de desarrollo).

## Solicitud de negocio

Nadie puede entrar al panel sin un Administrador DUBSS: el rol no se elige al registrarse y las cuentas del personal las crea un administrador. Hace falta una primera cuenta para desarrollar y probar.

## Objetivo

Al arrancar en desarrollo existe, una sola vez, la cuenta `admin@dubss.com` con rol Administrador DUBSS.

## Alcance

- `B/config/DatosInicialesAdmin.java` (`@Component`, `CommandLineRunner`, `@Order(1)`) con `@Profile("dev")` y `@ConditionalOnProperty("app.seed.admin.enabled", matchIfMissing = true)`: **solo con el perfil `dev`**, por defecto activo, apagable con `SEED_ADMIN_ENABLED=false`.
- Crea la cuenta si no existe otra con ese correo: correo `admin@dubss.com`, contraseña `123123123` (guardada solo como hash BCrypt), nombres «Administrador DUBSS», CI `0000001`, rol `ADMINISTRADOR_DUBSS`, activa. Valores configurables en `application.properties` (`app.seed.admin.correo`, `.contrasena`, `.ci`, con variables `SEED_ADMIN_*`).
- Idempotente: si la cuenta ya existe no hace nada (no pisa una contraseña cambiada); si el CI ya es de otra cuenta, avisa con un WARN y no crea nada.
- **La contraseña es de prueba y solo vive en el entorno de desarrollo**; nunca se loguea. Documentado en `docs/README.md`.
- Tests unitarios (`DatosInicialesAdminTest`): crea la cuenta con el correo normalizado y la contraseña hasheada; no hace nada si ya existe, si el CI es ajeno o si falta el rol.

Archivos esperados (crear): `DatosInicialesAdmin.java` y su test. (Tocar): `application.properties`, `docs/README.md`.

## Fuera de alcance (no tocar)

- Cuentas de otros roles.
- El alta del personal (CU-14) y el cambio de contraseña (CU-15).
- Correr en producción o con otro perfil.

## Resultado esperado

Con el perfil `dev`, después de arrancar se puede iniciar sesión con `admin@dubss.com` / `123123123` y llamar a la lista priorizada.

## Validación

1. `./mvnw test` pasa.
2. Arrancar con el perfil `dev`: el log dice «Administrador DUBSS inicial creado» y `POST /api/v1/auth/login` con esas credenciales responde `200`; un segundo arranque no vuelve a crearla.
3. Sin el perfil `dev` el bean no existe.
