# UT-CU07-BE-02 — Puntaje de la IA de 0 a 70

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-BE-02` |
| **Nombre** | Puntaje de la IA de 0 a 70 |
| **Caso de uso** | CU07-BE |
| **Depende de** | — |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) y el cargador `B/config/DatosPruebaListaPriorizada.java` (CU06).

## Solicitud de negocio

La IA califica solo hasta 70 puntos; los otros 30 los pone el auditor a mano en este CU.

## Objetivo

`puntaje_postulacion.puntaje_total` acepta solo de 0 a 70 y los datos de prueba y los tests de la lista priorizada lo respetan.

## Alcance

- Migración `V20261107110100__limitar_puntaje_de_ia_a_70.sql`: reescala los puntajes ya guardados (multiplica por 0,7 y redondea a 2 decimales; aún no hay datos reales, solo de prueba) y cambia `ck_puntaje_postulacion_total` a `BETWEEN 0 AND 70`.
- `DatosPruebaListaPriorizada` genera puntajes entre 0 y 70 (misma variedad: repetidos y más de un puntaje por postulación).
- Se ajustan los tests de CU-06 y CU-21 que dependan de valores concretos o de 100 como máximo (lista priorizada, PDF, finalistas).
- Se actualiza `docs/DiagramaConceptual.mermaid` (comentario del `CHECK`) y se deja avisado a quien hace el CU-03 que su modelo debe devolver 0–70.

Archivos esperados (crear): la migración. (Tocar): `DatosPruebaListaPriorizada.java` y los tests afectados.

## Fuera de alcance (no tocar)

- El cálculo del puntaje (CU-03, otro integrante).
- Guardar o mostrar el puntaje académico (UT-03 en adelante).
- El orden de la lista priorizada (sigue por el puntaje de la IA).

## Resultado esperado

Con la migración aplicada, ningún puntaje de la IA supera 70 y la lista priorizada se ve igual de ordenada.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Con `dev`: `select max(puntaje_total) from puntaje_postulacion` ≤ 70.
