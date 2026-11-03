# UT-CU01-BE-05 — JWT propio: firma, verificación y hash de contraseñas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-05` |
| **Nombre** | JWT propio: firma, verificación y hash de contraseñas |
| **Caso de uso** | CU01-BE |
| **Depende de** | — |

## Antes de implementar

- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md)
- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Ninguna petición a la API debe aceptarse sin un token de acceso válido emitido por el propio backend, y las contraseñas nunca pueden guardarse en texto plano.

## Objetivo

El backend firma sus tokens de acceso (JWT HS256), valida firma, emisor y expiración de los que recibe y rechaza con `401` todo lo demás; y dispone de un `PasswordEncoder` BCrypt.

## Alcance

- `pom.xml`: `spring-boot-starter-security-oauth2-resource-server` (trae Nimbus para firmar y verificar) y `spring-security-test` en test.
- `application.properties`: `app.jwt.secret=${JWT_SECRET:}`, `app.jwt.issuer=${JWT_ISSUER:comedor-uagrm}`, `app.jwt.access-ttl-minutes=${JWT_ACCESS_TTL_MINUTES:15}` y `app.jwt.refresh-ttl-days=${JWT_REFRESH_TTL_DAYS:7}`.
- `B/config/AutenticacionConfig.java`: beans `SecretKey` (falla el arranque con un mensaje que nombra `JWT_SECRET` si falta o tiene menos de 32 bytes), `JwtEncoder` y `JwtDecoder` (HS256, con validadores de **emisor** y expiración) y `PasswordEncoder` (`BCryptPasswordEncoder`).
- `B/security/TokenService.java`: `emitirAcceso(UUID codigoUsuario)` → JWT con `iss`, `sub` = código del usuario, `iat` y `exp`; devuelve el token y su vida en segundos. El token **no** lleva rol ni correo.
- `B/config/SecurityConfig.java`: `oauth2ResourceServer(jwt)`, sesión `STATELESS`, el resto de las reglas actuales (infra pública, `anyRequest().authenticated()`).
- `projects/backend/.env.example`, `.env.example` de la raíz y `docker-compose.yml` (servicio `backend`): variable `JWT_SECRET`. `src/test/resources/config/application.properties`: secreto de pruebas.
- `SEGURIDAD_AUTH_BACKEND.md`: documentar el modelo.
- Tests (`T/config/AutenticacionConfigTest`): un token emitido se decodifica con el mismo secreto y lleva el `sub`; uno firmado con otro secreto, de otro emisor o vencido es rechazado; secreto ausente o corto falla con un mensaje que nombra `JWT_SECRET`; el hash BCrypt no es la contraseña y se verifica.

Archivos esperados (crear/tocar): `pom.xml`, `application.properties`, `AutenticacionConfig.java`, `TokenService.java`, `SecurityConfig.java`, `.env.example` (ambos), `docker-compose.yml`, `SEGURIDAD_AUTH_BACKEND.md`, tests de config.

## Fuera de alcance (no tocar)

- El cuerpo del `401` con el sobre de la API (`UT-CU01-BE-06`): acá basta el código HTTP.
- Resolver el usuario de la base (`UT-CU01-BE-07`).
- Refresh tokens y sesiones (`UT-CU01-BE-15`).
- Cualquier uso de Firebase para autenticar (Firebase queda solo para notificaciones push).

## Resultado esperado

Con `JWT_SECRET` configurado, solo pasan las peticiones con un token de acceso vigente emitido por este backend.

## Validación

1. `./mvnw test` pasa, incluidos los tests de seguridad nuevos.
2. Con el stack levantado y `JWT_SECRET` definido: `curl -i localhost:8080/api/v1/auth/usuario-actual` sin token devuelve `401` y `curl -i localhost:8080/health` devuelve `200`.
3. Sin `JWT_SECRET`, el backend no arranca y el mensaje nombra la variable.
4. En el log de arranque ya no aparece "Using generated security password".
