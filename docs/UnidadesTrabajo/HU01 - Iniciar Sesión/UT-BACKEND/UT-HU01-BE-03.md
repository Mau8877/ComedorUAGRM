# UT-HU01-BE-03 — JWT: generación de access token

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU01-BE-03` |
| **Nombre** | JWT: generación del access token (`security/JwtService`) |
| **Historia** | HU01-BE |
| **Depende de** | — |

## Antes de implementar

Leer [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#flujo-jwt--refresh-token),
[CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md).

## Solicitud de negocio

El login debe emitir un access token JWT de corta duración con el
`codigo` del usuario y la lista de nombres de sus roles activos como
claims, sin datos sensibles.

## Objetivo

Agregar la librería JWT al proyecto (no existe ninguna todavía en
`pom.xml`) e implementar `JwtService` en `security/` para generar el
access token.

## Alcance

### Dependencia nueva (`pom.xml`)

No hay ninguna librería JWT en el proyecto — agregar **JJWT**
(`io.jsonwebtoken:jjwt-api` + `jjwt-impl` + `jjwt-jackson`, scope `impl`/
`jackson` en runtime). **Verificar la última versión estable contra Maven
Central antes de fijarla** (mismo criterio ya aplicado para `bucket4j` en
`SEGURIDAD_AUTH_BACKEND.md` — no asumir una versión de memoria):

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>{verificar}</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>{verificar}</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>{verificar}</version>
    <scope>runtime</scope>
</dependency>
```

### Configuración (`application.properties`)

Agregar la sección "6. JWT (Autenticación)" documentada en el
[README](./README.md#configuración-nueva-applicationproperties) — solo las
dos claves de esta UT: `app.jwt.secret`, `app.jwt.access-token-expiration-minutes`.

### `JwtService` — `security/JwtService.java`

```java
@Component
public class JwtService {

    public String generarAccessToken(UUID codigoUsuario, List<String> nombresRoles) { ... }
}
```

- Firma HMAC-SHA256 con la clave de `app.jwt.secret` (mínimo 32
  caracteres — documentar en el default de `application.properties` que
  hay que reemplazarlo en producción).
- Claims: `sub` = `codigoUsuario.toString()`, `roles` = `nombresRoles`
  (array de strings, ej. `["ADMIN"]`), `iat`/`exp` (expiración =
  `app.jwt.access-token-expiration-minutes`). **No** agregar `username`,
  `correo` ni ningún otro dato — solo lo mínimo necesario (ver
  [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#flujo-jwt--refresh-token)).
- **Solo generación, no validación/parseo.** Esta HU no necesita leer un
  access token de vuelta (el login únicamente lo emite) — un método
  `parsear(...)` no tiene ningún consumidor dentro de esta HU. Se
  construye recién en la HU/UT que efectivamente lo necesite (el filtro
  que valida tokens en requests protegidos, ver
  [UNIDADES_DE_TRABAJO.md](../../../../.claude/rules/UNIDADES_DE_TRABAJO.md#una-ut-nunca-construye-trabajo-de-otra-hu-aunque-sea-trivial-agregarlo-ahora) —
  no se anticipa acá aunque sea sencillo agregarlo ahora.

### Archivos esperados (crear/tocar)

- `security/JwtService.java`
- `pom.xml` (nueva dependencia)
- `application.properties` (nueva sección de config)

## Fuera de alcance (no tocar)

- Filtro `OncePerRequestFilter` de validación de tokens en requests
  (follow-up, no bloqueante para el login)
- `RefreshTokenService` (UT-HU01-BE-04)
- `AuthService`/`AuthController` (UT-HU01-BE-07 — ahí se **usa** este
  servicio, no se define)

## Resultado esperado

`JwtService.generarAccessToken(codigo, roles)` devuelve un JWT válido, con
claims mínimos y expiración configurable.

## Validación

1. Un token generado, decodificado manualmente (ej. en jwt.io), expone
   `sub` = el `codigo` correcto y `roles` = la lista de roles correcta.
2. Un usuario con dos roles activos genera un token con `roles` de
   longitud 2.
3. El `exp` del token generado corresponde a `app.jwt.access-token-expiration-minutes`
   minutos desde su emisión.
4. El JWT decodificado no contiene `username`, `correo` ni `passwordHash`.
