# UT-CU03-BE-08 — Módulo de IA: segunda opinión con aprendizaje automático

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU03-BE-08` |
| **Nombre** | Módulo de IA: segunda opinión con aprendizaje automático |
| **Caso de uso** | CU03-BACKEND |
| **Depende de** | UT-CU03-BE-02 |

## Antes de implementar

[`PUNTAJE_IA.md`](../../../../.claude/rules/backend-ia/PUNTAJE_IA.md), sección «Segunda opinión».

## Solicitud de negocio

La documentación de grado habla de un modelo de inteligencia artificial y hoy el puntaje es ponderado por reglas. Un modelo aprendido, aunque sea de contraste, muestra el proceso de datos → entrenamiento → evaluación y deja listo el reentrenamiento para cuando la DUBSS entregue postulaciones históricas.

## Objetivo

Un Random Forest entrenado con datos sintéticos, expuesto como contraste del puntaje oficial sin tocar el contrato con el core.

## Alcance

- `training/generar_datos.py`: postulantes sintéticos deterministas (semilla fija); la «necesidad real» usa otra lógica que la regla (ingreso por persona, interacciones, ruido).
- `training/entrenar.py`: entrena, evalúa (R², MAE, correlación de orden, cobertura del 10 % más necesitado, importancias) y guarda `app/modelos/ml_v1.joblib` y `ml_v1.metricas.json`.
- `app/ml.py`: carga el modelo y predice de 0 a 70; sin el archivo lanza `ModeloNoDisponible` (`503`).
- `POST /api/v1/segunda-opinion` (puntaje ponderado, aprendido, diferencia, advertencia) y `GET /api/v1/segunda-opinion/metricas`.
- `tests/test_segunda_opinion.py` (11 casos). Dependencias: `scikit-learn`, `joblib`; `pandas` y `scipy` solo para entrenar.

Archivos esperados (crear/tocar): `training/*`, `app/ml.py`, `app/modelos/*`, `app/main.py`, `app/schemas.py`, `tests/test_segunda_opinion.py`, `requirements*.txt`, `README.md`.

## Fuera de alcance (no tocar)

- Que el core llame a este endpoint, guarde su resultado o lo use para ordenar o decidir: el puntaje oficial es el ponderado.
- Mostrarlo en el expediente (CU07) o en la lista (CU06).
- Entrenar con datos reales: no los hay.

## Resultado esperado

`POST /api/v1/segunda-opinion` devuelve ambos puntajes con la advertencia de datos sintéticos; `/api/v1/puntaje` queda idéntico.

## Validación

1. `pytest` en `comedor-uagrm-ia`: 60 casos en verde y `ruff check . ; ruff format --check .` limpios.
2. `python -m training.entrenar` es reproducible: mismas métricas en cada corrida.
3. Un hogar necesitado recibe al menos 20 puntos más aprendidos que uno acomodado; sin el archivo del modelo la ruta da `503`.
