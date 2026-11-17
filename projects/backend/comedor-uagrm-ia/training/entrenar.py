"""Entrena y evalúa el modelo de segunda opinión con datos sintéticos.

    python -m training.entrenar            # genera 20 000 postulantes, entrena y guarda

Guarda `app/modelos/ml_v1.joblib` y `ml_v1.metricas.json`. Es determinista (semilla fija).

! Las métricas miden qué tan bien el modelo reproduce la «necesidad» SINTÉTICA, no qué tan bien
! acierta con postulantes reales. No se citan como evidencia de validez; ver `app/ml.py`.
"""

import json
import sys

import joblib
import numpy as np
from scipy.stats import spearmanr
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_absolute_error, r2_score
from sklearn.model_selection import train_test_split

from app.config import PUNTAJE_MAXIMO
from app.ml import ARCHIVO_METRICAS, ARCHIVO_MODELO, CARACTERISTICAS, vector
from app.schemas import SolicitudPuntaje
from app.scoring import calcular
from training.generar_datos import SEMILLA, generar

CAMPOS = (
    "ingresos_familiares dependientes procedencia patrimonio estado_vulnerabilidad "
    "promedio_academico dependencia_economica ocupacion_sosten"
).split()


def solicitud_de(fila) -> SolicitudPuntaje:
    tiene_hijos = bool(fila["tiene_hijos"])
    return SolicitudPuntaje(
        postulacion="00000000-0000-0000-0000-000000000000",
        ingresos_familiares=float(fila["ingresos_familiares"]),
        dependientes=int(fila["dependientes"]),
        promedio_academico=float(fila["promedio_academico"]),
        procedencia=fila["procedencia"],
        patrimonio=fila["patrimonio"],
        estado_vulnerabilidad=fila["estado_vulnerabilidad"],
        dependencia_economica=fila["dependencia_economica"],
        ocupacion_sosten=fila["ocupacion_sosten"],
        tiene_hijos=tiene_hijos,
        cantidad_hijos=fila["cantidad_hijos"] if tiene_hijos else None,
        discapacidad=bool(fila["discapacidad"]),
    )


def _cobertura_top(referencia: np.ndarray, estimado: np.ndarray, fraccion: float) -> float:
    """Qué parte del `fraccion` más alto de la referencia cae también en el de `estimado`."""
    k = int(len(referencia) * fraccion)
    a = set(np.argsort(-referencia)[:k])
    b = set(np.argsort(-estimado)[:k])
    return len(a & b) / k


def main(n: int = 20000) -> None:
    datos = generar(n)
    solicitudes = [solicitud_de(f) for _, f in datos.iterrows()]
    x = np.array([vector(s) for s in solicitudes])
    y = datos["necesidad"].to_numpy()
    reglas = np.array([calcular(s).puntaje for s in solicitudes])

    x_ent, x_pru, y_ent, y_pru, _, r_pru = train_test_split(
        x, y, reglas, test_size=0.25, random_state=SEMILLA
    )
    modelo = RandomForestRegressor(
        n_estimators=120, max_depth=10, min_samples_leaf=20, n_jobs=-1, random_state=SEMILLA
    )
    modelo.fit(x_ent, y_ent)
    pred = modelo.predict(x_pru)
    ml70 = np.clip(pred * PUNTAJE_MAXIMO / 100.0, 0, PUNTAJE_MAXIMO)

    metricas = {
        "advertencia": "Entrenado con datos sintéticos: demuestra el proceso, no valida el modelo.",
        "postulantes_sinteticos": n,
        "entrenamiento": len(y_ent),
        "prueba": len(y_pru),
        "r2": round(float(r2_score(y_pru, pred)), 4),
        "mae_sobre_100": round(float(mean_absolute_error(y_pru, pred)), 3),
        "spearman_modelo_vs_necesidad": round(float(spearmanr(pred, y_pru).statistic), 4),
        # El puntaje ponderado (oficial) frente a la misma necesidad sintética y frente al modelo.
        "spearman_reglas_vs_necesidad": round(float(spearmanr(r_pru, y_pru).statistic), 4),
        "spearman_reglas_vs_modelo": round(float(spearmanr(r_pru, ml70).statistic), 4),
        "cobertura_top10_modelo": round(_cobertura_top(y_pru, pred, 0.10), 4),
        "cobertura_top10_reglas": round(_cobertura_top(y_pru, r_pru, 0.10), 4),
        "coincidencia_top10_reglas_modelo": round(_cobertura_top(r_pru, ml70, 0.10), 4),
        "importancias": {
            c: round(float(v), 4)
            for c, v in zip(CARACTERISTICAS, modelo.feature_importances_, strict=True)
        },
    }
    ARCHIVO_MODELO.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(modelo, ARCHIVO_MODELO, compress=3)
    ARCHIVO_METRICAS.write_text(
        json.dumps(metricas, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    print(json.dumps(metricas, indent=2, ensure_ascii=False))
    print(f"Guardado: {ARCHIVO_MODELO} ({ARCHIVO_MODELO.stat().st_size / 1024:.0f} KB)")


if __name__ == "__main__":
    main(int(sys.argv[1]) if len(sys.argv) > 1 else 20000)
