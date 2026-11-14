# UT-CU07-BE-08 — Lista de finalistas con el resultado de la auditoría

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-BE-08` |
| **Nombre** | Lista de finalistas con el resultado de la auditoría |
| **Caso de uso** | CU07-BE |
| **Depende de** | `UT-CU07-BE-03` |

## Antes de implementar

- Las fichas del CU-21 (`UT-CU21-BE-05` y `UT-CU21-BE-10`) y [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md).

## Solicitud de negocio

La pantalla de finalistas debe mostrar quién ya fue auditado y con qué resultado, y llevar al expediente.

## Objetivo

El listado de finalistas del CU-21 devuelve el resultado de la última auditoría y el puntaje total.

## Alcance

- `FinalistaResponse` gana `resultadoAuditoria` (`SIN_AUDITAR`, `VALIDADO`, `OBSERVADO` o `NO_VALIDO`) y `puntajeTotal` (`BigDecimal`, `null` si no se puede calcular, como en UT-06). `puntaje` sigue siendo el de la IA.
- `FinalistaConsultaRepository`: la consulta suma un `LEFT JOIN LATERAL` a la última auditoría del finalista (la misma lógica de «más reciente» de UT-03).
- Se actualizan los tests y fixtures del CU-21 que construyen `FinalistaResponse` y los de la consulta.
- Sin `filter` ni `sort` nuevos.

Archivos esperados (tocar): `FinalistaResponse`, `FinalistaConsultaRepository`, `FinalistaService` si hace falta, y los tests de finalistas.

## Fuera de alcance (no tocar)

- Notificar y registrar la entrega (siguen igual).
- Filtrar u ordenar por resultado de auditoría.
- La pantalla (UT-FE-02).

## Resultado esperado

`GET /api/v1/finalistas` muestra el resultado de la auditoría y el puntaje total de cada finalista.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
