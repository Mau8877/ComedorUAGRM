"""Segunda opinión con el modelo aprendido (datos sintéticos): no cambia el puntaje oficial."""

import pytest
from fastapi.testclient import TestClient

from app import ml
from app.main import app
from training.entrenar import solicitud_de
from training.generar_datos import generar

cliente = TestClient(app)

POBRE = {
    "postulacion": "11111111-1111-1111-1111-111111111111",
    "ingresosFamiliares": 900,
    "dependientes": 5,
    "procedencia": "Área rural",
    "patrimonio": "Sin vivienda propia",
    "estadoVulnerabilidad": "Ingresos irregulares",
    "promedioAcademico": 70,
    "dependenciaEconomica": "Independiente",
    "ocupacionSosten": "Asalariado informal",
    "tieneHijos": True,
    "cantidadHijos": "Más de 1",
    "discapacidad": False,
}
ACOMODADO = {
    **POBRE,
    "ingresosFamiliares": 12000,
    "dependientes": 1,
    "procedencia": "Capital",
    "patrimonio": "Vivienda propia",
    "estadoVulnerabilidad": "Sin situación especial",
    "dependenciaEconomica": "Ambos padres",
    "ocupacionSosten": "Rentista",
    "tieneHijos": False,
    "cantidadHijos": None,
}


def test_generador_es_determinista_y_trae_la_necesidad_entre_0_y_100():
    a, b = generar(500), generar(500)
    assert a.equals(b)
    assert a["necesidad"].between(0, 100).all()
    assert len(a) == 500


def test_generador_hace_que_la_dificultad_se_note_en_los_datos():
    df = generar(5000)
    assert df["ingresos_familiares"].corr(df["necesidad"]) < -0.3


def test_las_solicitudes_sinteticas_son_validas_para_el_contrato():
    for _, fila in generar(50).iterrows():
        solicitud_de(fila)


def test_el_necesitado_recibe_mas_puntaje_aprendido_que_el_acomodado():
    pobre = cliente.post("/api/v1/segunda-opinion", json=POBRE).json()
    rico = cliente.post("/api/v1/segunda-opinion", json=ACOMODADO).json()
    assert pobre["puntajeAprendido"] > rico["puntajeAprendido"] + 20


@pytest.mark.parametrize("cuerpo", [POBRE, ACOMODADO])
def test_el_puntaje_aprendido_queda_entre_0_y_70_y_trae_la_advertencia(cuerpo):
    r = cliente.post("/api/v1/segunda-opinion", json=cuerpo)
    assert r.status_code == 200
    datos = r.json()
    assert 0 <= datos["puntajeAprendido"] <= 70
    assert datos["modeloAprendido"] == "ml_v1"
    assert "sintéticos" in datos["advertencia"]
    assert datos["diferencia"] == pytest.approx(
        datos["puntajeAprendido"] - datos["puntajePonderado"], abs=0.01
    )


def test_el_puntaje_ponderado_de_la_segunda_opinion_es_el_oficial():
    oficial = cliente.post("/api/v1/puntaje", json=POBRE).json()["puntaje"]
    segunda = cliente.post("/api/v1/segunda-opinion", json=POBRE).json()
    assert segunda["puntajePonderado"] == oficial


def test_el_orden_del_modelo_coincide_con_el_de_las_reglas():
    # Segunda opinión, no oráculo: deben coincidir en lo grueso (los 5 hogares sintéticos más
    # necesitados por reglas quedan por encima de los 5 menos necesitados también para el modelo).
    filas = [solicitud_de(f) for _, f in generar(300).iterrows()]
    from app.scoring import calcular

    por_reglas = sorted(filas, key=lambda s: calcular(s).puntaje)
    bajos, altos = por_reglas[:5], por_reglas[-5:]
    assert min(ml.predecir(s) for s in altos) > max(ml.predecir(s) for s in bajos)


def test_las_metricas_vienen_con_la_advertencia_de_datos_sinteticos():
    r = cliente.get("/api/v1/segunda-opinion/metricas")
    assert r.status_code == 200
    m = r.json()
    assert "sintéticos" in m["advertencia"]
    assert 0 <= m["r2"] <= 1
    assert set(m["importancias"]) == set(ml.CARACTERISTICAS)


def test_sin_el_archivo_del_modelo_responde_503(monkeypatch, tmp_path):
    monkeypatch.setattr(ml, "ARCHIVO_MODELO", tmp_path / "no_existe.joblib")
    ml._cargar.cache_clear()
    try:
        r = cliente.post("/api/v1/segunda-opinion", json=POBRE)
        assert r.status_code == 503
    finally:
        ml._cargar.cache_clear()


def test_pedir_la_segunda_opinion_no_cambia_el_contrato_del_puntaje():
    r = cliente.post("/api/v1/puntaje", json=POBRE)
    assert set(r.json()) == {"puntaje", "modelo", "detalle"}
