# UT-CU07-BE-06 — Consulta del expediente del finalista

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-BE-06` |
| **Nombre** | Consulta del expediente del finalista |
| **Caso de uso** | CU07-BE |
| **Depende de** | `UT-CU07-BE-03`, `UT-CU07-BE-04` |

## Antes de implementar

- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) y la referencia `B/features/finalistas/repository/FinalistaConsultaRepository.java` (consultas JDBC con borrado lógico).

## Solicitud de negocio

El administrador necesita ver todo el expediente en un solo lugar antes de decidir.

## Objetivo

`ExpedienteService.obtener` devuelve el expediente completo de un finalista.

## Alcance

- `B/features/expedientes/repository/ExpedienteConsultaRepository.java` (JDBC, solo lectura, `eliminado_en IS NULL`): datos del postulante (nombres, apellidos, CI, carrera), convocatoria, respuesta socioeconómica (incluido `promedio_academico` declarado, como referencia para el puntaje académico), finalista (posición, plazo, entrega), estado de la postulación, puntaje vigente de la IA con su `detalle`, documentos y auditorías.
- Documentos: una fila por cada tipo que exige la convocatoria (`convocatoria_documento`) con `estado` `RECIBIDO` o `FALTA` y, si hay adjunto, su nombre de archivo, tipo MIME y código; más los adjuntos de tipos que la convocatoria no exige, si los hay.
- Auditorías: de la más reciente a la más antigua, con resultado, observaciones, puntaje académico, fecha y nombre del auditor.
- `ExpedienteService.obtener(finalistaCodigo)` con `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")` y `@Transactional(readOnly = true)`: finalista inexistente o eliminado → `404` `ERR_FIN_01`.
- **Puntaje total** = puntaje de la IA + puntaje académico de la **última** auditoría; es `null` si no hay auditoría, si la última no tiene puntaje académico o si no hay puntaje de la IA. No se guarda.
- No loguea datos personales ni socioeconómicos. No anota la consulta en la bitácora.
- Tests unitarios del cálculo del total y de la derivación de «Recibido»/«Falta»; con base real: un finalista con y sin documentos, con varias auditorías y con borrado lógico.

Archivos esperados (crear): `ExpedienteConsultaRepository`, `ExpedienteService` (la parte de consulta) y sus tests.

## Fuera de alcance (no tocar)

- Registrar auditorías (UT-07).
- URLs de los documentos (UT-09).
- Endpoint (UT-10).

## Resultado esperado

Dado un finalista, el servicio devuelve el expediente con documentos, puntajes y auditorías.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
