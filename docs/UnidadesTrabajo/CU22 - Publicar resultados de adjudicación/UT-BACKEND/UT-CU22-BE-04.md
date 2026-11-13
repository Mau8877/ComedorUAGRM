# UT-CU22-BE-04 — Consulta de resultados

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-BE-04` |
| **Nombre** | Consulta de resultados |
| **Caso de uso** | CU22-BE |
| **Depende de** | `UT-CU22-BE-01` |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/backend/ARQUITECTURA_BACKEND.md), [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (paginación y `sort`).
- Como referencia: `CierrePostulacionConsultaRepository` (CU-20) y la consulta de adjudicación del CU-05.

## Solicitud de negocio

Publicar, resumir y consultar resultados necesita leer decisiones, finalistas y datos públicos de los adjudicados sin acoplar módulos.

## Objetivo

Existe una consulta propia con todo lo que los `service` del módulo necesitan.

## Alcance

- `B/features/resultados/repository/ResultadosConsultaRepository.java` (`NamedParameterJdbcTemplate`, respeta `eliminado_en IS NULL`; no lee el repository de otro módulo):
  - `convocatoria(UUID codigo)`: `Optional` con `id`, `codigo`, `nombre` y `estado`.
  - `bloquearConvocatoria(long id)`: `SELECT ... FOR UPDATE`.
  - `publicacion(long convocatoriaId)`: `Optional` con `publicadoEn` y nombre para mostrar de quien publicó.
  - `contarPorEstado(long convocatoriaId)`: adjudicados, no adjudicados (**solo por decisión de adjudicación**), finalistas en «calificada» y total de finalistas.
  - `destinatariosDelAviso(long convocatoriaId)`: **una sola consulta** que devuelve una fila por finalista **con `adjudicacion`** (nombre, usuario destinatario, correo, código de postulación y resultado), sin consultar por finalista; excluye archivados, rechazados y devueltos.
  - `decisiones(long convocatoriaId, FiltroDecisiones, Pageable)`: página de decisiones con `filter[resultado]`, `search` (nombres y apellidos) y `sort` (`puntajeFinal`, `apellidos`, `nombres`; por defecto `-puntajeFinal`). Un `sort` o `filter` no soportado lanza `ERR_SYS_05`.
  - `adjudicadosPublicos(long convocatoriaId, String search, Pageable)`: nombres, apellidos, `nota` (= `puntaje_final`), carrera y facultad **solo de adjudicados**; `search` sobre nombres, apellidos, carrera y facultad; `sort` (`nota`, `apellidos`, `nombres`; por defecto `-nota`). No selecciona CI ni correo. Los nombres de las tablas de carrera y facultad se verifican contra `DiagramaConceptual.mermaid` antes de escribir la consulta.
  - `miResultado(long convocatoriaId, UUID usuarioCodigo)`: resultado y nota del finalista con decisión de ese usuario, o vacío.
  - `convocatoriasPublicadas(Pageable)`: convocatorias con fila en `publicacion_resultado`, orden `-publicadoEn`.
- Tests con Testcontainers (se saltan sin Docker): conteos con finalistas en cada estado, destinatarios (excluye archivados, rechazados y devueltos), paginación y orden, `search`, que la consulta pública **no** devuelve no adjudicados ni CI/correo y que un `sort` inválido falla, y que las consultas de lista (decisiones y adjudicados) resuelven todo con un número constante de consultas, sin N+1.

Archivos esperados (crear): `ResultadosConsultaRepository` y su test.

## Fuera de alcance (no tocar)

- Escribir en la base (UT-06 guarda por JPA).
- Reglas de negocio y errores de negocio (UT-05, 06 y 07).
- Modificar las consultas de CU-05, 19, 20 o 21.

## Resultado esperado

Los `service` leen todo lo necesario con consultas propias, paginadas y sin datos sensibles.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
