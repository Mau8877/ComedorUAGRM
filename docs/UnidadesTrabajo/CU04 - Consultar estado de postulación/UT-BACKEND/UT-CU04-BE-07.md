# UT-CU04-BE-07 — Pruebas de integración con base real

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-BE-07` |
| **Nombre** | Pruebas de integración con base real |
| **Caso de uso** | CU04-BE |
| **Depende de** | `UT-CU04-BE-04`, `UT-CU04-BE-05`, `UT-CU04-BE-06` |

## Antes de implementar

- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md): `support/PostgresTestDatabase`, SKIPPED sin Docker, un Postgres por JVM y limpiar la base en `@AfterEach` si se commitea.
- Como referencia: el test de integración del CU-22 (`resultados`).

## Solicitud de negocio

Hay que probar de punta a punta, con base real, que el postulante ve lo suyo y nada más.

## Objetivo

Existe un test de integración (`@SpringBootTest` + `MockMvc`) que cubre los criterios de aceptación del CU-04.

## Alcance

`MisPostulacionesIntegrationTest` en la raíz del módulo de postulaciones, que verifica:

- CA01–CA03 y CA12: listado ordenado de la más reciente a la más antigua, vacío sin postulaciones, filtro por gestión y sin borradores.
- CA04: detalle con historial y sin autor.
- CA05, CA06, CA09: observaciones de una devolución, motivo de archivo o rechazo y bloque de finalista.
- CA07, CA08: «calificada» y fila oculta antes de publicar; tras publicar, «adjudicada» o «no adjudicada» con el código de la convocatoria y sin el motivo de la decisión final.
- CA10: el aviso de la bandeja incluye `postulacionCodigo`.
- CA11: postulación ajena e inexistente dan el mismo `404` `ERR_POS_01`; `401` sin sesión; `403` con otro rol.
- CA13: tras consultar, el conteo de filas de `postulacion`, `postulacion_historial`, `notificacion` (campo `leida`) y `bitacora` no cambia.
- El número de consultas del listado no crece con la cantidad de postulaciones.
- La base queda vacía al terminar (`@AfterEach`).

Archivos esperados (crear): el test de integración.

## Fuera de alcance (no tocar)

- Código de producción (corregir un fallo se hace en la UT de origen).
- Pruebas de otros CU.

## Resultado esperado

Los criterios de aceptación del CU-04 quedan verificados con base real.

## Validación

1. `./mvnw test` pasa (o queda SKIPPED sin Docker, nunca en rojo).
2. `./mvnw checkstyle:check` sin errores.
