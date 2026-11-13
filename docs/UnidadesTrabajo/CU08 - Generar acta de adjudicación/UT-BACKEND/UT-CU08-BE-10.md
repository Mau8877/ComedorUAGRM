# UT-CU08-BE-10 — Pruebas de integración con base real

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-BE-10` |
| **Nombre** | Pruebas de integración con base real |
| **Caso de uso** | CU08-BE |
| **Depende de** | `UT-CU08-BE-09` |

## Antes de implementar

- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md): patrón de Testcontainers con `assumeTrue` (el test se salta, no falla, sin Docker), `support/PostgresTestDatabase` y base limpia entre pruebas.
- Como referencia: `UT-CU22-BE-11`.

## Solicitud de negocio

Generar el acta toca varias tablas, un archivo y un correlativo con concurrencia: se prueba contra PostgreSQL real, de punta a punta.

## Objetivo

Hay pruebas de integración que recorren el flujo completo con base real y el almacenamiento simulado.

## Alcance

- `T/features/actas/ActaAdjudicacionIntegracionTest.java` (Testcontainers + `MockMvc`, con `AlmacenamientoService` simulado que guarda los bytes en memoria):
  - Generar con resultados publicados: `201`, una fila en `acta_adjudicacion`, número `001/AAAA`, archivo subido con la ruta esperada, PDF que contiene solo adjudicados (con nombre, CI, carrera y puntaje final, de mayor a menor) y bitácora `GENERACION_ACTA` sin datos sensibles.
  - Sin publicar: `409` `ERR_ACT_01`, nada guardado ni subido.
  - Segunda generación y dos peticiones simultáneas de la misma convocatoria: `409` `ERR_ACT_02`, una sola fila y un solo archivo.
  - Dos convocatorias publicadas generadas a la vez: números distintos y consecutivos.
  - Año nuevo: el correlativo reinicia en `001` (con el reloj simulado).
  - Convocatoria sin adjudicados: genera y el PDF dice «Sin adjudicados».
  - Un fallo simulado de MinIO: error `500` genérico, sin fila ni bitácora.
  - Vista previa: PDF «borrador», sin fila, sin archivo, sin consumir número y sin bitácora; con acta existente o sin publicar, `409`.
  - Listado: paginado, con `search` y `sort`; `sort` inválido `400`. `pendientes` solo trae las publicadas sin acta.
  - `acta` sin acta: `404` `ERR_ACT_03`; `descarga` devuelve la dirección temporal, anota `DESCARGA_ACTA` y no incluye la ruta; con archivo ausente `404` `ERR_ACT_04` sin anotar.
  - Ninguna postulación, adjudicación ni convocatoria cambia de estado; no hay avisos nuevos en `notificacion`.
  - Publicar sigue funcionando sin acta (no se rompe el CU-22).
  - `403` para un no administrador en todas las rutas; `401` sin sesión; `PUT`, `PATCH` y `DELETE` sobre el acta responden `405`.
- Todo vuelve limpio entre pruebas.

Archivos esperados (crear): la clase de integración.

## Fuera de alcance (no tocar)

- Pruebas unitarias ya cubiertas en las UT anteriores.
- Probar MinIO real.

## Resultado esperado

El flujo del acta funciona completo contra una base real y respeta cada regla del CU.

## Validación

1. `./mvnw test` pasa (con Docker corre; sin Docker, se salta).
2. `./mvnw checkstyle:check` sin errores.
