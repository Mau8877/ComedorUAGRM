# UT-CU05-BE-10 — Datos de prueba de adjudicación en el cargador de desarrollo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-10` |
| **Nombre** | Datos de prueba de adjudicación en el cargador de desarrollo |
| **Caso de uso** | CU05-BE |
| **Depende de** | `UT-CU05-BE-01`, `UT-CU05-BE-02` |

## Antes de implementar

- `B/config/DatosPruebaExpedientes.java` y su test (CU-07, ampliado por el CU-19 y el CU-20) y la regla de que los datos de prueba corren solo con el perfil `dev`.

## Solicitud de negocio

El CU-02, el CU-03 y el CU-13 los implementan otros integrantes y pueden no existir aún: hace falta tener a la mano una convocatoria cerrada con cupos y mínimo, postulaciones calificadas sin seleccionar y finalistas en cada situación, para probar la pantalla y los conflictos.

## Objetivo

Con el perfil `dev` hay datos para probar la selección y cada camino de la decisión, incluidos los casos límite.

## Alcance

- Se amplía el cargador de desarrollo (solo `dev`, nunca producción, idempotente), sobre la convocatoria `[PRUEBA]`:
  - la convocatoria queda `CERRADA`, con **cupos**, **puntaje mínimo** y **cantidad de finalistas** definidos (valores de ejemplo, claramente de prueba; **no** son los de la DUBSS, que están por definir; la cantidad se escribe con SQL propio del cargador porque el CU-13 aún no la edita);
  - al menos varias postulaciones «calificadas» con puntaje vigente **sin** ser finalistas, para probar la selección;
  - finalistas «calificada» en cada situación: con última auditoría «Validado» y puntaje suficiente, «Validado» con puntaje total **por debajo** del mínimo, «Observado», «No válido» y sin ninguna auditoría; con la entrega física registrada y sin registrar (los ya cargados por el CU-07, CU-19 y CU-20 se reutilizan, no se duplican);
  - una postulación «adjudicada» con su fila en `adjudicacion` y su historial, y otra «no adjudicada» por decisión final (con fila en `adjudicacion`), distinta de la «no adjudicada» por rechazo del CU-20 (sin fila);
- Dos convocatorias de casos límite, solo en `dev`: una `CERRADA` con los cupos **casi agotados** (queda uno libre, para probar la competencia) y otra `ABIERTA` o sin cupos o mínimo definidos (para `ERR_ADJ_05`). La convocatoria con **resultados ya publicados** no se carga: la marca la define el CU-22 (se agrega entonces).
- Test del cargador: carga una sola vez y deja esos casos, sin tocar los datos de otros CU más allá de lo indicado.

Archivos esperados (tocar): `DatosPruebaExpedientes` y su test (o un cargador hermano `DatosPruebaAdjudicacion` si el original queda muy largo).

## Fuera de alcance (no tocar)

- Datos en producción.
- Marcar resultados como publicados (CU-22).
- Cambiar los finalistas ya cargados por otros CU más allá de lo indicado.
- Inventar los valores reales de cupos o puntaje mínimo de la DUBSS.

## Resultado esperado

Se puede probar todo el CU a mano contra datos de ejemplo.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Con el perfil `dev`, el panel de la convocatoria `[PRUEBA]` muestra cupos, mínimo, cantidad de finalistas y calificadas sin seleccionar.
