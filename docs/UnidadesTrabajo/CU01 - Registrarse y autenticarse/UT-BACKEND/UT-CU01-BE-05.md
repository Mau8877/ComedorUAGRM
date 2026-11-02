# UT-CU01-BE-05 — Verificación del ID token de Firebase (resource server)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-05` |
| **Nombre** | Verificación del ID token de Firebase (resource server) |
| **Caso de uso** | CU01-BE |
| **Depende de** | — |

## Antes de implementar

- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md)
- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Ninguna petición a la API debe aceptarse sin un token de Firebase válido: hay que reemplazar el usuario en memoria de prueba por la verificación real.

## Objetivo

El backend valida el ID token de Firebase (firma, emisor, audiencia y expiración) y rechaza con `401` todo lo demás.

## Alcance

- `pom.xml`: agregar `spring-boot-starter-oauth2-resource-server` (y `spring-security-test` en test si no está).
- `application.properties`: `app.firebase.project-id=${FIREBASE_PROJECT_ID:}`.
- `B/config/FirebaseJwtConfig.java`: bean `JwtDecoder` (`NimbusJwtDecoder` con las claves públicas de Firebase, `https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com`) con validadores de **emisor** (`https://securetoken.google.com/{projectId}`), **audiencia** (`projectId`) y expiración. Si `app.firebase.project-id` está vacío, el arranque falla con un mensaje claro.
- `B/config/SecurityConfig.java`: `oauth2ResourceServer(jwt)`, sesión `STATELESS`, el resto de las reglas actuales (infra pública, `anyRequest().authenticated()`) sin cambios. Comprobar que ya no se genera el usuario en memoria con contraseña aleatoria.
- `projects/backend/.env.example`, `docker-compose.yml` (variable del servicio `backend`) y `.github/workflows/ci.yml` si el test de contexto la necesita: agregar `FIREBASE_PROJECT_ID`.
- `SEGURIDAD_AUTH_BACKEND.md`: reemplazar "Qué librería lo hace… se decide en la primera UT" por la decisión tomada (resource server).
- Tests (`T/config/`): con un `JwtDecoder` simulado, una petición a `/api/v1/algo` sin token da `401`; con un JWT de `spring-security-test` válido pasa la autenticación (el `404` posterior es esperado); `/health` sigue público.

Archivos esperados (crear/tocar): `pom.xml`, `application.properties`, `FirebaseJwtConfig.java`, `SecurityConfig.java`, `.env.example`, `docker-compose.yml`, `SEGURIDAD_AUTH_BACKEND.md`, test(s) de config.

## Fuera de alcance (no tocar)

- El cuerpo del `401` con el sobre de la API (`UT-CU01-BE-06`): acá basta el código HTTP.
- Resolver el usuario de la base (`UT-CU01-BE-07`).
- Crear el proyecto de Firebase (manual, fuera del repo).
- Admin SDK de Firebase.

## Resultado esperado

Con un `projectId` configurado, solo pasan las peticiones con un ID token de Firebase vigente de ese proyecto.

## Validación

1. `./mvnw test` pasa, incluidos los tests de seguridad nuevos.
2. Con el stack levantado y `FIREBASE_PROJECT_ID` definido: `curl -i localhost:8080/api/v1/carreras` sin token devuelve `401` y `curl -i localhost:8080/health` devuelve `200`.
3. En el log de arranque ya no aparece "Using generated security password".
