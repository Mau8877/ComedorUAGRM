# UT-CU09-BE-07 — Prueba de integración de punta a punta

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-BE-07` |
| **Nombre** | Prueba de integración de punta a punta |
| **Caso de uso** | CU09-BE |
| **Depende de** | `UT-CU09-BE-06` |

## Antes de implementar

- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md) (Postgres real con `PostgresTestDatabase`, limpiar la base al terminar, tests que no se pisan).
- Como referencia: `T/features/actas/ActaAdjudicacionIntegracionTest.java` y `T/features/becas/PadronIntegrationTest.java`.

## Solicitud de negocio

Comprobar con una base real que el CU funciona completo y que ningún dato sensible se filtra.

## Objetivo

`PlanillaIntegrationTest` recorre el CU por HTTP contra Postgres real con MinIO y el reloj simulados.

## Alcance

- `T/features/planillas/PlanillaIntegrationTest.java` (`@SpringBootTest` + `MockMvc`, `AlmacenamientoService` simulado en memoria, `Clock` simulado, rate limit subido): 
  - Generar: fila, archivo subido, texto del PDF (convocatoria, periodo, vigentes ordenados por apellido, sin baja ni sus CI) y bitácora sin nombres, CI ni ruta; no cambia el padrón.
  - La planilla refleja el padrón al generar (una baja y un alta entre dos planillas).
  - Rechazos: periodo inválido (`ERR_PLA_01`, `02`, `03`), fechas faltantes (`ERR_SYS_01`), sin becas vigentes (`ERR_PLA_04`) y convocatoria inexistente (`ERR_PRI_01`), sin generar nada.
  - Resumen, listado con `meta`, `sort` válido y no soportado (`ERR_SYS_05`) y `filter` no soportado.
  - Descarga con bitácora, planilla inexistente (`ERR_PLA_05`) y archivo faltante (`ERR_PLA_06` sin bitácora).
  - Vista previa en PDF «BORRADOR» sin huella y con el mismo error que generar.
  - Un postulante recibe `403` `ERR_AUTH_07` y sin sesión `401` en todos los endpoints; `PUT`, `PATCH` y `DELETE` dan `405`.
- Limpia las tablas al terminar (`@AfterEach`) para no afectar a los tests de repositorio que comparten la base.

Archivos esperados (crear): `PlanillaIntegrationTest.java`.

## Fuera de alcance (no tocar)

- Probar MinIO real (se simula) o la pantalla web.
- Pruebas de rendimiento con 20.000 postulaciones.

## Resultado esperado

El CU queda cubierto de punta a punta con una base real.

## Validación

1. Con un Postgres vacío: `./mvnw test -Dtest=PlanillaIntegrationTest -Dtest.postgres.url=jdbc:postgresql://localhost:55432/<base_vacia>` pasa (15 tests).
2. `./mvnw test` completo no agrega fallos nuevos respecto de `main` (los preexistentes son los `@SpringBootTest` de controller sin `PostgresTestDatabase` y dos tests documentados en la memoria del proyecto).
