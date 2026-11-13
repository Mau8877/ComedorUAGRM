# UT-CU08-BE-04 — Consultas del módulo de actas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-BE-04` |
| **Nombre** | Consultas del módulo de actas |
| **Caso de uso** | CU08-BE |
| **Depende de** | `UT-CU08-BE-01` |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/backend/ARQUITECTURA_BACKEND.md) (regla de dependencia: ningún módulo lee el repository del otro) y [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (paginación, `search`, `sort`).
- Como referencia: `ResultadosConsultaRepository` (CU-22), incluidos `ordenar` y `buscar`.

## Solicitud de negocio

El acta necesita leer, en pocas consultas, los datos de la convocatoria y los adjudicados con su carrera, y la pantalla necesita listar actas y convocatorias pendientes.

## Objetivo

Existe un repository JDBC con todas las lecturas del módulo, listas para los services.

## Alcance

- `B/features/actas/repository/ActasConsultaRepository.java` (`NamedParameterJdbcTemplate`), con los `record` internos que haga falta:
  - `convocatoria(UUID codigo)` → `id`, `codigo`, `nombre`, `estado`, `gestion` (etiqueta de `gestion_academica`), `fechaApertura`, `fechaCierre`, `cupos` y `puntajeMinimo`.
  - `bloquearConvocatoria(long id)` (`SELECT ... FOR UPDATE`).
  - `publicacion(long convocatoriaId)` → fecha de publicación o vacío (de `publicacion_resultado`, solo filas no eliminadas).
  - `adjudicadosDelActa(long convocatoriaId)` → una sola consulta: `nombres`, `apellidos`, `ci`, `carrera` y `puntajeFinal` de las adjudicaciones con resultado `ADJUDICADA`, ordenadas por `puntaje_final` descendente (nulos al final) y luego por `finalista.posicion`. **Solo adjudicados.**
  - `contarAdjudicados(long convocatoriaId)`.
  - `acta(long convocatoriaId)` → datos del acta con el nombre de quien la generó, o vacío.
  - `actas(String search, Pageable)` → página de actas con `adjudicados`; `search` en número y nombre de convocatoria; `sort` admite `numero` y `generadaEn`, por defecto `-generadaEn`; cualquier otro lanza `ValidationException` con `ERR_SYS_05` (mismo mecanismo que el CU-22).
  - `convocatoriasPendientes(Pageable)` → convocatorias con fila en `publicacion_resultado` y **sin** acta, con `adjudicados`, orden `-publicadoEn`.
  - `rutaDelActa(long convocatoriaId)` → ruta del archivo.
- Nunca se hace una consulta dentro de un bucle. Ningún campo de CI ni correo sale de este repository salvo `ci` en `adjudicadosDelActa` (para el PDF).
- Tests con base real (Testcontainers, se saltan sin Docker): trae solo adjudicados y en el orden pedido; no mezcla convocatorias; `convocatoriasPendientes` excluye las que ya tienen acta y las sin publicar; paginación, `search` y `sort`; `sort` inválido lanza `ERR_SYS_05`; las filas eliminadas no cuentan.

Archivos esperados (crear): `ActasConsultaRepository` y su test.

## Fuera de alcance (no tocar)

- Los services y la lógica de validación (UT-07 y UT-08).
- Reutilizar `ResultadosConsultaRepository` (es de otro módulo).
- Escribir datos: salvo `bloquearConvocatoria`, este repository solo lee.

## Resultado esperado

El módulo de actas lee todo lo que necesita con un número fijo de consultas.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
