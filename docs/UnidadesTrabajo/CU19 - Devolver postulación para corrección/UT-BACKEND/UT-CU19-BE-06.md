# UT-CU19-BE-06 — Lista de finalistas con el estado de la postulación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU19-BE-06` |
| **Nombre** | Lista de finalistas con el estado de la postulación |
| **Caso de uso** | CU19-BE |
| **Depende de** | — |

## Antes de implementar

- Las fichas `UT-CU07-BE-08` y `UT-CU21-BE-05`, y [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md).

## Solicitud de negocio

La pantalla de finalistas debe mostrar quién ya fue devuelto para no intentar auditarlo.

## Objetivo

El listado de finalistas devuelve el estado de la postulación de cada finalista.

## Alcance

- `FinalistaResponse` gana `estadoPostulacion` (`String`: el valor de `postulacion.estado`, por ejemplo `calificada` o `devuelta`).
- `FinalistaConsultaRepository`: el `SELECT` ya une `postulacion`; agrega `po.estado` como `estado_postulacion` (convertido a texto) y lo mapea.
- Se actualizan los tests y fixtures del CU-21 y del CU-07 que construyen `FinalistaResponse` y los de la consulta.
- Sin `filter` ni `sort` nuevos.

Archivos esperados (tocar): `FinalistaResponse`, `FinalistaConsultaRepository` y los tests de finalistas.

## Fuera de alcance (no tocar)

- Filtrar u ordenar por estado.
- Notificar y registrar la entrega (siguen igual).
- La pantalla (UT-CU19-FE-04).

## Resultado esperado

`GET /api/v1/finalistas` muestra el estado de la postulación de cada finalista.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
