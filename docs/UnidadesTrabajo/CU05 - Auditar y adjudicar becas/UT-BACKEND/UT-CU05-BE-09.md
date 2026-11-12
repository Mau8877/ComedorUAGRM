# UT-CU05-BE-09 — Las listas reflejan la decisión y la cantidad queda privada (solo pruebas)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-09` |
| **Nombre** | Las listas reflejan la decisión y la cantidad queda privada (solo pruebas) |
| **Caso de uso** | CU05-BE |
| **Depende de** | — (usa `estadoPostulacion` de `UT-CU19-BE-06` y las pruebas de `UT-CU20-BE-05`, ya implementadas) |

## Antes de implementar

- Las fichas `UT-CU20-BE-05`, `UT-CU19-BE-06`, `UT-CU21-BE-05` y `UT-CU06-BE-*` (lista priorizada), y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Una postulación adjudicada o no adjudicada debe salir de la lista priorizada y verse como tal en la lista de finalistas, para que nadie intente auditarla ni decidirla otra vez; y la cantidad de finalistas no debe filtrarse a quien no es Administrador.

## Objetivo

Queda comprobado, con pruebas, que las listas ya se comportan así y que ninguna consulta de postulantes ni pública entrega la cantidad de finalistas; solo se toca código si una prueba falla.

## Alcance

- `FinalistaConsultaRepository` (CU-21): prueba de que `estadoPostulacion` entrega `adjudicada` y `no_adjudicada` y que el finalista **sigue en la lista**.
- Consulta de la lista priorizada (CU-06): prueba de que una postulación `adjudicada` o `no_adjudicada` no aparece (solo se muestran las «calificadas»).
- `ExpedienteService` (CU-07): prueba de que auditar un finalista «adjudicada» o «no adjudicada» responde `409` `ERR_EXP_02` (ya no está «calificada»).
- **Cantidad privada:** prueba que revisa los DTOs de consulta que hoy toca una convocatoria (`ConvocatoriaPriorizableResponse` y las respuestas de convocatorias que ya existan) y comprueba que **ninguno** trae `cantidadFinalistas` ni `cantidad_finalistas`; `PanelAdjudicacionResponse` es el único que la lleva.
- Si alguna prueba revela que el comportamiento no es el esperado, se corrige en esta UT con el cambio mínimo.
- Sin `filter` ni `sort` nuevos.

Archivos esperados (tocar): los tests de `FinalistaConsultaRepository`, de la lista priorizada y de `ExpedienteService`; un test nuevo para la cantidad privada.

## Fuera de alcance (no tocar)

- Filtrar u ordenar por estado.
- Quitar al finalista decidido de la lista de finalistas.
- La pantalla (UT-CU05-FE-07).
- Crear el endpoint de consulta de convocatorias del postulante (CU-13 o CU-02): cuando exista, su propio CU repite esta comprobación.

## Resultado esperado

Las listas y el expediente tienen pruebas que fijan el comportamiento del CU-05 y la cantidad de finalistas queda solo para el Administrador.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
