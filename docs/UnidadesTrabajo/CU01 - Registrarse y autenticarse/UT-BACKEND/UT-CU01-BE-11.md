# UT-CU01-BE-11 — Lógica de registro de un Postulante

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-11` |
| **Nombre** | Lógica de registro de un Postulante |
| **Caso de uso** | CU01-BE |
| **Depende de** | `UT-CU01-BE-03`, `UT-CU01-BE-04`, `UT-CU01-BE-07`, `UT-CU01-BE-10` |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md)
- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md)
- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md)
- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)

## Solicitud de negocio

Registrar a un Postulante debe crear su cuenta una sola vez, sin duplicar CI, correo ni registro universitario, y siempre con el rol Postulante.

## Objetivo

`AuthService.registrar(...)` crea `usuario` + `postulante` de forma idempotente y anota el registro en la bitácora.

## Alcance

- `B/features/usuarios/service/UsuarioService.java`: `registrarPostulante(String firebaseUid, String correo, RegistroRequest req)`: en una transacción valida y crea, en este orden: carrera existente (si no, `NotFoundException` `ERR_US_04`), CI libre (`ConflictException` `ERR_US_01`), correo libre (`ERR_US_02`), registro universitario libre (`ERR_US_03`); crea `Usuario` con el rol `POSTULANTE` (de `RolRepository`) y `activo = true`, y su `Postulante`. Si ya existe un `usuario` con ese `firebaseUid`, **devuelve el existente sin crear nada** (idempotencia, A7/CA12). Traduce una `DataIntegrityViolationException` por carrera de concurrencia al `ERR_US_xx` del campo que corresponda.
- `B/features/auth/service/AuthService.java`: `registrar(FirebaseSinRegistro token, RegistroRequest req, String ip)` → llama a `UsuarioService` y a `BitacoraService.registrar(..., REGISTRO_USUARIO, ...)` solo cuando **creó** la cuenta; devuelve `UsuarioResponse` y un indicador de si fue creada o ya existía.
- Logging INFO en el service, **sin** CI, contraseñas ni tokens (usar el `codigo` del usuario).
- Tests unitarios con mocks (`@Mock` de repositorios): registro correcto con rol Postulante; cada uno de los tres conflictos; carrera inexistente; repetido con el mismo `firebaseUid` no crea ni anota nada; el request no puede cambiar el rol.

Archivos esperados (crear): `UsuarioService.java`, `AuthService.java` y sus tests.

## Fuera de alcance (no tocar)

- El controller y los códigos HTTP (`UT-CU01-BE-12`).
- Crear la cuenta en Firebase (la crea el cliente).
- Verificar el correo ni reglas de contraseña.

## Resultado esperado

La lógica de registro cumple CA01, CA02, CA03, CA06, CA12 y CA21 a nivel de service.

## Validación

1. `./mvnw test` pasa con los seis escenarios.
2. `./mvnw checkstyle:check` sin errores.
3. Revisar que ningún log del service contiene CI, correo ni tokens.
