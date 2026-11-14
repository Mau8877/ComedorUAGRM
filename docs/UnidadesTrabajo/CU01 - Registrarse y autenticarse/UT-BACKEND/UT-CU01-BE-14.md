# UT-CU01-BE-14 — Pruebas de integración con base real del registro y la sesión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-14` |
| **Nombre** | Pruebas de integración con base real del registro y la sesión |
| **Caso de uso** | CU01-BE |
| **Depende de** | `UT-CU01-BE-09`, `UT-CU01-BE-12`, `UT-CU01-BE-13` |

## Antes de implementar

- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md)
- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)

## Solicitud de negocio

Hay que comprobar contra un Postgres real que la migración, los índices únicos y el flujo completo funcionan juntos, no solo con mocks.

## Objetivo

Existe una prueba de integración que levanta Postgres con Testcontainers y recorre registro, conflicto y sesión.

## Alcance

- `T/features/auth/AuthIntegrationTest.java`: patrón obligatorio de `TESTING_BACKEND.md` (`assumeTrue` sobre Docker en un `@BeforeAll`; el contenedor se maneja a mano), `postgres:18-alpine`, contexto de Spring completo con **JWT firmados de verdad** (nada se simula).
- Escenarios: (1) `POST /auth/registro` crea `usuario`, `postulante` y `sesion`, guarda solo el hash de la contraseña y responde `201`; (2) repetirlo responde `200` sin duplicar filas; (3) otro correo con el mismo CI / el mismo correo en otra capitalización / el mismo registro universitario responden `409` `ERR_US_01` / `ERR_US_02` / `ERR_US_03`; (4) `login` correcto devuelve un token que abre `usuario-actual` y anota `INICIO_SESION`; (5) contraseña incorrecta y correo inexistente dan `401` `ERR_AUTH_04` con el mismo mensaje; (6) tras 5 fallos la cuenta responde `403` `ERR_AUTH_05` aunque acierte, y un acierto antes del 5.º reinicia el contador; (7) con el usuario desactivado, `login`, `usuario-actual` (aun con un token ya emitido) y `refresh` responden `403` `ERR_AUTH_03`; (8) `refresh` rota el token (el usado da `401` `ERR_AUTH_06`); (9) `logout` revoca solo la sesión de ese dispositivo y es idempotente; (10) `sesion.token_hash` no es el token; (11) token ausente o inventado → `401` `ERR_AUTH_01`; (12) `GET /carreras` sin token responde `200`; (13) flujo web solo con cookies `HttpOnly`: login, `usuario-actual`, refresh rotado y logout sin header `Authorization`.

Archivos esperados (crear): `AuthIntegrationTest.java`.

## Fuera de alcance (no tocar)

- Probar desde un navegador o un celular real.
- Tests de rendimiento.

## Resultado esperado

El backend del CU01 queda verificado de punta a punta contra una base real, donde haya Docker.

## Validación

1. Con Docker disponible, `./mvnw test` ejecuta y pasa los escenarios.
2. Sin Docker, el test queda `SKIPPED` y el build no se rompe.
