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

- `T/features/auth/AuthIntegrationTest.java`: patrón obligatorio de `TESTING_BACKEND.md` (`assumeTrue` sobre Docker en un `@BeforeAll`; el contenedor se maneja a mano), `postgres:18-alpine`, contexto de Spring completo con un `JwtDecoder` simulado que acepta tokens de prueba con `sub` y `email`.
- Escenarios: (1) `POST /auth/registro` crea `usuario` y `postulante` y responde `201`; (2) repetirlo responde `200` sin duplicar filas; (3) otro token con el mismo CI responde `409` `ERR_US_01`; (4) `POST /auth/sesion` devuelve el usuario y deja una fila `INICIO_SESION`; (5) `usuario-actual` no agrega filas; (6) con el usuario desactivado, `sesion` responde `403` `ERR_AUTH_03`; (7) `GET /carreras` sin token responde `200`.

Archivos esperados (crear): `AuthIntegrationTest.java`.

## Fuera de alcance (no tocar)

- Probar contra Firebase real (los tokens son de prueba).
- Tests de rendimiento.

## Resultado esperado

El backend del CU01 queda verificado de punta a punta contra una base real, donde haya Docker.

## Validación

1. Con Docker disponible, `./mvnw test` ejecuta y pasa los siete escenarios.
2. Sin Docker, el test queda `SKIPPED` y el build no se rompe.
