"""Segunda opinión con un modelo de aprendizaje automático (complementa, no reemplaza, el puntaje).

El puntaje oficial es el ponderado de `app/scoring.py`: se puede explicar variable por variable y la
DUBSS lo parametriza en el CU26. Este modelo lo acompaña como contraste: dice qué puntaje daría un
modelo aprendido de datos y qué variables pesan más para él.

! El modelo que se entrega está entrenado con datos SINTÉTICOS (`training/`): sirve para demostrar
! el proceso, no para decidir becas. No se usa en el puntaje que guarda el core.
"""

import json
from functools import lru_cache
from pathlib import Path
from typing import Any

import joblib

from app.config import PUNTAJE_MAXIMO
from app.schemas import SolicitudPuntaje
from app.scoring import niveles

DIRECTORIO = Path(__file__).parent / "modelos"
ARCHIVO_MODELO = DIRECTORIO / "ml_v1.joblib"
ARCHIVO_METRICAS = DIRECTORIO / "ml_v1.metricas.json"

NOMBRE_MODELO = "ml_v1"
ADVERTENCIA = "Modelo entrenado con datos sintéticos: sirve de contraste, no para decidir becas."

# Mismo orden en el entrenamiento y en la predicción.
CARACTERISTICAS = (
    "ingresos_familiares",
    "dependientes",
    "procedencia",
    "patrimonio",
    "estado_vulnerabilidad",
    "promedio_academico",
    "dependencia_economica",
    "ocupacion_sosten",
    "discapacidad",
    "hijos",
    "ingreso_por_persona",
)


class ModeloNoDisponible(Exception):
    """No existe el archivo del modelo entrenado (falta correr `training/entrenar.py`)."""


def vector(s: SolicitudPuntaje) -> list[float]:
    """Las características del modelo: los niveles del puntaje más el ingreso por persona."""
    nivel = niveles(s, [])
    # Lo que no se respondió se toma como nivel neutro (0,5), igual que una opción desconocida.
    fila = [0.5 if nivel[c] is None else float(nivel[c]) for c in CARACTERISTICAS[:-1]]
    por_persona = s.ingresos_familiares / (s.dependientes + 1)
    fila.append(min(por_persona, 5000.0) / 5000.0)
    return fila


@lru_cache(maxsize=1)
def _cargar() -> Any:
    if not ARCHIVO_MODELO.exists():
        raise ModeloNoDisponible("El modelo de aprendizaje automático no está entrenado")
    return joblib.load(ARCHIVO_MODELO)


def metricas() -> dict[str, Any]:
    if not ARCHIVO_METRICAS.exists():
        raise ModeloNoDisponible("El modelo de aprendizaje automático no está entrenado")
    return json.loads(ARCHIVO_METRICAS.read_text(encoding="utf-8"))


def predecir(s: SolicitudPuntaje) -> float:
    """El puntaje (0 a `PUNTAJE_MAXIMO`) que da el modelo aprendido."""
    bruto = float(_cargar().predict([vector(s)])[0])
    return round(min(max(bruto * PUNTAJE_MAXIMO / 100.0, 0.0), PUNTAJE_MAXIMO), 2)
