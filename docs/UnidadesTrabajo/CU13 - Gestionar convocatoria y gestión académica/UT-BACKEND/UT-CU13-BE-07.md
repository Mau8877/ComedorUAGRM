# UT-CU13-BE-07 — Listar y consultar convocatorias; listar tipos de documento

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU13-BE-07` |
| **Nombre** | Listar y consultar convocatorias; listar tipos de documento |
| **Caso de uso** | CU13-BE |
| **Depende de** | `UT-CU13-BE-01`, `UT-CU13-BE-03`, `UT-CU13-BE-04`, `UT-CU13-BE-05` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (`page`, `pageSize`, `search`, `filter[...]`, `sort`)
- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (las lecturas no se loguean)
- `B/features/usuarios/service/UsuarioPersonalService.java` (método `listar`)

## Solicitud de negocio

El Administrador entra a «Convocatorias», ve la lista con gestión, fechas, cupos y estado, abre una para ver el detalle y, al armar una, elige los tipos de documento que se exigen.

## Objetivo

`ConvocatoriaService` lista y consulta convocatorias, y `TipoDocumentoService` lista el catálogo de tipos de documento.

## Alcance

- `B/features/convocatorias/service/ConvocatoriaService.java` (`@Service`, `@RequiredArgsConstructor`, `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")` en cada método público; las UT 08, 09 y 11 agregan sus métodos en esta misma clase):
  - `listar(int page, int pageSize, String search, EstadoConvocatoria estado, UUID gestion, String sort)`: `@Transactional(readOnly = true)`. `search` se convierte en el patrón `%texto%` sin mayúsculas. `sort` admite `nombre`, `fechaApertura`, `fechaCierre`, `estado`, `creadoEn` (por defecto `-creadoEn`; mapa campo → propiedad como `CAMPOS_ORDENABLES` de `UsuarioPersonalService`); otro valor es `ValidationException` `ERR_CV_11`. Devuelve `PaginaConvocatorias(datos: List<ConvocatoriaResumenResponse>, meta: PageMeta)`.
  - `obtener(UUID codigo)`: `@Transactional(readOnly = true)`. Si no existe, `NotFoundException` `ERR_CV_01`. Devuelve `ConvocatoriaResponse` con sus documentos y `tienePostulaciones` (vía `PostulacionesDeConvocatoriaService`).
  - Método privado `aRespuesta(Convocatoria)` reutilizable por las UT 08, 09 y 10 para armar el `ConvocatoriaResponse`.
- `B/features/convocatorias/service/TipoDocumentoService.java`: `listar(int page, int pageSize, String search, String sort)` con `sort` `nombre` (por defecto `nombre`), `ERR_CV_11` si es otro; devuelve `PaginaTiposDocumento`.
- Los `filter[estado]` y `filter[gestion]` llegan ya convertidos desde el controller (un valor de `estado` que no es del enum es `400` allá, `UT-CU13-BE-13`).
- Tests unitarios: lista paginada con meta correcta; filtros y búsqueda se pasan al repositorio; `sort` inválido → `ERR_CV_11`; `obtener` inexistente → `ERR_CV_01`; `tienePostulaciones` refleja `PostulacionesDeConvocatoriaService`; una convocatoria sin documentos devuelve lista vacía.

Archivos esperados (crear/tocar): `ConvocatoriaService.java`, `TipoDocumentoService.java` y sus tests.

## Fuera de alcance (no tocar)

- Crear, editar, abrir, cerrar o eliminar (UT 08 a 11).
- Los controllers (`UT-CU13-BE-13`).
- Devolver la lista a los Postulantes: no existe en este CU (eso es del CU02).

## Resultado esperado

El Administrador puede consultar la lista y el detalle de convocatorias y el catálogo de tipos de documento.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
