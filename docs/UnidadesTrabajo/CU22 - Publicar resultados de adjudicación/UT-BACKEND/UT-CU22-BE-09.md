# UT-CU22-BE-09 — Conectar la marca de publicación y habilitar comunicados a `ADJUDICADOS`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-BE-09` |
| **Nombre** | Conectar la marca de publicación y habilitar comunicados a `ADJUDICADOS` |
| **Caso de uso** | CU22-BE |
| **Depende de** | `UT-CU22-BE-02` |

## Antes de implementar

- Las fichas `UT-CU05-BE-04/05`, `UT-CU19-BE-*`, `UT-CU20-BE-03` y `UT-CU21-BE-08`, y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Tras publicar, ya no se registran ni modifican decisiones; y la DUBSS puede escribirle un comunicado solo a los adjudicados.

## Objetivo

Los CU-05, 19 y 20 leen la marca real de publicación, y el CU-21 acepta el destinatario `ADJUDICADOS`.

## Alcance

- Los métodos `resultadosPublicados` que hoy devuelven siempre `false` con un `TODO` hacia el CU-22 (consulta del CU-05, del CU-19 y del CU-20) pasan a leer `publicacion_resultado` por `convocatoria_id`. Cambio mínimo en cada consulta; el panel del CU-05 refleja `resultadosPublicados` y `decisionesHabilitadas = false` tras publicar.
- Pruebas nuevas (con el `service` simulado y con Testcontainers) de que, con la fila presente, adjudicar, no adjudicar, devolver, archivar y rechazar responden `409` (el código propio de cada CU: `ERR_ADJ_01`, el del CU-19 o `ERR_ARC_01`) sin cambiar nada.
- **CU-21:** `ComunicadoService` deja de rechazar `ADJUDICADOS` (`ERR_COM_01`) y resuelve los destinatarios como los finalistas con adjudicación `ADJUDICADA` de la convocatoria (que pasa a ser obligatoria, como con `FINALISTAS`: `ERR_COM_02` si falta). Prueba de que el comunicado llega solo a los adjudicados y de que sin convocatoria publicada igual funciona (los adjudicados existen desde que se decide).
- Si alguna prueba revela otro comportamiento, se corrige en esta UT con el cambio mínimo.

Archivos esperados (tocar): las consultas de CU-05, 19 y 20, `ComunicadoService` y los tests de cada una.

## Fuera de alcance (no tocar)

- Cambiar reglas de negocio de CU-05, 19, 20 o 21 más allá de lo descrito.
- Crear marcas nuevas o columnas.
- Pantallas.

## Resultado esperado

Después de publicar nadie puede decidir sobre esa convocatoria, y los comunicados a `ADJUDICADOS` funcionan.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
