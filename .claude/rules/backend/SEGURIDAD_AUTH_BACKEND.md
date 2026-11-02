# Seguridad y Autenticación — Backend

## Modelo: Firebase Auth + backend como servidor de recursos

La autenticación **no la implementa este backend**: la hace **Firebase
Auth** (stack definido en la documentación de grado: *Spring Boot (REST,
JWT, HTTPS)* + *Firebase: Auth, FCM, Storage*). Firebase se encarga de las
credenciales, el registro de la cuenta, la verificación de correo, la
recuperación de contraseña, el bloqueo por intentos fallidos y la emisión y
renovación de los tokens. El backend solo **verifica** el token que llega y
**autoriza** según los datos del dominio.

Por eso este backend **no** tiene: endpoint de login propio, emisión de
JWT propios, refresh tokens, cookies de sesión, `BCryptPasswordEncoder` ni
contraseñas en la base de datos (`usuario` no tiene `password_hash`; solo
`firebase_uid`, ver [PERSISTENCIA_BD_BACKEND.md](PERSISTENCIA_BD_BACKEND.md)).

> **Estado actual.** `SecurityConfig.java` ya verifica el ID token de
> Firebase (resource server, ver el paso 3 de abajo) y la sesión es
> `STATELESS`: no existe el usuario en memoria con contraseña aleatoria que
> traía Spring Boot por defecto. CSRF sigue deshabilitado a propósito: no
> hay cookies de sesión, la API es stateless y el token viaja en el header
> `Authorization`. El `projectId` se configura con `FIREBASE_PROJECT_ID`
> (`app.firebase.project-id`); sin él el backend no arranca.

## Flujo de una request autenticada

1. El cliente (web React o app Flutter) inicia sesión con el **SDK de
   Firebase** (correo + contraseña) y obtiene un **ID token** (JWT firmado
   por Google, de vida corta). El SDK lo renueva solo; el cliente nunca
   maneja el refresh token a mano ni lo copia a otro almacenamiento.
2. En cada request a la API: `Authorization: Bearer <ID token>`.
3. El backend valida el token en un filtro de seguridad: **firma, `iss`
   (`https://securetoken.google.com/{projectId}`), `aud` (el projectId) y
   expiración**. Lo hace el **resource server de Spring Security**
   (`spring-boot-starter-security-oauth2-resource-server`) contra las
   claves públicas de Firebase (`FirebaseJwtConfig`), y no el Admin SDK de
   Firebase, porque para verificar no hace falta una credencial de
   servicio. El Admin SDK se agregará cuando haya que crear cuentas en
   Firebase desde el backend (CU14).
4. Con el `uid` del token (`sub`) busca el `usuario` por `firebase_uid`,
   comprueba que esté **activo y no eliminado** (`usuario.activo`,
   `eliminado_en IS NULL`) y arma el `Authentication` con las `authorities`
   `ROLE_{rol.nombre}`.
5. Token válido pero **sin `usuario`** con ese `firebase_uid` →
   `UnauthorizedException` (`401`). Usuario existente pero **inactivo** →
   `ForbiddenException` (`403`). Token ausente, mal firmado o vencido →
   `401`.

- El `activo` se consulta en cada request (no solo al iniciar sesión): así
  desactivar a un usuario (CU-14) surte efecto de inmediato aunque su ID
  token siga vigente unos minutos. Si el costo se vuelve relevante, se
  cachea en Redis con TTL corto.
- **El rol sale de la base de datos**, no del token: el ID token de Firebase
  no lleva el rol y no se usan *custom claims* salvo que aparezca un
  requisito concreto que la consulta a la base no cubra.
- **Cerrar sesión** es del cliente (`signOut` del SDK); el backend es
  stateless y no guarda sesiones. No hay lista de tokens revocados.
- El `firebase_uid` **siempre** sale del token verificado, nunca de un
  campo del body o de la URL.

### Registro (CU-01)

Firebase crea la cuenta; luego el cliente llama al backend con el ID token
recién obtenido para crear el `usuario` del dominio (y su `postulante`):

- El endpoint de registro exige un token de Firebase **válido**, pero no
  que ya exista un `usuario` (es el único que lo permite). El rol inicial es
  `POSTULANTE`; el cliente no elige su rol.
- `ci` y `correo` únicos → `ConflictException` (`409`) con su código de
  error catalogado (ver [EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md)).
- Si falla la creación en la base después de crear la cuenta en Firebase,
  la cuenta de Firebase queda huérfana: el registro debe poder reintentarse
  con el mismo token sin duplicar el `usuario` (idempotente por
  `firebase_uid`).
- Recuperar contraseña y verificar correo (CU-15 / CU-01) los resuelve
  Firebase; en la base solo se reflejan datos de contacto (`telefono`,
  `correo`). El `ci` no es modificable (RF-10).

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

- Los nombres de rol se usan como **constantes de texto** (`RolNombres` o
  similar, en el módulo dueño del dato de `usuario`), no como un enum Java
  cerrado: son filas de la tabla `rol`.
- Las `authorities` son `ROLE_{nombre}`. Se protege por rol con
  `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")` (o `hasAnyRole(...)`) a
  nivel de método de `service` (no en el controller — mantiene la regla de
  controllers delgados de [ENDPOINTS_BACKEND.md](ENDPOINTS_BACKEND.md)).
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

`UUID`, no `Long` ni el `uid` de Firebase — es el `codigo_usuario` público
del usuario (resuelto a partir del `firebase_uid` del token), nunca el `id`
interno de la tabla `usuario`.

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
`/v3/api-docs/**`) y **una única excepción de negocio: `GET /api/v1/carreras`**.
El formulario de registro necesita mostrar la lista de carreras **antes** de
que la persona tenga cuenta ni token; es solo lectura de un catálogo, sin
datos personales. **Ningún otro endpoint de negocio es público**: incluso el
registro exige un token de Firebase válido (ver arriba), porque el login
y la creación de la cuenta ocurren en Firebase, no acá. Cualquier otro
endpoint nuevo es privado por default — si necesita ser público, es una
decisión explícita (como la de las carreras: documentada acá con su motivo),
no un olvido de agregar la regla en `SecurityConfig`.
