# UT-HU01-BE-06 — Autenticación real: `UserDetailsServiceImpl` + `PasswordEncoder` + `SecurityConfig`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU01-BE-06` |
| **Nombre** | Reemplazo del `InMemoryUserDetailsManager`: `UserDetailsServiceImpl`, `PasswordEncoder`, `AuthenticationManager`, whitelist de `/auth/login` |
| **Historia** | HU01-BE |
| **Depende de** | UT-HU01-BE-01 (`UsuarioRepository`, `UsuarioRolRepository`) |

## Antes de implementar

Leer [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#reemplazo-del-inmemoryuserdetailsmanager)
completo.

## Solicitud de negocio

Hoy `SecurityConfig` usa el `InMemoryUserDetailsManager` autogenerado de
Spring Boot (placeholder de desarrollo, usuario/password aleatorio en el
log) — el login tiene que validar contra usuarios reales de la tabla
`users`.

## Objetivo

Reemplazar el mecanismo in-memory por un `UserDetailsService` real
respaldado por `UsuarioRepository`, con `BCryptPasswordEncoder`, y exponer
un `AuthenticationManager` que `AuthService` (UT-HU01-BE-07) pueda usar
para validar credenciales.

## Alcance

### `UserDetailsServiceImpl` — `security/UserDetailsServiceImpl.java`

```java
@Service
@RequiredArgsConstructor // Lombok -- ver CONVENCIONES_JAVA_BACKEND.md#lombok
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;

    @Override
    public UserDetails loadUserByUsername(String identificador) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsernameOrCorreo(identificador)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        List<String> authorities = usuarioRolRepository.findRolesActivosDeUsuario(usuario.getId()).stream()
                .map(ur -> "ROLE_" + ur.getRol().getNombre())
                .toList();

        return org.springframework.security.core.userdetails.User.builder()
                .username(identificador)
                .password(usuario.getPasswordHash())
                .authorities(authorities.toArray(new String[0]))
                .disabled(usuario.getEstado() != EstadoUsuario.ACTIVO)
                .build();
    }
}
```

- `.username(identificador)` — el valor tal como lo escribió el usuario
  (`username` o `correo`), no un campo fijo de la entidad; Spring Security
  solo lo usa como clave interna del `UserDetails`, no se persiste nada
  con ese valor.
- Si `authorities` queda vacío (usuario sin ningún rol activo), el login
  igual sucede a nivel de Spring Security, pero un `@PreAuthorize` en
  cualquier operación real lo bloqueará — no hace falta un chequeo
  adicional acá para esta HU (no hay ningún endpoint protegido por rol
  todavía).

> **Nota de comportamiento de Spring Security (no es un bug a corregir):**
> `AbstractUserDetailsAuthenticationProvider` valida `enabled` (nuestro
> `estado == ACTIVO`) **antes** de comparar la contraseña. Esto significa
> que una cuenta con `estado != ACTIVO` responde `403` (`DisabledException`
> → mapeado por `AuthService`) aunque la contraseña ingresada sea
> incorrecta, sin pasar primero por el `401` genérico. La HU no exige
> ocultar la existencia de una cuenta inactiva ante una contraseña
> incorrecta, así que se acepta este orden — es el comportamiento estándar
> de Spring Security, no algo que se está construyendo a mano.

### `SecurityConfig` — cambios sobre el archivo existente

**Archivo ya existente y compartido — modificar con cuidado, no
reescribir entero.**

- Agregar bean `PasswordEncoder`:
  ```java
  @Bean
  public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
  }
  ```
- Agregar bean `AuthenticationManager` (para que `AuthService` lo inyecte):
  ```java
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
      return config.getAuthenticationManager();
  }
  ```
- En `authorizeHttpRequests`, agregar **antes** de `.anyRequest().authenticated()`:
  ```java
  .requestMatchers("/api/v1/auth/login").permitAll()
  ```
- **No** hace falta declarar `UserDetailsServiceImpl` como bean manual — al
  estar anotado `@Service`, Spring Boot lo detecta y **deja de
  autoconfigurar** el `InMemoryUserDetailsManager` automáticamente.

### Archivos esperados (crear/tocar)

- `security/UserDetailsServiceImpl.java` (nuevo)
- `config/SecurityConfig.java` (**editar**, no reescribir)

## Fuera de alcance (no tocar)

- Whitelist de `/api/v1/auth/refresh` y `/api/v1/auth/logout` — se agregan
  en [HU-02](../../../HistoriasDeUsuario/HU02%20-%20Renovar%20Sesión.md)/[HU-03](../../../HistoriasDeUsuario/HU03%20-%20Cerrar%20Sesión.md)
  cuando esos endpoints existan (agregar la ruta antes de que el
  controller exista no rompe nada, pero no es responsabilidad de esta UT)
- Filtro JWT de validación de tokens en requests (follow-up)
- `RateLimitFilter` (ya implementado, no se toca)
- `AuthService`/`AuthController` (UT-HU01-BE-07)

## Resultado esperado

Al arrancar la app, ya **no** aparece el usuario/password aleatorio de
`InMemoryUserDetailsManager` en el log. `POST /api/v1/auth/login` es
públicamente accesible (sin token) — el resto de rutas sigue exigiendo
autenticación.

## Validación

1. Log de arranque sin el mensaje `"Using generated security password"`.
2. Un `AuthenticationManager.authenticate(...)` con credenciales válidas
   (usando `username`) contra un `Usuario` de prueba (`estado=ACTIVO`)
   autentica correctamente. Repetir usando `correo` en vez de `username`
   — debe autenticar igual.
3. Con `estado=INACTIVO`, `authenticate(...)` lanza `DisabledException`.
4. Con contraseña incorrecta, `authenticate(...)` lanza `BadCredentialsException`.
5. Un usuario con dos roles activos autentica con `authorities` de tamaño
   2 (`ROLE_{nombre1}`, `ROLE_{nombre2}`).
5. `POST /api/v1/auth/login` sin header `Authorization` no responde `401`
   por falta de autenticación (la ruta está en la whitelist) — cualquier
   error que devuelva en este punto viene de lógica de negocio, no de
   Spring Security bloqueando el acceso.
