# UT-CU20-BE-05 — Las listas reflejan el cierre (solo pruebas)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU20-BE-05` |
| **Nombre** | Las listas reflejan el cierre (solo pruebas) |
| **Caso de uso** | CU20-BE |
| **Depende de** | — (usa `estadoPostulacion` de `UT-CU19-BE-06`, ya implementada) |

## Antes de implementar

- Las fichas `UT-CU19-BE-06`, `UT-CU21-BE-05` y `UT-CU06-BE-*` (lista priorizada), y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Una postulación archivada o no adjudicada debe salir de la lista priorizada y verse como tal en la lista de finalistas, para que nadie intente auditarla.

## Objetivo

Queda comprobado, con pruebas, que ambas listas ya se comportan así; solo se toca código si una prueba falla.

## Alcance

- `FinalistaConsultaRepository` (CU-21): prueba nueva de que `estadoPostulacion` entrega `archivada` y `no_adjudicada` y que el finalista **sigue en la lista** *(por revisar)*.
- Consulta de la lista priorizada (CU-06): prueba de que una postulación `archivada` o `no_adjudicada` no aparece (solo se muestran las «calificadas»).
- Si alguna prueba revela que el comportamiento no es el esperado, se corrige en esta UT con el cambio mínimo.
- Sin `filter` ni `sort` nuevos.

Archivos esperados (tocar): los tests de `FinalistaConsultaRepository` y de la lista priorizada.

## Fuera de alcance (no tocar)

- Filtrar u ordenar por estado.
- Quitar al finalista cerrado de la lista de finalistas.
- La pantalla (UT-CU20-FE-04).

## Resultado esperado

Las dos listas tienen pruebas que fijan el comportamiento del CU-20.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
