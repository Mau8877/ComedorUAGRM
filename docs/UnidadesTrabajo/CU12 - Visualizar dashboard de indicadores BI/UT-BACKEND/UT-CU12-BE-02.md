# UT-CU12-BE-02 — Refresco por lote de la vista

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-02` |
| **Nombre** | Refresco por lote de la vista |
| **Caso de uso** | CU12-BE |
| **Depende de** | `UT-CU12-BE-01` |

## Antes de implementar

- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (INFO/ERROR en el `service`, nunca datos personales) y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).
- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md): `config/` para los beans de Spring.

## Solicitud de negocio

Los indicadores se actualizan solos cada noche para que la consulta del Directivo sea rápida y no afecte a las operaciones.

## Objetivo

La vista se refresca cada noche a las 02:00 y también al arrancar si está vacía.

## Alcance

- `B/config/SchedulingConfig.java` con `@EnableScheduling` (el proyecto hoy no lo tiene).
- `B/features/dashboard/service/RefrescoIndicadoresService.java`:
  - `refrescar()`: ejecuta `REFRESH MATERIALIZED VIEW CONCURRENTLY mv_indicadores_convocatoria` con `JdbcTemplate`; loguea `INFO` con la duración y `ERROR` si falla (sin datos de postulantes); un fallo **no** tumba la aplicación ni borra los datos anteriores.
  - `refrescarSiEstaVacia()`: si `count(*) = 0` en la vista, hace un `REFRESH` sin `CONCURRENTLY` (en una vista vacía el modo concurrente no sirve).
- `B/features/dashboard/service/RefrescoIndicadoresJob.java`: `@Scheduled(cron = "${app.dashboard.refresco-cron:0 0 2 * * *}", zone = "America/La_Paz")` que llama a `refrescar()`, y un `@EventListener(ApplicationReadyEvent.class)` que llama a `refrescarSiEstaVacia()`.
- `R/application.properties`: `app.dashboard.refresco-cron=${DASHBOARD_REFRESCO_CRON:0 0 2 * * *}` y su variable en `.env.example` del backend.
- Tests: `refrescar()` actualiza `calculado_en` y refleja filas nuevas (base real); `refrescarSiEstaVacia()` solo actúa con la vista vacía; un error de SQL se loguea y no se propaga fuera del job; el cron toma el valor de la propiedad.

Archivos esperados (crear): `SchedulingConfig`, `RefrescoIndicadoresService`, `RefrescoIndicadoresJob` y sus tests. (Tocar): `application.properties`, `.env.example`.

## Fuera de alcance (no tocar)

- Un botón o endpoint para refrescar a mano.
- Coordinar varias réplicas.
- La asistencia (CU-10).

## Resultado esperado

Cada noche la vista se actualiza sola y, tras instalar el sistema, no queda vacía.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Con el cron cambiado a un minuto en un entorno de prueba, `calculado_en` avanza.
