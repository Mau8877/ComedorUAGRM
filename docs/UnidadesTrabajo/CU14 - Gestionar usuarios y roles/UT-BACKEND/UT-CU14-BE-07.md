# UT-CU14-BE-07 — Listar las cuentas del personal

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-07` |
| **Nombre** | Listar las cuentas del personal |
| **Caso de uso** | CU14-BE |
| **Depende de** | `UT-CU14-BE-01`, `UT-CU14-BE-03`, `UT-CU14-BE-06` |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md)
- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (listados, `sort`)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#roles-y-permisos) (`@PreAuthorize` en el `service`)
- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

El Administrador necesita ver las cuentas del personal con su rol y estado, y poder filtrarlas y buscarlas, para decidir qué hacer con cada una (flujo A).

## Objetivo

`UsuarioPersonalService.listar(...)` devuelve una página de `UsuarioPersonalResponse` con su `meta`, solo para el Administrador DUBSS.

## Alcance

- `B/features/usuarios/service/UsuarioPersonalService.java` (clase nueva, `@Service` + `@RequiredArgsConstructor`): `listar(int page, int pageSize, String search, String rol, Boolean activo, String sort)`, con `@Transactional(readOnly = true)` y `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")`. Devuelve un `record PaginaUsuariosPersonal(List<UsuarioPersonalResponse> datos, PageMeta meta)`, igual que `CarreraService.PaginaCarreras`.
- Reglas: `page` y `pageSize` mayores a cero (si no, `ValidationException` `ERR_SYS_01`, igual que `CarreraService`); `rol`, si llega, tiene que ser uno de los tres del personal (si no, `ValidationException` `ERR_US_10`, incluido `POSTULANTE`); `sort` acepta `apellidos`, `ci`, `correo`, `rol` y `creadoEn`, con `-` para descendente, y por defecto `-creadoEn` (un campo no soportado → `ValidationException` `ERR_US_11`); el `Sort` se arma con referencias a propiedad tipadas, no con strings. Usa `UsuarioRepository.buscarPersonal`.
- Mapea cada `Usuario` a `UsuarioPersonalResponse` (nunca expone el hash ni el `id`).
- Las lecturas no se loguean ([`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md)).
- Tests con el repositorio mockeado: mapeo y `meta`; `ERR_US_10` con `POSTULANTE` y con un rol inexistente; `ERR_US_11` con un `sort` inválido; el orden por defecto; `page` y `pageSize` inválidos.

Archivos esperados (crear): `UsuarioPersonalService.java` y su test.

## Fuera de alcance (no tocar)

- El endpoint y el parseo de `filter[...]`: es de `UT-CU14-BE-12`.
- Crear, editar y cambiar de estado: las UT 08 a 11 agregan sus métodos a esta misma clase.
- Búsqueda por otros campos (correo, rol) y exportaciones.

## Resultado esperado

El controller puede pedir una página de cuentas del personal ya filtrada y ordenada.

## Validación

1. `./mvnw test` pasa.
2. Un test confirma que ningún `UsuarioPersonalResponse` lleva datos de contraseña ni de bloqueo.
3. `./mvnw checkstyle:check` sin errores.
