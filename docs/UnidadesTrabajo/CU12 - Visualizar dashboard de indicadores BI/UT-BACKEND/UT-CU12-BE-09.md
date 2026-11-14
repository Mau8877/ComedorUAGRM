# UT-CU12-BE-09 — Pruebas de integración con base real

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-09` |
| **Nombre** | Pruebas de integración con base real |
| **Caso de uso** | CU12-BE |
| **Depende de** | `UT-CU12-BE-02`, `UT-CU12-BE-07`, `UT-CU12-BE-08` |

## Antes de implementar

- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md): `PostgresTestDatabase`, un solo Postgres por JVM, pool chico, y los tests que commitean limpian la base en `@AfterEach`.
- Como referencia: la prueba de integración del CU-22 (`UT-CU22-BE-11`).

## Solicitud de negocio

Antes de dar el CU por hecho hay que probar el recorrido completo contra una base real.

## Objetivo

Una prueba de integración recorre el dashboard de punta a punta y cubre los criterios de aceptación del backend.

## Alcance

`T/features/dashboard/DashboardIntegrationTest.java` (`@SpringBootTest` + `MockMvc` + base real; sin Docker queda `SKIPPED`):

- **CA01, CA05:** con datos sembrados y la vista refrescada, `GET /indicadores` trae los totales de la gestión vigente y `actualizadoEn`; con la vista sin refrescar tras crear datos nuevos, sigue mostrando lo último calculado.
- **CA02, CA03:** filtrar por gestión y por convocatoria cambia los totales y la serie; una convocatoria que no es de la gestión da `404` `ERR_DAS_01`.
- **CA04:** una gestión sin datos responde `200` con `sinDatos = true` y ceros.
- **CA06, CA07:** un borrador no suma; las adjudicaciones de una convocatoria sin publicar no suman y las de una publicada sí.
- **CA08:** `asistenciaPromedio` y `asistencia` vienen `null`.
- **CA09:** consultar y descargar no dejan filas nuevas en `bitacora` ni cambian ninguna tabla.
- **CA10:** un Administrador DUBSS o un Postulante reciben `403` `ERR_AUTH_07`; sin sesión `401`; gestión inexistente `404`.
- **CA11, CA12:** el PDF empieza con `%PDF`; sin datos responde `400` `ERR_DAS_02` y no genera nada.
- **CA13:** con 20 000 postulaciones sembradas en una convocatoria y la vista refrescada, la consulta responde en 3 segundos o menos.
- **Refresco:** `refrescar()` deja `calculado_en` más reciente; `refrescarSiEstaVacia()` no hace nada si ya hay datos.
- Limpia lo que commitea en `@AfterEach`.

Archivos esperados (crear): `DashboardIntegrationTest`.

## Fuera de alcance (no tocar)

- Pruebas del frontend (UT-FRONTEND).
- Cambiar código de producción: si la prueba revela un fallo, se corrige en la UT que lo origina.

## Resultado esperado

La suite demuestra, con base real, que el backend cumple los criterios de aceptación del CU12.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Cada CA de la tabla del README tiene al menos una aserción.
