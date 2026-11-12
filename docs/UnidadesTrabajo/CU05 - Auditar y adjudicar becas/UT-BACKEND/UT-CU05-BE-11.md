# UT-CU05-BE-11 — Pruebas de integración con base real

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-11` |
| **Nombre** | Pruebas de integración con base real |
| **Caso de uso** | CU05-BE |
| **Depende de** | `UT-CU05-BE-08`, `UT-CU05-BE-09`, `UT-CU05-BE-10` |

## Antes de implementar

- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).
- Como referencia: `T/features/postulaciones/CierrePostulacionIntegrationTest.java` (CU-20, Testcontainers con `assumeTrue` de Docker).

## Solicitud de negocio

Confirmar que todo funciona junto contra una PostgreSQL de verdad, incluida la competencia por el último cupo.

## Objetivo

Existe una prueba de integración de punta a punta del CU.

## Alcance

- `T/features/postulaciones/AdjudicacionIntegrationTest.java` con Testcontainers (se salta sin Docker, patrón obligatorio), sobre las migraciones reales y los datos de UT-10:
  - **panel:** cupos, adjudicados, no adjudicados, disponibles, mínimo, cantidad de finalistas y calificadas sin seleccionar; sin cupos o mínimo, `decisionesHabilitadas = false`;
  - **seleccionar finalistas:** con la cantidad del request y con la configurada; quedan creados en el orden de la lista priorizada con su posición en la lista completa; una segunda selección no repite a los anteriores; cantidad cero, negativa o mayor que las disponibles: `400` y no se crea nada; cantidad mayor que los cupos: permitida; convocatoria no cerrada o sin cupos o mínimo: `409` `ERR_ADJ_05`;
  - **adjudicar** con auditoría «Validado», puntaje suficiente y cupos: `200`, estado «adjudicada», una fila en `adjudicacion` con puntaje final, usuario y fecha, una fila de historial (sin motivo), una anotación de bitácora sin datos sensibles, cupos disponibles reducidos en uno y **ningún** aviso en la bandeja del postulante;
  - **no adjudicar** con motivo válido (con auditoría de cada resultado): `200`, estado «no adjudicada», fila en `adjudicacion` `NO_ADJUDICADA`, historial con el motivo, bitácora sin el motivo, cupos sin cambio y sin aviso; sin motivo, solo espacios o de 501 caracteres: `400` y nada cambia;
  - **errores de adjudicar:** sin auditoría y última no «Validado» (`ERR_ADJ_02`), puntaje bajo (`ERR_ADJ_03`), cupos agotados (`ERR_ADJ_04`); en cada caso sigue permitido «No adjudicar» salvo sin auditoría;
  - **doble petición y concurrencia:** una segunda decisión seguida recibe `409` `ERR_ADJ_01` y deja una sola fila nueva; dos peticiones simultáneas para el **mismo** finalista: una `200` y otra `409`, con una sola anotación de bitácora; dos adjudicaciones simultáneas a **distintos** finalistas con un solo cupo libre: una `200` y otra `409` `ERR_ADJ_04`, sin superar los cupos;
  - postulación ya devuelta, archivada, rechazada, adjudicada o no adjudicada: `409` `ERR_ADJ_01`;
  - la postulación decidida no aparece en la lista priorizada, aparece con su estado en la lista de finalistas y ya no se puede auditar (`ERR_EXP_02`); auditorías, puntaje de la IA y documentos siguen igual; el historial conserva todas las filas anteriores;
  - el puntaje final de una adjudicación no cambia si después se reprocesa el puntaje de la IA;
  - `404` convocatoria o finalista inexistente, `401` sin sesión y `403` con otro rol.
- La regla de resultados publicados se verifica en los tests de los `service` (UT-06 y UT-07); aquí solo cuando el CU-22 ya aporte la marca.

Archivos esperados (crear): `AdjudicacionIntegrationTest`.

## Fuera de alcance (no tocar)

- Pruebas de la pantalla.
- Publicar resultados y avisar a los postulantes (CU-22).

## Resultado esperado

El CU queda verificado de punta a punta, incluido que los cupos nunca se superan.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
