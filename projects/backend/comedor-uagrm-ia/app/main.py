"""Módulo de IA de ComedorUAGRM (FastAPI).

Calcula el puntaje de vulnerabilidad que `comedor-uagrm-core` usa para la preselección de la Beca
Comedor (CU03) y para reprocesar con otra versión del modelo (CU26). No guarda estado: aplica las
ponderaciones que recibe en cada solicitud.
"""

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app import ml
from app.schemas import PuntajeCalculado, SegundaOpinion, SolicitudPuntaje
from app.scoring import ParametrosInvalidos, calcular

app = FastAPI(
    title="ComedorUAGRM IA",
    description="Módulo de IA de ComedorUAGRM: puntaje de vulnerabilidad para la preselección",
    version="0.1.0",
)


@app.exception_handler(ParametrosInvalidos)
def parametros_invalidos(_: Request, error: ParametrosInvalidos) -> JSONResponse:
    # 422: el core lo trata como «la IA rechazó los datos» (no como «no disponible»).
    return JSONResponse(status_code=422, content={"detail": str(error)})


@app.exception_handler(ml.ModeloNoDisponible)
def modelo_no_disponible(_: Request, error: ml.ModeloNoDisponible) -> JSONResponse:
    return JSONResponse(status_code=503, content={"detail": str(error)})


@app.get("/health", tags=["infraestructura"])
def health() -> dict[str, str]:
    return {"status": "UP"}


@app.post("/api/v1/puntaje", tags=["puntaje"])
def puntaje(solicitud: SolicitudPuntaje) -> PuntajeCalculado:
    return calcular(solicitud)


@app.post("/api/v1/segunda-opinion", tags=["puntaje"])
def segunda_opinion(solicitud: SolicitudPuntaje) -> SegundaOpinion:
    """Contrasta el puntaje oficial con el de un modelo aprendido; el core no lo usa."""
    ponderado = calcular(solicitud).puntaje
    aprendido = ml.predecir(solicitud)
    return SegundaOpinion(
        puntaje_ponderado=ponderado,
        puntaje_aprendido=aprendido,
        diferencia=round(aprendido - ponderado, 2),
        modelo_aprendido=ml.NOMBRE_MODELO,
        advertencia=ml.ADVERTENCIA,
    )


@app.get("/api/v1/segunda-opinion/metricas", tags=["puntaje"])
def metricas_segunda_opinion() -> dict[str, object]:
    return ml.metricas()
