# UT-CU03-BE-03 — Core: contrato con la IA y cliente HTTP

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU03-BE-03` |
| **Nombre** | Core: contrato con la IA y cliente HTTP |
| **Caso de uso** | CU03-BACKEND |
| **Depende de** | UT-CU03-BE-01 |

## Antes de implementar

[`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md) y la ficha de implementación del [CU02](../../../Implementacion/CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md) (contrato).

## Solicitud de negocio

El core debe poder pedirle el puntaje a la IA y distinguir si no está disponible o si rechazó los datos, para decidir si reintenta.

## Objetivo

`PrecalificacionClient` con su implementación HTTP, que convierte lo que responde la IA en un resultado o en una de dos excepciones.

## Alcance

- `SolicitudPuntaje` (con `modelo`, `parametros` y los cinco datos del formulario oficial) y `PuntajeCalculado` (`puntaje`, `modelo`, `detalle`) en `features/preseleccion/dto/`.
- `IaHttpPrecalificacionClient.calcular()`: `POST {app.ia.base-url}/api/v1/puntaje` con 2 s de conexión y `app.ia.timeout-segundos` (5) de lectura. `404`, red caída, `5xx` y tiempo agotado → `IaNoDisponibleException`; otro `4xx` o una respuesta sin puntaje, sin versión o fuera de 0 a 70 → `IaDatosInvalidosException`.
- `VariablesModelo`: las diez claves y nombres de las variables (comparten con la IA y con la migración de `v1.0`).
- `IaHttpPrecalificacionClientTest` contra un servidor HTTP falso de la JDK: respuesta válida, camelCase sin datos personales, `404`, `500`, tiempo agotado, nadie escuchando, `422`, fuera de rango y tope exacto de 70.

Archivos esperados (crear/tocar): `SolicitudPuntaje.java`, `PuntajeCalculado.java`, `PrecalificacionClient.java`, `IaHttpPrecalificacionClient.java`, `IaNoDisponibleException.java`, `IaDatosInvalidosException.java`, `VariablesModelo.java`, `IaHttpPrecalificacionClientTest.java`.

## Fuera de alcance (no tocar)

- Qué hace el servicio con cada excepción (UT-CU03-BE-05).
- Reintentos automáticos (UT-CU03-BE-06): el cliente no reintenta.
- Registrar en bitácora: no se audita (es una acción del sistema, no de un usuario).

## Resultado esperado

El core entiende cualquier respuesta de la IA: un puntaje válido llega como `PuntajeCalculado` y cada falla llega como la excepción que corresponde.

## Validación

1. `cd projects/backend/comedor-uagrm-core && ./mvnw -o test -Dtest=IaHttpPrecalificacionClientTest` en verde (9 casos).
2. Con la IA levantada, `calcular()` con la versión `null` devuelve `v1.0`; con una variable desconocida en `parametros` lanza `IaDatosInvalidosException`.
