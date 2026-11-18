# UT-CU12-BE-13 — Datos de prueba y pruebas de integración del análisis BI

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-13` |
| **Nombre** | Datos de prueba y pruebas de integración del análisis BI |
| **Caso de uso** | CU12-BE |
| **Depende de** | `UT-CU12-BE-08`, `UT-CU12-BE-09`, `UT-CU12-BE-12` |

## Antes de implementar

- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md): un solo Postgres por JVM y los tests que commitean limpian la base.

## Solicitud de negocio

En desarrollo el Directivo debe ver el análisis con datos y los criterios deben probarse contra una base real.

## Objetivo

La semilla del dashboard trae becas, asistencia y puntajes, y la integración cubre CA14–CA22.

## Alcance

- `B/config/DatosPruebaDashboard.java`: puntajes de la IA repartidos en tramos, una beca vigente por adjudicada y asistencia de los días hábiles del 26 de octubre al 17 de noviembre, con tasas alta, media y baja (hay becas en riesgo); al final se refrescan las cinco vistas.
- `T/config/DatosPruebaDashboardTest.java`: carga becas, asistencia y las vistas BI; `borrarTodo` borra también `asistencia`, `movimiento_beca` y `beca`.
- `T/features/dashboard/DashboardIntegrationTest.java` (22 pruebas): asistencia real (83,3 % y variación), `/bi` (embudo, facultades, comparación, tendencia, 401/403), Excel (hojas y `ERR_DAS_02`), sin escrituras (huella con `beca` y `asistencia`) y limpieza de las cinco vistas en `@AfterEach`.
- `T/features/dashboard/repository/VistasBiMigracionTest.java`.

## Fuera de alcance (no tocar)

- Las pruebas de la pantalla (ver `UT-FRONTEND`).

## Resultado esperado

`mvn test` del módulo `dashboard` en verde y la semilla muestra datos de BI en dev.

## Validación

1. `mvn test -Dtest='com.comedoruagrm.backend.features.dashboard.**.*Test'` (125 pruebas) y `-Dtest='DatosPrueba*Test'` (46) sin fallos.
2. `mvn checkstyle:check` limpio.
