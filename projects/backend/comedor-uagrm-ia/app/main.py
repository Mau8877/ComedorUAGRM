"""Módulo de IA de ComedorUAGRM (FastAPI).

Hoy solo expone /health: la lógica de negocio (el cálculo del puntaje de
vulnerabilidad que consume el backend `comedor-uagrm-core`) se agrega junto
con el primer caso de uso que la necesite.
"""

from fastapi import FastAPI

app = FastAPI(
    title="ComedorUAGRM IA",
    description="Módulo de IA de ComedorUAGRM",
    version="0.0.1",
)


@app.get("/health", tags=["infraestructura"])
def health() -> dict[str, str]:
    return {"status": "UP"}
