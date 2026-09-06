# UT-HU01-BE-05 — Bloqueo por intentos fallidos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU01-BE-05` |
| **Nombre** | Bloqueo por intentos fallidos consecutivos (`features/auth/service/LoginAttemptService`) |
| **Historia** | HU01-BE |
| **Depende de** | — |

## Antes de implementar

Leer la regla de negocio "Bloqueo por intentos fallidos" en
[HU01 - Iniciar Sesión.md](../../../HistoriasDeUsuario/HU01%20-%20Iniciar%20Sesión.md).

## Solicitud de negocio

Evitar ataques de fuerza bruta contra el login: tras un número máximo de
intentos fallidos consecutivos, la cuenta se bloquea temporalmente aunque
las credenciales correctas se ingresen después, hasta que pase el tiempo
de bloqueo.

## Objetivo

Implementar `LoginAttemptService` en `features/auth/service/` (es lógica
de negocio propia del módulo `auth`, no infraestructura transversal — a
diferencia de `JwtService`/`RefreshTokenService`, que sí viven en
`security/` porque los necesitará cualquier mecanismo de autenticación
futuro, no solo el login).

## Alcance

### Configuración (`application.properties`)

Agregar la sección "8. AUTH — Bloqueo por intentos fallidos" documentada
en el [README](./README.md#configuración-nueva-applicationproperties).

### `LoginAttemptService` — `features/auth/service/LoginAttemptService.java`

El contador se lleva por **identificador de login** (`username` o
`correo`, el valor que el usuario haya escrito en el formulario — ver
[HU01 - Iniciar Sesión.md](../../../HistoriasDeUsuario/HU01%20-%20Iniciar%20Sesión.md)),
no por un campo fijo de `Usuario`:

```java
@Component
public class LoginAttemptService {

    private static final String INTENTOS_PREFIX = "auth:failed-attempts:";
    private static final String BLOQUEO_PREFIX = "auth:locked:";

    // Incrementa el contador de auth:failed-attempts:{identificador} (TTL =
    // ventana-intentos-minutos en el primer incremento). Si alcanza
    // app.auth.max-intentos-fallidos, setea auth:locked:{identificador}
    // con TTL = duracion-bloqueo-minutos.
    public void registrarFallo(String identificador) { ... }

    // Optional.empty() si no está bloqueado; si lo está, el tiempo restante
    // (leído del TTL de auth:locked:{identificador} en Redis).
    public Optional<Duration> tiempoRestanteBloqueo(String identificador) { ... }

    // Borra auth:failed-attempts:{identificador} (no borra
    // auth:locked:{identificador} -- si ya está bloqueado, un login
    // exitoso no debería ser posible porque AuthService verifica el
    // bloqueo antes de validar credenciales).
    public void reiniciar(String identificador) { ... }
}
```

- Usa `StringRedisTemplate`, igual que `RefreshTokenService`.
- El incremento (`registrarFallo`) usa `RedisTemplate.opsForValue().increment(key)`
  y, si el valor devuelto es `1` (primera vez), setea el TTL de la ventana
  con `expire(key, ...)` — evita pisar el TTL en cada intento fallido
  posterior dentro de la misma ventana.
- Si el usuario alterna entre escribir su `username` y su `correo` en
  intentos sucesivos, el contador **no** los une (son dos keys distintas)
  — se acepta esta limitación menor, no hay caso de uso real que la
  justifique resolver ahora (el atacante típico prueba un solo
  identificador repetidamente).

### Archivos esperados (crear/tocar)

- `features/auth/service/LoginAttemptService.java`
- `application.properties` (nueva sección de config)

## Fuera de alcance (no tocar)

- `AuthService`/`AuthController` (UT-HU01-BE-07 — ahí se **usa** este
  servicio: llama a `tiempoRestanteBloqueo` antes de autenticar, a
  `registrarFallo` en credenciales inválidas, y a `reiniciar` en éxito)
- `RateLimitFilter` (mecanismo distinto, por IP, ya implementado — ver
  [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#rate-limit);
  no se tocan ni se fusionan ambos mecanismos)

## Resultado esperado

Tras `app.auth.max-intentos-fallidos` llamadas a `registrarFallo(identificador)`,
`tiempoRestanteBloqueo(identificador)` devuelve un `Duration` positivo;
antes de llegar al máximo, devuelve `Optional.empty()`.

## Validación

1. Registrar 4 fallos (con `max-intentos-fallidos=5`) → `tiempoRestanteBloqueo`
   sigue vacío.
2. Registrar el 5º fallo → `tiempoRestanteBloqueo` devuelve un valor ≈
   `duracion-bloqueo-minutos`.
3. `reiniciar(identificador)` borra el contador de intentos; llamarlo no
   desbloquea una cuenta ya bloqueada (el bloqueo expira solo por TTL).
4. Dos identificadores distintos no interfieren entre sí (keys independientes).
