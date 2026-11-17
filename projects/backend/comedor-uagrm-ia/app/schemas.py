"""Contrato HTTP con `comedor-uagrm-core` (`SolicitudPuntaje` y `PuntajeCalculado` del lado Java).

Los nombres viajan en camelCase, como los arma el core. La solicitud lleva el código público de la
postulación y las variables del modelo, nunca nombres, CI ni datos de contacto.
"""

from typing import Any
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel


class SolicitudPuntaje(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    postulacion: UUID
    ingresos_familiares: float = Field(ge=0)
    dependientes: int = Field(ge=0, le=50)
    procedencia: str
    # La carrera viaja pero no tiene ponderación: no entra al puntaje.
    carrera: str | None = None
    patrimonio: str
    estado_vulnerabilidad: str
    promedio_academico: float = Field(ge=0, le=100)
    # Del formulario oficial de la DUBSS. Son opcionales: una postulación enviada antes de que el
    # formulario los pidiera no los tiene, y entonces esas variables no entran al puntaje.
    dependencia_economica: str | None = None
    ocupacion_sosten: str | None = None
    tiene_hijos: bool | None = None
    cantidad_hijos: str | None = None
    discapacidad: bool | None = None
    # Versión del modelo y ponderaciones de cada variable (CU26). Son nulos solo si el core todavía
    # no guardó ninguna versión: entonces se usa el modelo por defecto.
    modelo: str | None = None
    parametros: dict[str, float] | None = None


class PuntajeCalculado(BaseModel):
    puntaje: float
    modelo: str
    detalle: dict[str, Any]


class SegundaOpinion(BaseModel):
    """El puntaje ponderado (oficial) frente al de un modelo aprendido, para contrastar."""

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    puntaje_ponderado: float
    puntaje_aprendido: float
    diferencia: float
    modelo_aprendido: str
    advertencia: str
