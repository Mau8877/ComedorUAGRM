# Seguridad y Autenticación — Backend

## Modelo: JWT propio (access token + refresh token)

La autenticación **la implementa este backend**: guarda las contraseñas
(solo como hash BCrypt), verifica las credenciales, emite los tokens y los
verifica en cada request. **No se usa Firebase Auth** (Firebase queda
únicamente para notificaciones push desde mobile, ver
[ARQUITECTURA_BACKEND.md](../ARQUITECTURA_BACKEND.md#módulos-del-dominio)).

| Pieza | Qué es | Dónde vive |
| --- | --- | --- |
| **Token de acceso** | JWT firmado con HS256 (`JWT_SECRET`), vida corta (15 min por default). Lleva el `codigo` público del usuario como `sub`, y nada más (ni rol ni correo). Viaja en `Authorization: Bearer`. | `security/TokenService`, `config/AutenticacionConfig` |
| **Refresh token** | Cadena aleatoria opaca de 256 bits (no es un JWT), vida larga (7 días por default). Sirve para renovar el token de acceso. Uno por dispositivo/sesión. | tabla `sesion` (solo su hash SHA-256), `features/auth/service/SesionService` |
| **Contraseña** | Hash BCrypt en `usuario.password_hash`. Nunca en texto plano, nunca en un DTO de salida, nunca en un log. | `features/usuarios/` |

El backend usa el **resource server de Spring Security**
(`spring-boot-starter-security-oauth2-resource-server`) para verificar la
firma, el emisor (`JWT_ISSUER`) y la expiración del token de acceso.

- `JWT_SECRET` es **obligatorio y de al menos 32 caracteres**: si falta o es
  corto, el backend no arranca (`AutenticacionConfig`). Nunca se versiona
  (solo vive en el `.env`); cambiarlo invalida todos los tokens de acceso
  emitidos.
- Tiempos configurables: `JWT_ACCESS_TTL_MINUTES` (15) y
  `JWT_REFRESH_TTL_DAYS` (7).

## Cómo viajan los tokens: cookies en la web, Bearer en mobile

| Cliente | Token de acceso | Refresh token |
| --- | --- | --- |
| **Web** | Cookie `access_token` (`HttpOnly`, `SameSite=Strict`, `Path=/api/v1`) | Cookie `refresh_token` (`HttpOnly`, `SameSite=Strict`, `Path=/api/v1/auth`) |
| **Mobile** | Header `Authorization: Bearer` (en memoria) | Cuerpo de `/refresh` y `/logout` (guardado en `flutter_secure_storage`) |

- `registro`, `login` y `refresh` **siempre** escriben las cookies (`AuthCookies`) **y** devuelven los tokens en el cuerpo (`SesionResponse`); la web ignora el cuerpo y mobile ignora las cookies. `logout` vence las cookies.
- El token se lee del header `Authorization` y, si no viene, de la cookie `access_token`; en `/refresh` y `/logout` gana el cuerpo sobre la cookie `refresh_token`. `/refresh` sin ninguno de los dos → `401` `ERR_AUTH_06`; `/logout` sin ninguno igual responde `200`.
- **Por qué cookies HttpOnly:** el JavaScript de la página no puede leerlas, así un XSS no roba la sesión (con `localStorage` sí). Por eso la web **no guarda ningún token** en `localStorage`/`sessionStorage`/memoria.
- **CSRF:** el backend tiene CSRF deshabilitado porque la API es stateless; con cookies la defensa es `SameSite=Strict` (el navegador no las manda desde otro sitio) más el CORS restringido a los orígenes de `app.cors.allowed-origins` (`allowCredentials=true`, nunca `*`). Web y API deben ser el mismo *site* (hoy `localhost`, y en producción el mismo dominio detrás del proxy). Si algún día el frontend vive en otro dominio, hay que replantear esto (token CSRF).
- `COOKIE_SECURE=true` es **obligatorio en producción** (cookie solo por HTTPS); en desarrollo local por http queda en `false`.

## Endpoints de autenticación

Todos bajo `/api/v1/auth`. Los cuatro primeros son **públicos** (no exigen
token de acceso; ver [Endpoints públicos](#endpoints-públicos)).

| Método y ruta | Qué hace | Respuesta |
| --- | --- | --- |
| `POST /registro` | Crea la cuenta del Postulante y abre su sesión (CU01, flujo A) | `201` (`200` si la misma petición ya se había procesado) con la sesión |
| `POST /login` | Verifica correo y contraseña y abre la sesión (flujo B) | `200` con la sesión |
| `POST /refresh` | Canjea un refresh token vigente por un par nuevo; el usado queda revocado (rotación) | `200` con la sesión |
| `POST /logout` | Revoca el refresh token recibido: cierra **solo** la sesión de ese dispositivo (flujo C) | `200`, `data: null`; idempotente (un token desconocido o ya cerrado no es error) |
| `GET /usuario-actual` | El usuario del token de acceso, para recuperar la sesión al recargar | `200` con el usuario |
| `POST /recuperar-contrasena` | Pide el enlace para restablecer la contraseña (CU15). Público. Responde siempre `200` con el mismo mensaje exista o no la cuenta | `200`, `data: null` |
| `POST /restablecer-contrasena` | Define la contraseña nueva con el token del enlace del correo (CU15). Público | `200`, `data: null`; `400` `ERR_AUTH_08` si el enlace no sirve |
| `POST /cambiar-contrasena` | Cambia la contraseña desde el perfil con la actual (CU15). Exige sesión; cierra las demás sesiones y conserva la actual | `200`, `data: null`; `400` `ERR_AUTH_09` si la actual es incorrecta |

La "sesión" que devuelven `registro`, `login` y `refresh` es:

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "9d3f...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "usuario": { "codigo": "...", "nombres": "...", "apellidos": "...", "correo": "...", "rol": "POSTULANTE" }
}
```

## Flujo de una request autenticada

1. El cliente inicia sesión (o se registra) y recibe `accessToken` y
   `refreshToken`.
2. En cada request: la cookie `access_token` (web, la manda el navegador) o `Authorization: Bearer <accessToken>` (mobile).
3. El resource server valida firma, emisor y expiración. Token ausente,
   mal firmado o vencido → `401` `ERR_AUTH_01`.
4. `UsuarioActivoFilter` busca el `usuario` por el `sub` (su `codigo`),
   comprueba que esté **activo y no eliminado** y arma el `Authentication`
   con las `authorities` `ROLE_{rol.nombre}`. Usuario inexistente → `401`
   `ERR_AUTH_02`; desactivado → `403` `ERR_AUTH_03`.
5. Con el token de acceso vencido, el cliente llama a `POST /refresh` con su
   refresh token y reintenta. Si el refresh token también venció o fue
   revocado → `401` `ERR_AUTH_06` y el cliente lleva al usuario al login.

- El `activo` se consulta en cada request (no solo al iniciar sesión): así
  desactivar a un usuario (CU-14) surte efecto de inmediato aunque su token
  de acceso siga vigente unos minutos, y `refresh` también lo niega. Si el
  costo se vuelve relevante, se cachea en Redis con TTL corto.
- **El rol sale de la base de datos**, no del token: el token no lleva
  rol, así un cambio de rol también surte efecto de inmediato.
- Una **ruta pública ignora el header `Authorization`** (`RutasPublicas` +
  el `BearerTokenResolver` de `SecurityConfig`): un token vencido que el
  cliente sigue mandando no debe hacer fallar el login con `401`.
- El `codigo` del usuario **siempre** sale del token verificado, nunca de un
  campo del body o de la URL.
- **Rotación del refresh token:** cada `refresh` revoca el token usado y
  emite uno nuevo. El refresh token se guarda en la base solo como hash
  SHA-256 (una filtración de la tabla no permite abrir sesiones).
- Las sesiones vencidas o revocadas quedan en la tabla `sesion`; su limpieza
  periódica no está implementada todavía.

### Registro (CU01, flujo A)

- Público: no exige token. El rol inicial es siempre `POSTULANTE`; el
  cliente no elige su rol (si lo manda, se ignora).
- La contraseña tiene 6 a 72 caracteres (72 es el máximo que procesa BCrypt)
  y debe coincidir con su confirmación; el correo se normaliza a minúsculas
  y sin espacios antes de guardarlo y de compararlo.
- `ci`, `correo` y `registro_universitario` únicos → `ConflictException`
  (`409`) con su código de error catalogado (ver
  [EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md)).
- **Idempotente (RNF-03):** repetir la misma petición (mismo correo, CI y
  contraseña) devuelve la cuenta existente con `200` y abre una sesión
  nueva, sin duplicar el `usuario`.

### Inicio de sesión y bloqueo por intentos fallidos (CU01, flujo B)

Orden de las verificaciones en `UsuarioService.validarCredenciales`:

1. **Cuenta bloqueada** → `403` `ERR_AUTH_05`, aunque la contraseña sea
   correcta (el mensaje dice cuántos minutos faltan).
2. **Contraseña incorrecta o correo inexistente** → `401` `ERR_AUTH_04` con
   **el mismo mensaje en ambos casos** (no se revela cuál falló). Cuando el
   correo no existe se compara igual contra un hash de relleno para que la
   respuesta tarde lo mismo.
3. **Cuenta desactivada** → `403` `ERR_AUTH_03`.

El bloqueo lo implementa este backend (columnas `intentos_fallidos`,
`bloqueos_consecutivos`, `bloqueado_hasta` de `usuario`):

- A los **5 intentos fallidos consecutivos** la cuenta se bloquea; el
  contador de intentos vuelve a 0.
- La duración crece con cada bloqueo seguido: **5 min, 15 min, 1 h, 24 h**
  (la última se repite). ⚠️ La escala exacta está **por definir con la
  DUBSS**; hoy es una constante en `UsuarioService.DURACIONES_BLOQUEO`.
- Un inicio de sesión correcto reinicia los intentos y la escalada.
- El conteo debe guardarse aunque el login termine en error: por eso
  `AuthService.iniciarSesion` y `UsuarioService.validarCredenciales` usan
  `@Transactional(noRollbackFor = BusinessException.class)`.
- Todo inicio de sesión exitoso (incluido el que sigue a un registro) queda
  anotado en la bitácora (`INICIO_SESION`).

- Verificar el correo no está implementado todavía; cuando se haga, lo define
  su propio caso de uso. El `ci` no es modificable (RF-10).

### Recuperar y cambiar la contraseña (CU15)

- **Enlace por correo.** `recuperar-contrasena` genera un token aleatorio de 256
  bits (`TokenEnlace`), guarda **solo su hash SHA-256** y su vencimiento en las
  columnas `restablecimiento_token_hash` y `restablecimiento_expira_en` de
  `usuario` (60 minutos, `app.auth.reset-ttl-minutes`) y lo envía por correo
  (`CorreoRestablecimientoService`, asíncrono) a `{app.web.base-url}/restablecer-contrasena?token=…`.
  Solo hay un enlace vigente por cuenta: pedir otro lo sobrescribe, y usarlo lo borra.
- **Mensaje neutro.** La solicitud responde siempre igual, exista o no el correo y
  esté o no activa la cuenta; no se anota en la bitácora. Un fallo del SMTP se
  registra (sin correo ni token) y no llega al cliente.
- **Restablecer** guarda el hash BCrypt nuevo, reinicia el bloqueo por intentos
  fallidos, borra el enlace y **cierra todas las sesiones** de la cuenta. Un enlace
  desconocido, vencido, usado, reemplazado o de una cuenta desactivada da el mismo
  `400` `ERR_AUTH_08`.
- **Cambiar la contraseña desde el perfil** exige la actual (`400` `ERR_AUTH_09` si
  es incorrecta: no es `401`, para que el cliente no intente renovar la sesión) y
  cierra las sesiones de los **demás** dispositivos conservando la actual
  (`SesionService.revocarOtras`, que identifica la sesión por el refresh token del
  cuerpo —mobile— o de la cookie —web—). Por eso vive en `/api/v1/auth`, donde viaja
  la cookie `refresh_token`.
- **Correo.** `spring-boot-starter-mail`; en desarrollo los correos caen en Mailpit
  (`http://localhost:8025`), y pasar a un proveedor real es cambiar las variables
  `MAIL_*` y `FRONTEND_URL`, sin tocar código.

## Roles y permisos

**Un usuario tiene un único rol**: `usuario.rol_id` → `rol` (diagrama de
base de datos). Catálogo actual (valores de `rol.nombre`):

| Rol | Quién |
| --- | --- |
| `POSTULANTE` | Estudiante UAGRM; postula y, si resulta becado, consulta su beca (el «Becado» **no** es un rol: es un postulante con `beca` vigente). |
| `ADMINISTRADOR_DUBSS` | Personal de la DUBSS: audita, adjudica, gestiona convocatorias, usuarios, padrón, bitácora y parámetros del modelo. Si se divide en subroles (Trabajo Social / Encargada de Sistemas), **por definir con la DUBSS**. |
| `DIRECTIVO` | Consume el dashboard de indicadores BI. |
| `PERSONAL_COMEDOR` | Registra la asistencia digital. |

«Usuario del Sistema» (el actor abstracto del diagrama de casos de uso) no
se almacena como rol.

- Los nombres de rol se usan como **constantes de texto** (`RolNombres`, en
  `features/usuarios/constant/`, el módulo dueño del dato de `usuario`), no
  como un enum Java cerrado: son filas de la tabla `rol`.
- Las `authorities` son `ROLE_{nombre}`. Se protege por rol con
  `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")` (o `hasAnyRole(...)`) a
  nivel de método de `service` (no en el controller — mantiene la regla de
  controllers delgados de [ENDPOINTS_BACKEND.md](ENDPOINTS_BACKEND.md)).
- Un usuario autenticado que llama a algo que exige un rol que no tiene recibe
  `403` con `ERR_AUTH_07` en el sobre estándar (lo escribe `ApiAccessDeniedHandler`).
- El nombre exacto de cada rol y a qué endpoints accede lo fija el caso de
  uso que lo necesita.

## Obtener el usuario autenticado en los controllers

No se extrae el token manualmente en cada endpoint
(`request.getHeader("Authorization")`, parsear, etc.). Se usa una
anotación custom que resuelve el dato directo desde el `SecurityContext`:

```java
@GetMapping("/mis-postulaciones")
public ResponseEntity<ApiResponse<List<PostulacionResponse>>> misPostulaciones(@CurrentUserId UUID userId) {
    ...
}
```

`UUID`, no `Long` — es el `codigo_usuario` público del usuario (el `sub`
del token de acceso), nunca el `id` interno de la tabla `usuario`.

La anotación `@CurrentUserId` (y su `HandlerMethodArgumentResolver`) se
define una sola vez en `security/` — ver
[CONVENCIONES_JAVA_BACKEND.md](CONVENCIONES_JAVA_BACKEND.md) para dónde vive
exactamente el archivo, para no terminar con dos definiciones de la misma
anotación en dos lugares distintos.

## Rate limit

**Ya implementado** — `RateLimitFilter` (`security/RateLimitFilter.java`),
registrado en `SecurityConfig` con `addFilterBefore(rateLimitFilter,
UsernamePasswordAuthenticationFilter.class)` (corre antes que la
autenticación: un cliente sin token igual consume su cupo, no es gratis
intentar de más).

- **Alcance**: solo `/api/v1/**` (`shouldNotFilter` excluye todo lo demás).
  Explícitamente **no** aplica a `/health`, `/prometheus` ni
  `/swagger-ui/**`/`/v3/api-docs/**` — `/prometheus` en particular recibe
  scrape de Prometheus cada 5s (ver `infrastructure/prometheus/prometheus.yml`)
  y lo bloquearía por error si estuviera dentro del alcance.
- **Corre por prefijo de URL, no por endpoint real.** El filtro se ejecuta
  a nivel servlet, **antes** de que Spring resuelva si existe un
  `@RestController` que atienda esa ruta. Esto significa que una IP
  consume cupo igual pegándole a un endpoint real que a una URL bajo
  `/api/v1/**` que ni siquiera existe todavía (un 404) — no hace falta que
  el feature esté implementado para que el rate limit ya esté activo sobre
  su futura ruta.
- **Algoritmo**: token bucket vía **Bucket4j** (`com.bucket4j:bucket4j_jdk11-core`,
  no `bucket4j-core` — esa coordinada vieja ya no es la que resuelve Maven
  Central; se verificó contra el índice de Maven Central antes de fijar la
  versión). Un bucket por IP (`ConcurrentHashMap<String, Bucket>`), refill
  "greedy" (los tokens se regeneran continuo, no en ráfagas cada N segundos).
- **Config** (`application.properties`, sobreescribible por env var):
  `app.ratelimit.capacity` (default `100`) y
  `app.ratelimit.refill-per-minute` (default `100`) → `RATE_LIMIT_CAPACITY` /
  `RATE_LIMIT_REFILL_PER_MINUTE`.
- **Respuesta al superar el límite**: `429`, con el mismo sobre de
  [RESPONSES_BACKEND.md](RESPONSES_BACKEND.md), código `ERR_SYS_02` (ver
  [EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md)). Se escribe **a mano**
  (`response.getWriter().write(...)`), no vía `ApiResponse.error(...)` ni
  `GlobalExceptionHandler` — el filtro corre a nivel servlet, antes de que
  exista un `DispatcherServlet`/`@RestControllerAdvice` que pueda intervenir.
  Igual respeta el contrato exacto del sobre para que el cliente (frontend/
  mobile) lo trate igual que cualquier otro error de la API.
- **Identificación del cliente**: `request.getRemoteAddr()`. Alcanza hoy
  porque nginx es el único proxy delante del backend dentro de la red de
  Docker. Si en el futuro hay más de un proxy en la cadena, hay que leer
  `X-Forwarded-For` con cuidado (el primer valor, no el último — un cliente
  puede falsificar ese header).

> **Limitación conocida, aceptada a propósito:** el bucket vive en memoria
> del proceso (`ConcurrentHashMap`), no es distribuido. Con una sola
> instancia del backend (el caso de este proyecto) funciona perfecto. Si en
> algún momento se corre más de una réplica, cada una tendría su propio
> límite independiente (un atacante repartiendo requests entre instancias
> efectivamente multiplica su cupo). Para eso existe
> `bucket4j_jdk11-redis` (Redis ya está en el stack y
> `spring-boot-starter-data-redis` ya está en el `pom.xml`) — no se
> implementa hasta que haya más de una instancia real corriendo.

> **Esto no reemplaza protección a nivel de infraestructura.** Un rate
> limit en la app (como este) es la última línea de defensa, no la única —
> nginx (que ya está en `docker-compose.yml` como proxy) también podría
> tener su propio `limit_req_zone`/`limit_req`, más barato en recursos
> porque corta el tráfico antes de que llegue a la JVM. No está configurado
> todavía; si se agrega, va en una regla de infraestructura aparte, no acá.

## Endpoints públicos

Quedan sin autenticación los de infraestructura ya definidos en
`SecurityConfig` (`/health`, `/health/**`, `/prometheus`, `/swagger-ui/**`,
`/v3/api-docs/**`) y estas excepciones de negocio, todas listadas en
`security/RutasPublicas`:

| Endpoint | Motivo |
| --- | --- |
| `POST /api/v1/auth/registro` | La persona todavía no tiene cuenta |
| `POST /api/v1/auth/login` | La persona todavía no tiene sesión |
| `POST /api/v1/auth/refresh` | Se llama justamente cuando el token de acceso ya venció; se autentica con el refresh token del body |
| `POST /api/v1/auth/logout` | Se autentica con el refresh token del body; cerrar una sesión ya vencida no debe dar error (CU01, A10) |
| `POST /api/v1/auth/recuperar-contrasena` | La persona olvidó la contraseña y no tiene sesión; la respuesta es siempre neutra (CU15) |
| `POST /api/v1/auth/restablecer-contrasena` | Se autentica con el token de un solo uso del enlace del correo (CU15) |
| `GET /api/v1/carreras` | El formulario de registro necesita la lista **antes** de que exista cuenta; es solo lectura de un catálogo, sin datos personales |

**Ningún otro endpoint de negocio es público.** Cualquier endpoint nuevo es
privado por default — si necesita ser público, es una decisión explícita
(documentada acá con su motivo y agregada a `RutasPublicas`), no un olvido
de agregar la regla en `SecurityConfig`.
