# UT-CU03-BE-02 — Módulo de IA: cálculo ponderado y explicable del puntaje

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU03-BE-02` |
| **Nombre** | Módulo de IA: cálculo ponderado y explicable del puntaje |
| **Caso de uso** | CU03-BACKEND |
| **Depende de** | UT-CU03-BE-01 |

## Antes de implementar

[`PUNTAJE_IA.md`](../../../../.claude/rules/backend-ia/PUNTAJE_IA.md) (criterio de puntaje) y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Ordenar a los postulantes por su necesidad de forma objetiva, explicable y con las ponderaciones que defina la DUBSS.

## Objetivo

`calcular()` devuelve un puntaje de 0 a 70 con lo que aportó cada variable, aplicando las ponderaciones recibidas.

## Alcance

- `app/scoring.py`: el nivel (0 a 1) de cada una de las diez variables (`niveles()`), la validación de ponderaciones (`_ponderaciones()`) y `calcular()`, que escala linealmente de 100 a 70 y devuelve `detalle` (variables, puntaje sobre 100, máximo, advertencias).
- Variables sin dato (postulaciones anteriores al formulario ampliado) no entran: el 100 % se reparte entre las que sí tienen; una opción desconocida usa un nivel neutro y se avisa.
- `app/config.py`: versión por defecto `v1.0`, tope de 70 (`PUNTAJE_MAXIMO`) y ponderaciones por defecto (suman 100; el promedio académico pesa 0).
- `tests/test_puntaje.py`: `TestRango`, `TestMonotonia`, `TestParametros`, `TestFormularioOficial`, `TestRendimiento`.

Archivos esperados (crear/tocar): `app/scoring.py`, `app/config.py`, `tests/test_puntaje.py`.

## Fuera de alcance (no tocar)

- Guardar ponderaciones o versiones: la IA no guarda estado (eso es del CU26).
- Entrenar un modelo con datos (tarea B2, no hecha).
- Leer los documentos adjuntos o aplicar OCR.

## Resultado esperado

Más necesidad nunca da menos puntaje; el resultado queda entre 0 y 70 con dos decimales; unas ponderaciones que no suman 100 se normalizan; una versión antigua de seis ponderaciones sigue funcionando.

## Validación

1. `pytest tests/test_puntaje.py` en verde (45 casos, incluidos los de la UT-01).
2. Con 200 solicitudes seguidas, cada cálculo tarda mucho menos de 0,1 s (`TestRendimiento`).
