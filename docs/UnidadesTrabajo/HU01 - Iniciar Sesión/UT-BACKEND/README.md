# Unidades de trabajo — HU01-BE

**Historia:** Login del sistema — validar credenciales, emitir access token (JWT) + refresh token (cookie `HttpOnly`), bloquear la cuenta tras intentos fallidos consecutivos.
**Fuente HU:** [`../../HistoriasDeUsuario/HU01 - Iniciar Sesión.md`](../../HistoriasDeUsuario/HU01%20-%20Iniciar%20Sesión.md)
**Recurso HTTP:** `POST /api/v1/auth/login`
**Docs de errores:** `docs/errors/auth/ERRORES_AUTH.md` (se crea en UT-HU01-BE-02)

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — backend en particular. Prioridad
según la unidad: [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md),
[CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md),
[SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md),
[EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md),
[GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md),
[RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md),
[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md),
[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md),
[LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md),
[TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).
Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules.

## Decisión de diseño: transporte del refresh token

Confirmado con la persona a cargo del proyecto (no estaba resuelto en la
HU original): el refresh token viaja como **cookie `HttpOnly`** seteada
por el backend, nunca en el body de la respuesta ni manejado por
JavaScript en el cliente. El detalle completo de atributos de la cookie
(`SameSite`, `Secure`, `Path`, `Max-Age`) está en
[SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#flujo-jwt--refresh-token)
— estas UT no repiten esos valores, lo citan.

## Decisión de diseño: `Usuario` vive en `features/usuarios/`, no en `features/auth/`

`auth` es un módulo de **acciones** (login, y a futuro refresh/logout — ver
[HU-02](../../../HistoriasDeUsuario/HU02%20-%20Renovar%20Sesión.md) y
[HU-03](../../../HistoriasDeUsuario/HU03%20-%20Cerrar%20Sesión.md)), no el
dueño del recurso `Usuario`. El Model, la migración y el repositorio de
`Usuario` se crean en `features/usuarios/` (dueño del dato); `auth`
depende de `UsuarioRepository`/`UserDetailsServiceImpl`, nunca al revés —
mismo criterio de la [regla de dependencia](../../../../.claude/rules/ARQUITECTURA_BACKEND.md#regla-de-dependencia).
El CRUD administrativo completo de usuarios (listar, crear, editar) **no**
es parte de esta HU — es una HU aparte a futuro; acá solo se crea lo
mínimo que el login necesita.

## Contrato de datos (mínimo para esta HU — fuente: `docs/DiagramaConceptual.mermaid`)

**`Usuario`** (tabla `users`, extiende `BaseModel` — `id` bigint interno +
`codigo` público en columna `codigo_usuario`, ver
[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#identificador-de-fila-id-interno--codigo-público-patrón-basemodel)):

| Campo | Tipo | Notas |
|-------|------|-------|
| `codigo` | UUID | Heredado de `BaseModel` — identificador público, **nunca** el `id` interno |
| `username` | String | Único; identificador de login (junto con `correo`) |
| `correo` | String | Único; identificador de login (junto con `username`) |
| `passwordHash` | String | BCrypt, nunca texto plano, nunca se serializa a un DTO de respuesta |
| `estado` | `EstadoUsuario` (enum `ACTIVO`/`INACTIVO`) | `@Enumerated(EnumType.STRING)`; controla CA05 |
| `correoVerificado` | boolean | Existe en el diagrama; **no** bloquea el login en esta HU (fuera de alcance, ver follow-ups) |
| `createdAt`/`updatedAt`/`deletedAt` | — | Heredados de `BaseModel`, no se tocan acá |

**`Rol`** (tabla `roles`) y **`UsuarioRol`** (tabla `user_roles`, entidad
de asociación con su propio `codigo`/`isActive`/timestamps — no un
`@ManyToMany` simple): ver
[SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#roles-y-permisos)
para el porqué de este modelo (un usuario puede tener varios roles
activos a la vez, no una columna enum fija).

**No** hay campo `nombre` en `Usuario` — el nombre para mostrar vive en la
entidad `Perfil` del diagrama conceptual, fuera de alcance de esta HU.

## Recurso HTTP de esta HU

| Método | Path | HTTP éxito | `data` | Efecto adicional |
|--------|------|------------|--------|-------------------|
| `POST` | `/api/v1/auth/login` | 200 | `{ accessToken, usuario: { codigo, username, correo, roles } }` | Setea cookie `refreshToken` (`HttpOnly`) |

`roles` es un array de nombres de rol (`string[]`, ej. `["ADMIN"]` o
`["ADMIN", "USUARIO"]`) — un usuario puede tener más de uno.

`/api/v1/auth/refresh` y `/api/v1/auth/logout` **no** se implementan en
estas UT — son [HU-02](../../../HistoriasDeUsuario/HU02%20-%20Renovar%20Sesión.md)
y [HU-03](../../../HistoriasDeUsuario/HU03%20-%20Cerrar%20Sesión.md).

## Códigos de error nuevos (módulo `AUTH`)

| Código | HTTP | Excepción | Cuándo |
|--------|------|-----------|--------|
| `ERR_AUTH_01` | 401 | `UnauthorizedException` | `username`/`correo` inexistente o contraseña incorrecta (mismo mensaje genérico para ambos) |
| `ERR_AUTH_02` | 403 | `ForbiddenException` | Cuenta con `estado != ACTIVO` |
| `ERR_AUTH_03` | 423 | `LockedException` | Cuenta bloqueada por superar el máximo de intentos fallidos consecutivos |

`ERR_SYS_00`/`ERR_SYS_01` (ya existentes) cubren el 500 genérico y la
validación de `@Valid` sobre `LoginRequest` — no se catalogan de nuevo acá.

## Configuración nueva (`application.properties`)

Seguir el mismo patrón ya usado en el archivo (comentario de sección
numerada + `${VAR:default}`):

```properties
# ==========================================
# 6. JWT (Autenticación)
# ==========================================
app.jwt.secret=${JWT_SECRET:cambiar-este-secreto-en-produccion-min-32-caracteres}
app.jwt.access-token-expiration-minutes=${JWT_ACCESS_EXPIRATION_MINUTES:15}

# ==========================================
# 7. REFRESH TOKEN (cookie HttpOnly)
# ==========================================
app.jwt.refresh-token-expiration-days=${JWT_REFRESH_EXPIRATION_DAYS:7}
# false en dev (stack local sobre HTTP); Docker/prod la pisa a true por env var.
app.cookies.secure=${COOKIES_SECURE:false}

# ==========================================
# 8. AUTH — Bloqueo por intentos fallidos
# ==========================================
app.auth.max-intentos-fallidos=${AUTH_MAX_INTENTOS_FALLIDOS:5}
app.auth.ventana-intentos-minutos=${AUTH_VENTANA_INTENTOS_MINUTOS:15}
app.auth.duracion-bloqueo-minutos=${AUTH_DURACION_BLOQUEO_MINUTOS:15}
```

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HU01-BE-01](./UT-HU01-BE-01.md) | Persistencia: `Usuario`, `Rol`, `UsuarioRol`, migraciones, repositorios | — |
| [UT-HU01-BE-02](./UT-HU01-BE-02.md) | Excepciones y catálogo de errores del módulo `AUTH` | — |
| [UT-HU01-BE-03](./UT-HU01-BE-03.md) | JWT: generación de access token (`security/JwtService`) | — |
| [UT-HU01-BE-04](./UT-HU01-BE-04.md) | Refresh token: emisión, storage en Redis y cookie (`security/RefreshTokenService`) | — |
| [UT-HU01-BE-05](./UT-HU01-BE-05.md) | Bloqueo por intentos fallidos (`features/auth/service/LoginAttemptService`) | — |
| [UT-HU01-BE-06](./UT-HU01-BE-06.md) | Autenticación real: `UserDetailsServiceImpl` + `PasswordEncoder` + `SecurityConfig` | UT-HU01-BE-01 |
| [UT-HU01-BE-07](./UT-HU01-BE-07.md) | DTOs + `AuthService.login()` + `AuthController` | UT-HU01-BE-01 … UT-HU01-BE-06 |
| [UT-HU01-BE-08](./UT-HU01-BE-08.md) | Pruebas unitarias de `AuthService`/`AuthController` | UT-HU01-BE-07 |

**Cadena:** `01, 02, 03, 04, 05 → 06 → 07 → 08` (01-05 son independientes entre sí y pueden implementarse en paralelo).

## Criterios de aceptación cubiertos

| CA (HU01) | Descripción | UTs |
|-----------|-------------|-----|
| CA01 | Login exitoso → 200 + accessToken + usuario (con roles) + cookie | 01, 03, 04, 06, 07 |
| CA02/CA03 | `username`/`correo` inexistente / password incorrecta → 401 genérico | 02, 06, 07 |
| CA04 | Campos vacíos/inválidos → 400 `ERR_SYS_01` (Bean Validation, ya existente) | 07 |
| CA05 | Cuenta con `estado != ACTIVO` → 403 | 01, 02, 06, 07 |
| CA06 | Máximo de intentos fallidos → 423 | 02, 05, 07 |
| CA07 | Intento fallido no bloqueante → mismo 401, contador incrementado | 05, 07 |
| CA08 | Login exitoso resetea el contador de intentos | 05, 07 |
| CA09 | Error inesperado → 500 `ERR_SYS_00` (ya existente, sin cambios) | — |
| CA10 | Sobre de respuesta estándar en todas las respuestas | 07 |

## Follow-ups (no se implementan en estas UT)

- Filtro `OncePerRequestFilter` que valida el access token en cada request y setea el `SecurityContext` — hoy el login emite el token, pero ningún endpoint de negocio lo verifica todavía (no hay otros endpoints privados reales que probarlo).
- `@PreAuthorize("hasRole(...)")` en services de otros módulos.
- `@CurrentUserId` (ver [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md)) — no hace falta hasta que exista un endpoint que lo consuma.
- CRUD completo de `usuarios`/`roles` (listar/crear/editar/desactivar) — HU aparte.
- Rotación del refresh token en cada renovación — decisión de [HU-02](../../../HistoriasDeUsuario/HU02%20-%20Renovar%20Sesión.md), no bloqueante acá.
- Unicidad de `username`/`correo` con partial index (excluyendo soft-deleted) — no aplica todavía porque `usuarios` no tiene flujo de borrado implementado; se revisa si/cuando exista.
- Entidad `Perfil` (nombre para mostrar) — fuera del diagrama consultado para esta HU.
- Gate por `correoVerificado` en el login — el campo existe en el Model pero no se usa para bloquear todavía; sin CA que lo exija.

## Qué NO incluyen estas UT

- Los endpoints `/api/v1/auth/refresh` y `/api/v1/auth/logout` ([HU-02](../../../HistoriasDeUsuario/HU02%20-%20Renovar%20Sesión.md)/[HU-03](../../../HistoriasDeUsuario/HU03%20-%20Cerrar%20Sesión.md))
- CRUD de usuarios/roles (alta/edición/listado administrativo)
- Entidad `Perfil`
- Filtro JWT de validación en requests subsecuentes
- Código frontend (ver [`../UT-FRONTEND/README.md`](../UT-FRONTEND/README.md))
