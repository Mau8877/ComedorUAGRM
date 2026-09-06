# UT-HU01-BE-08 — Pruebas unitarias de `AuthService`/`AuthController`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU01-BE-08` |
| **Nombre** | Pruebas unitarias de login (`AuthServiceTest`, `AuthControllerTest`) |
| **Historia** | HU01-BE |
| **Depende de** | UT-HU01-BE-07 |

## Antes de implementar

Leer [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)
completo.

## Solicitud de negocio

Evitar regresiones en las reglas de negocio del login: credenciales,
cuenta inactiva, bloqueo por intentos fallidos, y el contrato HTTP exacto
de la respuesta.

## Objetivo

Implementar `AuthServiceTest` (JUnit 5 + Mockito, sin `@SpringBootTest`) y
`AuthControllerTest` (`@WebMvcTest` + `MockMvc`, con
`spring-boot-starter-webmvc-test` ya disponible).

## Alcance

### `AuthServiceTest` — `src/test/java/.../features/auth/service/AuthServiceTest.java`

- `@ExtendWith(MockitoExtension.class)`, mocks: `AuthenticationManager`,
  `UsuarioRepository`, `JwtService`, `RefreshTokenService`,
  `LoginAttemptService`. `@InjectMocks AuthService`.

| # | Caso | Assert |
|---|------|--------|
| 1 | Login exitoso, cuenta activa, un rol | `AuthResult` con `accessToken`/`refreshToken`/`usuario` correctos (`roles` con 1 elemento); `loginAttemptService.reiniciar` invocado |
| 2 | Login exitoso, usuario con dos roles activos | `usuario.roles()` con 2 elementos; `jwtService.generarAccessToken` invocado con una lista de 2 |
| 3 | Cuenta bloqueada (intentos fallidos) | `LockedException` con `errorCode = ERR_AUTH_03`; `authenticationManager.authenticate` **nunca** invocado |
| 4 | `username`/`correo` inexistente | `authenticate` lanza `BadCredentialsException` (mock) → `AuthService` lanza `UnauthorizedException` `ERR_AUTH_01`; `loginAttemptService.registrarFallo` invocado |
| 5 | Password incorrecta | mismo resultado que el caso 4 (mismo código/mensaje) |
| 6 | Cuenta con `estado=INACTIVO` | `authenticate` lanza `DisabledException` (mock) → `AuthService` lanza `ForbiddenException` `ERR_AUTH_02`; `registrarFallo` **no** invocado |
| 7 | Login exitoso resetea contador | verificar `loginAttemptService.reiniciar(identificador)` invocado exactamente una vez |
| 8 | Claims del JWT | verificar que `jwtService.generarAccessToken` se invoca con el `codigo` y los `roles` del usuario encontrado, no con datos del request |

Estilo AAA, sin base de datos real, sin Redis real (todo mockeado).

### `AuthControllerTest` — `src/test/java/.../features/auth/controller/AuthControllerTest.java`

- `@WebMvcTest(AuthController.class)`, `@MockBean AuthService`,
  `@MockBean RefreshTokenService` (para el `buildCookie` del controller).

| # | Caso | Assert |
|---|------|--------|
| 1 | Login exitoso | `200`; body `status:"success"`, `data.accessToken` presente, `data.usuario` sin `passwordHash`; header `Set-Cookie` con `refreshToken` |
| 2 | `AuthService` lanza `UnauthorizedException` | `401`; body `status:"failed"`, `error:"ERR_AUTH_01"` |
| 3 | `AuthService` lanza `ForbiddenException` | `403`; `error:"ERR_AUTH_02"` |
| 4 | `AuthService` lanza `LockedException` | `423`; `error:"ERR_AUTH_03"` |
| 5 | Body con `identificador` vacío | `400`; `error:"ERR_SYS_01"` (Bean Validation, `AuthService` ni se invoca) |
| 6 | Body con `password` vacío | `400`; `error:"ERR_SYS_01"` |

### Archivos esperados (crear)

- `src/test/java/com/comedoruagrm/backend/features/auth/service/AuthServiceTest.java`
- `src/test/java/com/comedoruagrm/backend/features/auth/controller/AuthControllerTest.java`

## Fuera de alcance (no tocar)

- Tests de integración con Testcontainers/BD real (no aplica — no hay
  queries complejas ni constraints que requieran Postgres real para este
  flujo)
- Tests de `JwtService`/`RefreshTokenService`/`LoginAttemptService` en
  aislamiento (opcionales, no obligatorios para esta UT — la cobertura de
  `AuthServiceTest` ya ejercita su contrato a través de mocks)
- Tests de frontend (ver [`../UT-FRONTEND/`](../UT-FRONTEND/))

## Resultado esperado

Suite verde, sin dependencia de orden de ejecución, sin Docker/Redis/
Postgres reales corriendo.

## Validación

1. `mvn test -Dtest=AuthServiceTest,AuthControllerTest` pasa.
2. Los 6 CA de error de HU01 (CA02, CA03, CA04, CA05, CA06, CA09) tienen
   al menos un test que los cubre explícitamente.
3. Ningún test verifica el valor exacto del JWT/refresh token generado
   (son mocks) — solo que se invocan con los argumentos correctos.
