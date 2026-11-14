# UT-CU06-BE-06 — Consulta de la lista priorizada

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-06` |
| **Nombre** | Consulta de la lista priorizada (orden, posición, búsqueda, filtro, paginación) |
| **Caso de uso** | CU06-BE |
| **Depende de** | `UT-CU06-BE-02`, `UT-CU06-BE-04` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (paginación, `filter[...]`, `sort`) y [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (índices parciales, borrado lógico).
- Como referencia: `UsuarioPersonalService.listar` y `UsuarioRepository.buscarPersonal` (patrón de búsqueda, `PageMeta`).

## Solicitud de negocio

El administrador ve a los mejor puntuados primero, con su posición real, y puede buscar a una persona o acotar por carrera sin perder esa posición.

## Objetivo

`ListaPriorizadaService.listarPriorizados(...)` devuelve una página de la lista con la posición calculada sobre toda la convocatoria, en menos de 3 segundos con 20 000 postulaciones.

## Alcance

- `ListaPriorizadaRepository.buscar(...)` y `contar(...)`: consulta SQL con una CTE (en `ListaPriorizadaRepository`, con `NamedParameterJdbcTemplate`; no un `@Query` de Spring Data):
  1. **Base:** postulaciones no eliminadas de la convocatoria con `estado = 'calificada'` y su puntaje `vigente` no eliminado, con `ROW_NUMBER() OVER (ORDER BY puntaje_total DESC, fecha_envio ASC, postulacion.id ASC)` como `posicion`.
  2. **Filtros después de numerar:** por carrera (`carrera.codigo_carrera`) y por búsqueda (nombres y apellidos o CI, sin distinguir mayúsculas, con el mismo patrón de `UsuarioPersonalService`).
  3. Pagina con `LIMIT/OFFSET` y tiene su propio `count`. Une `usuario`, `postulante` y `carrera` en esa misma consulta (solo lectura; ver la decisión de dependencia del README).
  - Una proyección (interface o record) con los campos de `PostulacionPriorizadaResponse`.
- `ListaPriorizadaService.listarPriorizados(UUID convocatoriaCodigo, String search, UUID carreraCodigo, int page, int pageSize, String sort)`, `@Transactional(readOnly = true)` y `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")`:
  - `page` y `pageSize` menores a 1 → `ValidationException` `ERR_SYS_01`; `sort` recibido → `ValidationException` `ERR_PRI_02`.
  - Convocatoria inexistente o eliminada → `NotFoundException` `ERR_PRI_01`.
  - Devuelve `PaginaListaPriorizada(List<PostulacionPriorizadaResponse>, PageMeta)`.
  - Sin logs (es lectura) y sin tocar el estado de ninguna postulación.
- Tests de servicio (mocks): `sort` rechazado, página inválida, convocatoria inexistente, mapeo a la respuesta y `meta`.
- **Tests con base real** (`@DataJpaTest` o integración, usando datos armados en el propio test): orden por puntaje; **desempate** estable por `fecha_envio` e `id` (recorrer todas las páginas no repite ni omite); la postulación con dos puntajes aparece **una vez** y con el vigente; las `pendiente`, `devuelta`, `archivada`, `adjudicada` y `no_adjudicada` no aparecen; el filtro y la búsqueda **conservan la posición real** (el 37.º filtrado sigue siendo el 37.º); página 2 con 20 por página empieza en la posición 21; un reproceso (cambiar el puntaje vigente) cambia el orden en la siguiente consulta.

Archivos esperados (crear): los tests. (Tocar): `PostulacionRepository.java`, `ListaPriorizadaService.java` (de UT-05).

## Fuera de alcance (no tocar)

- Los endpoints (UT-08) y el PDF (UT-07).
- Un parámetro `sort` o un orden configurable.
- Guardar la posición (la guarda el CU-05 al elegir a un finalista).
- Calcular o reprocesar puntajes.

## Resultado esperado

La lista sale ordenada, paginada y con la posición correcta, también cuando se busca o se filtra.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. La medición con 20 000 postulaciones se hace en UT-09.
