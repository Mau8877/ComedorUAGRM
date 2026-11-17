# UT-CU13-BE-06 — Gestiones académicas: listar, crear y editar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU13-BE-06` |
| **Nombre** | Gestiones académicas: listar, crear y editar |
| **Caso de uso** | CU13-BE |
| **Depende de** | `UT-CU13-BE-01`, `UT-CU13-BE-02`, `UT-CU13-BE-04`, `UT-CU13-BE-05` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md)
- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#roles-y-permisos) (`@PreAuthorize` en el `service`)
- `B/features/usuarios/service/UsuarioPersonalService.java` (patrón de `listar`, `crear` y `editar` con `@Auditar`)

## Solicitud de negocio

Sin una gestión académica («2026-II») no se puede crear ninguna convocatoria, y hoy ninguna pantalla ni endpoint las crea. El Administrador tiene que poder mantenerlas.

## Objetivo

`GestionAcademicaService` lista, crea y edita gestiones académicas y anota en la bitácora cada alta y cada edición.

## Alcance

- `B/features/bitacora/constant/BitacoraAcciones.java`: agrega `CREACION_GESTION` y `EDICION_GESTION`.
- `B/features/convocatorias/service/GestionAcademicaService.java` (`@Service`, `@RequiredArgsConstructor`, `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")` en cada método público):
  - `listar(int page, int pageSize, String search, String sort)`: devuelve `PaginaGestiones(datos, meta)`. `sort` admite `etiqueta`, `fechaInicio`, `fechaFin` (por defecto `-fechaInicio`); otro valor es `ValidationException` `ERR_CV_11`.
  - `crear(UUID actorCodigo, GestionAcademicaRequest)`: con `@Transactional` y `@Auditar(accion = CREACION_GESTION, entidad = "gestion_academica")`. Normaliza la etiqueta (`trim`); si `fechaFin` no es posterior a `fechaInicio`, `ValidationException` `ERR_CV_13`; si la etiqueta ya existe, `ConflictException` `ERR_CV_12` (y se traduce también la violación del índice único si dos altas llegan juntas). Devuelve `ResultadoAuditado<GestionAcademicaResponse>` con `detalle = {etiqueta}`.
  - `editar(UUID actorCodigo, UUID codigo, GestionAcademicaRequest)`: `@Auditar(accion = EDICION_GESTION, ...)`. Si no existe, `NotFoundException` `ERR_CV_02`; mismas validaciones (la etiqueta propia no es conflicto); aplica los tres campos. `detalle = {camposModificados: [...]}` (solo nombres). Si nada cambia, `auditar = false`.
  - El actor se resuelve a su `Usuario` por el `codigo` del token (como `UsuarioPersonalService.actor(...)`).
- `INFO` en cada alta y edición (`module=convocatorias`, `gestionId` = código). Las lecturas no se loguean.
- Tests unitarios (repositorios simulados): crear OK; etiqueta repetida → `ERR_CV_12`; fin igual o anterior al inicio → `ERR_CV_13`; editar inexistente → `ERR_CV_02`; editar sin cambios → `auditar = false`; `sort` inválido → `ERR_CV_11`; el `detalle` no lleva fechas ni texto libre más allá de la etiqueta del alta.

Archivos esperados (crear/tocar): `GestionAcademicaService.java`, `BitacoraAcciones.java` y su test.

## Fuera de alcance (no tocar)

- El controller (`UT-CU13-BE-13`).
- Eliminar gestiones académicas: este CU no lo pide.
- Impedir que una gestión cambie de fechas si ya tiene convocatorias: el CU no lo define.
- Las convocatorias (UT 07 a 12).

## Resultado esperado

El Administrador puede dar de alta y editar gestiones, con validación y bitácora.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
