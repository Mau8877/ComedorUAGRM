# UT-CU13-BE-14 — Pruebas de integración con base real

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU13-BE-14` |
| **Nombre** | Pruebas de integración con base real |
| **Caso de uso** | CU13-BE |
| **Depende de** | `UT-CU13-BE-12`, `UT-CU13-BE-13` |

## Antes de implementar

- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md#tests-de-integración-con-bd-real-testcontainers) (`PostgresTestDatabase`, SKIPPED sin Docker, un Postgres por JVM, limpiar al terminar)
- `T/features/usuarios/UsuarioPersonalIntegrationTest.java` o el test de integración de otro CU como referencia de estructura

## Solicitud de negocio

Las reglas más delicadas de este CU (una abierta por gestión, el cierre automático, el borrado lógico con clave compuesta) dependen de la base real y no se pueden confiar a un mock.

## Objetivo

Los flujos completos del CU quedan probados de punta a punta (HTTP → servicio → PostgreSQL → bitácora), incluyendo el cierre automático.

## Alcance

- `T/features/convocatorias/ConvocatoriaIntegrationTest.java` (`@SpringBootTest` + `MockMvc`, base real con `PostgresTestDatabase`, sesión de Administrador). Casos:
  - Crear una gestión, crear una convocatoria con documentos y leerla: queda en `BORRADOR`, con los documentos y las fechas en hora de La Paz.
  - Validaciones: fechas invertidas, sin documento obligatorio, tipo repetido, tipo inexistente.
  - Abrir: una `BORRADOR` pasa a `ABIERTA`; abrir otra en la **misma** gestión → `409` `ERR_CV_07`, y en **otra** gestión sí; dos aperturas simultáneas dejan una sola `ABIERTA`.
  - Cerrar a mano y cierre automático: una `ABIERTA` con cierre vencido pasa a `CERRADA` al ejecutar `cerrarVencidas()`; el evento queda en `bitacora` con `usuario_id` nulo.
  - Editar: reemplazar documentos; quitar y volver a agregar un tipo reactiva la misma fila (no viola la clave primaria); `CERRADA` → `409` `ERR_CV_08`.
  - Con postulaciones (se inserta una): cambiar fecha de apertura o documentos → `409` `ERR_CV_09`; eliminar → `409` `ERR_CV_10`.
  - Eliminar una `BORRADOR` sin postulaciones: desaparece de la lista y sus documentos también.
  - Bitácora: cada operación deja su fila con la acción, el actor, la entidad y el `detalle` esperados, y **no** deja fila cuando la operación falla.
  - Seguridad: sin sesión `401`, con rol Postulante `403` (`ERR_AUTH_07`).
  - `cantidadFinalistas`: aparece en las respuestas de este CU y **no** en ningún endpoint del Postulante (por ejemplo `GET /api/v1/postulaciones` y el detalle de una postulación).
- El test limpia lo que commitea (`@AfterEach`), como pide `TESTING_BACKEND.md`, para no ensuciar los tests de repositorio que corren después.
- `docs/errors/convocatorias/ERRORES_CONVOCATORIAS.md`: se verifica que los «Dónde se lanza» coincidan con las clases y métodos finales.

Archivos esperados (crear/tocar): `ConvocatoriaIntegrationTest.java` y, si hace falta, ajustes a `ERRORES_CONVOCATORIAS.md`.

## Fuera de alcance (no tocar)

- Pruebas unitarias de cada servicio (ya están en sus UT).
- Pruebas de rendimiento o de carga.
- Pruebas del frontend (`UT-FRONTEND`).

## Resultado esperado

Los criterios CA01 a CA17 del CU que son de backend tienen una prueba contra una base real.

## Validación

1. `./mvnw test` pasa (con Docker disponible el test corre; sin Docker queda `SKIPPED`, o con `-Dtest.postgres.url=...` contra una base vacía descartable).
2. `./mvnw checkstyle:check` sin errores.
3. `./mvnw clean package` sin errores.
