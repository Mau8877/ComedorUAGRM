# UT-CU25-BE-01 — Consulta de la bitácora (repositorio, filtros y servicio)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU25-BE-01` |
| **Nombre** | Consulta de la bitácora (repositorio, filtros y servicio) |
| **Caso de uso** | CU25-BACKEND |
| **Depende de** | — (la tabla y el model `Bitacora` ya existen) |

## Antes de implementar

[`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (formato de `sort`, paginación) y
[`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (las lecturas no se loguean).

## Solicitud de negocio

El Administrador necesita encontrar acciones concretas sin recorrer toda la bitácora.

## Objetivo

Un servicio que devuelve una página de la bitácora filtrada por usuario, acción y rango de fechas.

## Alcance

- `BitacoraSpecs.filtrar(patron, accion, desde, hasta)`: filtros opcionales combinados con AND; `hasta` exclusivo.
- `BitacoraRepository`: `JpaSpecificationExecutor`, `buscar(...)` con `@EntityGraph("usuario")` y `accionesRegistradas()`.
- `BitacoraConsultaService.listar(...)` y `acciones()`, con `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")`.
  Convierte los días de calendario a instantes en `America/La_Paz`; orden por defecto `-fecha`, con `id` de desempate.
- `desde` posterior a `hasta` → `ERR_BIT_02`; `sort` no soportado → `ERR_BIT_01`.

Archivos esperados (crear/tocar): `BitacoraSpecs.java` (crear), `BitacoraConsultaService.java` (crear),
`BitacoraRepository.java` (editar).

## Fuera de alcance (no tocar)

- `BitacoraService`, `AuditoriaAspect` y `BitacoraAcciones`: la escritura no cambia.
- Controller y DTO (UT-CU25-BE-02).

## Resultado esperado

`listar(...)` devuelve la página y su `meta`, o lanza el error catalogado que corresponda.

## Validación

1. `./mvnw compile` sin errores.
2. `./mvnw test -Dtest=BitacoraConsultaServiceTest` en verde (UT-CU25-BE-03).
3. `./mvnw checkstyle:check` sin errores.
