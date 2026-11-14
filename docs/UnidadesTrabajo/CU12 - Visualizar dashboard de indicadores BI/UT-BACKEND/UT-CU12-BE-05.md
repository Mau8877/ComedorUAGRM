# UT-CU12-BE-05 — Consulta de indicadores

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-05` |
| **Nombre** | Consulta de indicadores |
| **Caso de uso** | CU12-BE |
| **Depende de** | `UT-CU12-BE-01`, `UT-CU12-BE-03`, `UT-CU12-BE-04` |

## Antes de implementar

- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (una consulta de lectura no se loguea) y [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).

## Solicitud de negocio

El Directivo ve los indicadores de la gestión o convocatoria que elige, con la fecha en que se calcularon.

## Objetivo

El `service` devuelve los totales y la serie por mes de la selección, leyendo solo la vista.

## Alcance

- `B/features/dashboard/repository/IndicadoresRepository.java` (JDBC sobre `mv_indicadores_convocatoria`):
  - totales y serie por mes de una gestión (con `convocatoria_id` opcional), en una consulta cada uno;
  - `max(calculado_en)` de toda la vista;
  - resolución de la gestión y la convocatoria por su `codigo` y verificación de que la convocatoria pertenece a la gestión.
- `B/features/dashboard/service/IndicadoresService.java`:
  - `@PreAuthorize("hasRole('DIRECTIVO')")` y `@Transactional(readOnly = true)`. Sin `@Auditar`: no se anota en la bitácora (CA09).
  - `IndicadoresResponse consultar(UUID gestion, UUID convocatoria)`:
    - sin ninguno: gestión vigente (`gestionVigente()` de UT-04); con solo la convocatoria: la gestión de esa convocatoria; con ambos: se comprueba que coincidan;
    - gestión o convocatoria inexistente, o que no coinciden → `NotFoundException` `ERR_DAS_01`;
    - sin gestiones: respuesta con `gestion = null`, totales en cero y `sinDatos = true` (no es error);
    - la serie `porMes` se completa con ceros para los meses sin filas entre el primero y el último con datos;
    - `actualizadoEn` = `max(calculado_en)`; `null` si el lote nunca se ejecutó;
    - `sinDatos` = verdadero si todos los totales son cero;
    - `asistenciaPromedio` y `asistencia` = `null`, con el comentario `// TODO CU-10: calcular el promedio de asistencia cuando exista el registro de asistencia`.
  - La misma lógica la reutiliza el PDF (UT-06) a través de un método interno `obtener(...)` sin repetir `@PreAuthorize`.
- Tests unitarios (repository simulado): resolución de gestión y convocatoria, los tres `404`, la serie sin huecos, `sinDatos`, `actualizadoEn` nulo, un usuario sin rol Directivo recibe `403`. Con base real: los números salen de la vista y no de las tablas transaccionales; una consulta con 20 000 postulaciones sembradas responde en menos de 3 segundos.

Archivos esperados (crear): el repository, el `service` y sus tests.

## Fuera de alcance (no tocar)

- El endpoint (UT-07) y el PDF (UT-06).
- Calcular la asistencia (CU-10).
- Leer `postulacion`, `finalista` ni `adjudicacion` desde este módulo.

## Resultado esperado

Dada una selección, el `service` devuelve totales y serie por mes coherentes con la vista, y el aviso de «sin datos» o de «lote sin ejecutar» cuando corresponde.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. El plan de ejecución de la consulta usa los índices de la vista (sin recorrer las tablas transaccionales).
