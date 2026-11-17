"""Las ponderaciones por defecto de la IA y el modelo `v1.0` que el core siembra son la misma cosa.

Si el core no tiene la versión `v1.0` registrada, rechaza el puntaje de la IA; y si las
ponderaciones difieren, la IA calcula con unas y la base dice otras. Este test falla en cuanto una
cambia sin la otra.
"""

import re
from pathlib import Path

import pytest

from app.config import PONDERACIONES_DEFECTO, VERSION_MODELO_DEFECTO

MIGRACION = (
    Path(__file__).resolve().parents[2]
    / "comedor-uagrm-core/src/main/resources/db/migration"
    / "V20261121100000__sembrar_modelo_preseleccion_v1_0.sql"
)

pytestmark = pytest.mark.skipif(
    not MIGRACION.exists(), reason="el proyecto comedor-uagrm-core no está junto a este"
)


def ponderaciones_sembradas() -> dict[str, float]:
    sql = MIGRACION.read_text(encoding="utf-8")
    return {clave: float(peso) for clave, peso in re.findall(r"\('(\w+)',\s*([\d.]+)\)", sql)}


def test_la_migracion_siembra_la_version_por_defecto_de_la_ia():
    assert f"'{VERSION_MODELO_DEFECTO}'" in MIGRACION.read_text(encoding="utf-8")


def test_las_ponderaciones_sembradas_son_las_mismas_que_las_de_la_ia():
    assert ponderaciones_sembradas() == PONDERACIONES_DEFECTO


def test_las_ponderaciones_por_defecto_suman_100():
    assert sum(PONDERACIONES_DEFECTO.values()) == 100
