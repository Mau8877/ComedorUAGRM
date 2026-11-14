# UT-CU15-BE-09 — Solicitar el enlace de restablecimiento

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-09` |
| **Nombre** | Solicitar el enlace de restablecimiento |
| **Caso de uso** | CU15-BE |
| **Depende de** | `UT-CU15-BE-02`, `UT-CU15-BE-04`, `UT-CU15-BE-05`, `UT-CU15-BE-08` |

## Antes de implementar

- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md)
- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Una persona que olvidó su contraseña pide un enlace con su correo; el sistema nunca revela si el correo existe.

## Objetivo

`RestablecimientoContrasenaService.solicitar(correo)` genera y envía el enlace cuando corresponde y no hace nada visible cuando no.

## Alcance

- `B/features/auth/service/RestablecimientoContrasenaService.java` (`@Service`, `@RequiredArgsConstructor`) con `@Transactional void solicitar(String correo)`:
  1. Normaliza el correo (minúsculas, sin espacios) y busca la cuenta (`UsuarioRepository.findByCorreo`).
  2. Si no existe o **no está activa**: no hace nada (A1). WARN/INFO sin el correo.
  3. Si está activa: genera el token (`TokenEnlace`), llama a `usuario.iniciarRestablecimiento(hash, ahora + app.auth.reset-ttl-minutes)` (sobrescribe el enlace anterior, así solo el último sirve, A11) y llama a `CorreoRestablecimientoService.enviarEnlace(...)`.
  - **No** lleva `@Auditar` ni escribe en la bitácora (CU15, regla de negocio). Nunca loguea el correo, el token ni el enlace; sí `INFO` con `userId` cuando se envió.
- **El correo se envía después de confirmar la transacción** (`TransactionSynchronization.afterCommit` o un evento `@TransactionalEventListener(AFTER_COMMIT)`), para que el enlace nunca llegue al correo si la base lo deshizo.
- El método no devuelve nada que distinga los casos (ni excepciones distintas).
- Tests con repositorios y correo mockeados: cuenta activa → la cuenta queda con el hash (no el token) y su vencimiento, y se envía el token; correo desconocido y cuenta desactivada → no se guarda ni se envía nada, y el comportamiento observable es idéntico; dos solicitudes seguidas → la cuenta queda solo con el hash del segundo token; un fallo del envío no hace fallar `solicitar`; el correo mayúsculas/espacios encuentra la cuenta.

Archivos esperados (crear): `RestablecimientoContrasenaService.java` y su test.

## Fuera de alcance (no tocar)

- Restablecer la contraseña (UT-10).
- El endpoint y la ruta pública (UT-13).
- Límite de solicitudes por cuenta (follow-up).

## Resultado esperado

Pedir un enlace con un correo de cuenta activa deja un enlace vigente y un correo en camino; con cualquier otro correo no pasa nada visible.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
