---
globs: projects/backend/**/*
---

# Seguridad y Autenticación — Backend

## Estado actual (punto de partida)

`SecurityConfig.java` hoy usa el `InMemoryUserDetailsManager` autogenerado
por Spring Boot (usuario/password aleatorio impreso en el log en cada
arranque). Esto es **solo un placeholder de desarrollo** y debe
reemplazarse por la implementación real descrita abajo antes de considerar
la autenticación "hecha" — no es un requisito opcional.

## Flujo: JWT + refresh token

- **Access token**: JWT de corta duración (ej. 15 minutos). Va en el header
  `Authorization: Bearer <token>` en cada request. Contiene el `id` del
  usuario y su rol (ver [Roles](#roles-y-permisos)) como claims — nada
  sensible además de eso.
- **Refresh token**: de larga duración (ej. 7 días), opaco (no
  necesariamente JWT — puede ser un UUID aleatorio persistido). Se usa
  únicamente contra el endpoint de refresh, nunca contra endpoints de
  negocio.
- **Endpoints** (`/api/v1/auth/...`):
  - `POST /api/v1/auth/login` → valida credenciales, devuelve `{ accessToken, refreshToken }`.
  - `POST /api/v1/auth/refresh` → recibe el refresh token, valida que no esté
    invalidado/expirado, devuelve un access token (y opcionalmente rota el
    refresh token).
  - `POST /api/v1/auth/logout` → invalida el refresh token del usuario (ver
    abajo).
- **Invalidación en logout**: el refresh token se guarda del lado del
  servidor (tabla `refresh_tokens` o, mejor, en **Redis** con TTL igual a su
  vencimiento — Redis ya está en `docker-compose.yml`, pero **la dependencia
  `spring-boot-starter-data-redis` todavía no está en `pom.xml`**, hay que
  agregarla antes de implementar esto). En logout se borra/marca inválido esa
  entrada; en refresh se valida contra ese storage, no solo contra la firma
  del JWT.
- El access token **no** se invalida antes de tiempo (es stateless por
  diseño) — por eso su vida útil es corta. No se implementa una blacklist de
  access tokens salvo que aparezca un requisito concreto que lo justifique.

## Reemplazo del `InMemoryUserDetailsManager`

- Entidad `User` (tabla `users`) con al menos: `id`, `email` (único),
  `passwordHash` (BCrypt, nunca texto plano), `role`, `createdAt`. Todavía no
  existe — es un requisito de esta regla, no algo que ya esté implementado.
- `UserDetailsServiceImpl implements UserDetailsService` en `security/`, que
  busca el usuario por email vía el repositorio y arma el
  `UserDetails`/principal con su rol.
- `SecurityConfig` pasa a usar este `UserDetailsService` + un
  `PasswordEncoder` (`BCryptPasswordEncoder`) en vez del in-memory. El filtro
  de autenticación por JWT (`OncePerRequestFilter` custom) valida el token en
  cada request y setea el `Authentication` en el `SecurityContext`.

## Roles y permisos

Esquema simple de **roles fijos** (no permisos granulares) — apropiado para
el alcance de este proyecto salvo que el dominio real termine necesitando
algo más fino:

- `ADMIN`: acceso completo.
- `USUARIO`: acceso a sus propios recursos / operaciones estándar.

El rol vive como columna `role` en `User` (enum Java `Role { ADMIN, USUARIO }`,
persistido como `String` con `@Enumerated(EnumType.STRING)` — nunca
`ORDINAL`, para no depender del orden de declaración del enum). Se protege
por rol con `@PreAuthorize("hasRole('ADMIN')")` a nivel de método de
`service` (no en el controller — mantiene la regla de controllers delgados
de [ENDPOINTS_BACKEND.md](ENDPOINTS_BACKEND.md)).

Si en algún momento un módulo necesita permisos más finos que "admin vs
usuario", se define en su propia regla puntual — no se expande este esquema
global de forma especulativa.

## Obtener el usuario autenticado en los controllers

No se extrae el JWT manualmente en cada endpoint (`request.getHeader("Authorization")`,
parsear, etc.). Se usa una anotación custom que resuelve el dato directo
desde el `SecurityContext`:

```java
@GetMapping("/{id}/pedidos")
public ResponseEntity<ApiResponse<List<PedidoResponse>>> misPedidos(@CurrentUserId Long userId) {
    ...
}
```

La anotación `@CurrentUserId` (y su `HandlerMethodArgumentResolver`) se
define una sola vez en `security/` — ver
[CONVENCIONES_JAVA_BACKEND.md](CONVENCIONES_JAVA_BACKEND.md) para dónde vive
exactamente el archivo, para no terminar con dos definiciones de la misma
anotación en dos lugares distintos.

## Endpoints públicos

Solo quedan sin autenticación los ya definidos en `SecurityConfig`
(`/health`, `/health/**`, `/prometheus`, `/swagger-ui/**`, `/v3/api-docs/**`)
más los de `/api/v1/auth/login`, `/api/v1/auth/refresh` (obviamente, todavía
no hay token) y `/api/v1/auth/register` si el proyecto lo requiere.
Cualquier otro endpoint nuevo es privado por default — si necesita ser
público, es una decisión explícita, no un olvido de agregar la regla en
`SecurityConfig`.
