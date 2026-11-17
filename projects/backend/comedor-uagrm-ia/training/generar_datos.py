"""Genera postulantes SINTÉTICOS para entrenar el modelo de segunda opinión.

! No hay datos reales de la DUBSS: lo que se genera acá es inventado. Un modelo entrenado con esto
! no demuestra que acierte con postulantes de verdad; solo demuestra el proceso (datos,
! entrenamiento, métricas, reentrenamiento). Cuando la DUBSS entregue postulaciones históricas, se
! reemplaza este generador por la lectura de esos datos y se vuelve a correr `entrenar.py`.

* Para que no sea circular (aprender exactamente la regla que ya tiene `scoring.py`), la
* «necesidad real» se arma con otra lógica: parte de una dificultad latente del hogar, mira el
* ingreso por persona (no solo el ingreso total) y suma interacciones y ruido.
"""

import numpy as np
import pandas as pd

from app.scoring import (
    _DEPENDENCIA_ECONOMICA,
    _ESTADO_VULNERABILIDAD,
    _OCUPACION_SOSTEN,
    _PATRIMONIO,
    _PROCEDENCIA,
)

SEMILLA = 2026


def _elegir(
    rng: np.random.Generator, opciones: list[str], dificultad: np.ndarray, sesgo: float
) -> np.ndarray:
    """Elige una opción por persona; a más dificultad, más probabilidad de las últimas opciones."""
    n = len(opciones)
    posicion = np.linspace(0, 1, n)
    distancia = np.abs(1 / (1 + np.exp(-dificultad))[:, None] - posicion[None, :])
    prob = np.exp(-sesgo * distancia * n)
    prob /= prob.sum(axis=1, keepdims=True)
    acumulada = prob.cumsum(axis=1)
    indice = (rng.random(len(dificultad))[:, None] > acumulada).sum(axis=1)
    return np.array(opciones)[np.minimum(indice, n - 1)]


def generar(n: int, semilla: int = SEMILLA) -> pd.DataFrame:
    rng = np.random.default_rng(semilla)
    d = rng.normal(size=n)  # dificultad latente del hogar (no se observa)

    ingresos = np.clip(np.exp(8.3 - 0.45 * d + rng.normal(0, 0.45, n)), 300, 20000).round(0)
    dependientes = np.clip(rng.poisson(np.exp(0.9 + 0.25 * d)), 0, 12)

    # Opciones ordenadas de menos a más vulnerable según el nivel de `scoring.py`.
    ordenadas = {
        "procedencia": sorted(_PROCEDENCIA, key=_PROCEDENCIA.get),
        "patrimonio": sorted(_PATRIMONIO, key=_PATRIMONIO.get),
        "estado_vulnerabilidad": sorted(_ESTADO_VULNERABILIDAD, key=_ESTADO_VULNERABILIDAD.get),
        "dependencia_economica": sorted(_DEPENDENCIA_ECONOMICA, key=_DEPENDENCIA_ECONOMICA.get),
        "ocupacion_sosten": sorted(_OCUPACION_SOSTEN, key=_OCUPACION_SOSTEN.get),
    }
    df = pd.DataFrame({"ingresos_familiares": ingresos, "dependientes": dependientes})
    for columna, opciones in ordenadas.items():
        df[columna] = _elegir(rng, opciones, d, sesgo=2.0)
    df["promedio_academico"] = np.clip(rng.normal(72 - 3 * d, 9, n), 51, 100).round(1)
    df["discapacidad"] = rng.random(n) < 0.04 + 0.02 * (d > 1)
    df["tiene_hijos"] = rng.random(n) < 0.06 + 0.05 * (d > 0.5)
    df["cantidad_hijos"] = np.where(
        df["tiene_hijos"], np.where(rng.random(n) < 0.3, "Más de 1", "1"), None
    )

    # Necesidad «real» (0-100), con otra lógica que la regla ponderada.
    por_persona = ingresos / (dependientes + 1)
    carencia_ingreso = 1 / (1 + np.exp((por_persona - 1200) / 450))
    carga = np.clip(dependientes / 7, 0, 1)
    z = (
        2.4 * carencia_ingreso
        + 1.1 * carga * carencia_ingreso  # interacción: muchas bocas con poco ingreso
        + 0.9 * df["patrimonio"].map(_PATRIMONIO)
        + 0.7 * df["procedencia"].map(_PROCEDENCIA) * (ingresos < 4000)
        + 0.8 * df["dependencia_economica"].map(_DEPENDENCIA_ECONOMICA)
        + 0.5 * df["ocupacion_sosten"].map(_OCUPACION_SOSTEN)
        + 0.4 * df["estado_vulnerabilidad"].map(_ESTADO_VULNERABILIDAD)
        + 1.2 * df["discapacidad"]
        + 0.5 * df["tiene_hijos"]
        + rng.normal(0, 0.35, n)  # lo que ningún formulario captura
    )
    df["necesidad"] = (100 * (z - z.min()) / (z.max() - z.min())).round(3)
    return df
