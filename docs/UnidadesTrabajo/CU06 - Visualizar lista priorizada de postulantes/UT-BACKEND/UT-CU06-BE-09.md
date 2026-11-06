# UT-CU06-BE-09 — Pruebas de integración con base real y de rendimiento

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-09` |
| **Nombre** | Pruebas de integración con base real y de rendimiento |
| **Caso de uso** | CU06-BE |
| **Depende de** | `UT-CU06-BE-03`, `UT-CU06-BE-08` |

## Antes de implementar

- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md) (`PostgresTestDatabase`, un contenedor por JVM, pool chico, limpiar lo que se commitea).
- Como referencia: `T/features/auth/AuthIntegrationTest.java` (u otro test de integración existente).

## Solicitud de negocio

Confirmar de punta a punta que la lista cumple los criterios de aceptación del CU, incluido el tiempo con el volumen real.

## Objetivo

Un test de integración recorre el CU completo contra una base real y mide el rendimiento con 20 000 postulaciones.

## Alcance

- `T/features/postulaciones/ListaPriorizadaIntegrationTest.java` (`@SpringBootTest` + `MockMvc` + base real; sin Docker queda `SKIPPED`):
  - Administrador con sesión consulta la lista: orden, posición, un solo puntaje por postulación, estados excluidos, resumen con pendientes (CA01 a CA04, CA14).
  - Paginación y posición en la página 2 (CA05, CA06); búsqueda y filtro con posición real (CA07); empates estables recorriendo todas las páginas (CA08).
  - Reprocesar (cambiar el puntaje vigente) y volver a consultar (CA10).
  - `401` sin sesión, `403` con un Postulante, `404` con una convocatoria inexistente (CA11 a CA13); la consulta no cambia ningún estado (CA14).
  - Exportación: el PDF descargado empieza con `%PDF`, respeta el filtro, la lista vacía responde `400` `ERR_PRI_03`, y queda una fila de bitácora sin nombres ni CI (CA16 a CA20).
  - Sin tokens ni contraseñas en los logs capturados.
- **Rendimiento (CA09 y CA21):** un test marcado (por ejemplo `@Tag("rendimiento")`, fuera de la corrida normal) carga 20 000 postulaciones calificadas con el cargador de UT-03 y verifica que **cualquier página** de la lista responde en menos de 3 segundos y que la exportación termina sin `OutOfMemoryError`. Se registra en el PR el tiempo real del PDF (el máximo aceptable lo decide el equipo con esa medida) y el `EXPLAIN` de la consulta de la lista confirmando que usa los índices de UT-01.
- El test deja la base vacía al terminar (`@AfterEach`).

Archivos esperados (crear): `ListaPriorizadaIntegrationTest.java` y el test de rendimiento. (Tocar): ninguno.

## Fuera de alcance (no tocar)

- Pruebas de la pantalla (UT-FRONTEND).
- Pruebas de carga con muchos usuarios a la vez (el CU menciona 50 administradores; se deja como follow-up si hace falta medirlo).

## Resultado esperado

Los criterios de aceptación del CU están verificados contra una base real y se conoce el tiempo con 20 000 postulaciones.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw test -Dtest=ListaPriorizadaIntegrationTest -Drendimiento=true` pasa con 20 000 postulaciones y muestra los tiempos.
3. `./mvnw checkstyle:check` sin errores.

## Resultado de la medición (con el código ya implementado)

Postgres 18 local, cargador de UT-03 con 20 000 postulantes (13 922 calificadas), sin Docker:

| Operación | Tiempo |
|-----------|--------|
| Carga de datos de prueba (20 000 postulantes) | 24 s |
| Páginas 1, 2, 100, 350 y 700 de la lista (20 por página) | 56 a 150 ms |
| Exportar a PDF toda la lista (13 922 filas, 833 KB) | 4,5 s |

Se corre con `./mvnw test -Dtest=ListaPriorizadaIntegrationTest -Drendimiento=true` (más `-Dtest.postgres.url=…` si Testcontainers no conecta). El máximo aceptable para el PDF lo decide el equipo con esta medida.
