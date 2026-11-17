import time
from copy import deepcopy

import pytest
from fastapi.testclient import TestClient

from app.config import PONDERACIONES_DEFECTO, PUNTAJE_MAXIMO, VERSION_MODELO_DEFECTO
from app.main import app

client = TestClient(app)
RUTA = "/api/v1/puntaje"

# Lo que arma `SolicitudPuntaje` del core (camelCase), con los textos de `OpcionesSocioeconomicas`.
SOLICITUD = {
    "postulacion": "0198f5a0-7b1e-7c3a-9d4e-2a6f1b8c3d90",
    "ingresosFamiliares": 1800.0,
    "dependientes": 4,
    "procedencia": "Provincia",
    "carrera": "Ing. Informática",
    "patrimonio": "Sin vivienda propia",
    "estadoVulnerabilidad": "Hogar monoparental",
    "promedioAcademico": 72.5,
    "modelo": None,
    "parametros": None,
}

# Lo que trae una postulación enviada con el formulario oficial de la DUBSS.
FORMULARIO_OFICIAL = {
    "dependenciaEconomica": "Solo de padre o madre",
    "ocupacionSosten": "Comerciante minorista",
    "tieneHijos": False,
    "cantidadHijos": None,
    "discapacidad": False,
}

MAS_VULNERABLE = {
    **SOLICITUD,
    "ingresosFamiliares": 0,
    "dependientes": 10,
    "procedencia": "Área rural",
    "patrimonio": "Sin vivienda propia",
    "estadoVulnerabilidad": "Persona con discapacidad en el hogar",
    "promedioAcademico": 100,
}
MENOS_VULNERABLE = {
    **SOLICITUD,
    "ingresosFamiliares": 20000,
    "dependientes": 0,
    "procedencia": "Capital",
    "patrimonio": "Vivienda propia",
    "estadoVulnerabilidad": "Sin situación especial",
    "promedioAcademico": 0,
}


def pedir(**cambios):
    cuerpo = deepcopy(SOLICITUD)
    cuerpo.update(cambios)
    return client.post(RUTA, json=cuerpo)


def puntaje_de(**cambios) -> float:
    respuesta = pedir(**cambios)
    assert respuesta.status_code == 200, respuesta.text
    return respuesta.json()["puntaje"]


class TestContrato:
    def test_responde_puntaje_modelo_y_detalle(self):
        respuesta = pedir()

        assert respuesta.status_code == 200
        cuerpo = respuesta.json()
        assert set(cuerpo) == {"puntaje", "modelo", "detalle"}
        assert isinstance(cuerpo["puntaje"], float)

    def test_sin_modelo_usa_el_modelo_por_defecto_y_lo_informa(self):
        assert pedir().json()["modelo"] == VERSION_MODELO_DEFECTO

    def test_con_modelo_repite_la_version_pedida(self):
        # * El reprocesamiento (CU26) exige que la respuesta repita la versión que pidió.
        assert (
            pedir(modelo="v1.4", parametros=dict(PONDERACIONES_DEFECTO)).json()["modelo"] == "v1.4"
        )

    def test_ignora_campos_que_no_conoce(self):
        assert pedir(campoNuevo="x").status_code == 200

    def test_la_carrera_no_cambia_el_puntaje(self):
        assert (
            puntaje_de(carrera="Medicina")
            == puntaje_de(carrera="Ing. Informática")
            == puntaje_de(carrera=None)
        )

    def test_el_detalle_explica_cada_variable_y_suma_el_puntaje(self):
        cuerpo = pedir(**FORMULARIO_OFICIAL).json()
        variables = cuerpo["detalle"]["variables"]

        assert set(variables) == set(PONDERACIONES_DEFECTO)
        assert sum(v["puntos"] for v in variables.values()) == pytest.approx(
            cuerpo["puntaje"], abs=0.02
        )
        assert cuerpo["detalle"]["advertencias"] == []


class TestFormularioOficial:
    """Las variables del formulario oficial: dependencia, ocupación, hijos y discapacidad."""

    def test_las_ponderaciones_por_defecto_suman_100(self):
        assert sum(PONDERACIONES_DEFECTO.values()) == 100

    def test_el_caso_mas_necesitado_con_todos_los_datos_sigue_llegando_a_70(self):
        extremo = {
            **MAS_VULNERABLE,
            "dependenciaEconomica": "Independiente",
            "ocupacionSosten": "Asalariado informal",
            "tieneHijos": True,
            "cantidadHijos": "Más de 1",
            "discapacidad": True,
        }
        # Ocupación informal no es el nivel 1,0 (hay otras ocupaciones que se asumen más estables),
        # así que el tope se alcanza solo si todas las variables están al máximo.
        assert 60 < puntaje_de(**extremo) <= 70

    def test_no_tener_discapacidad_ni_hijos_aporta_cero(self):
        variables = pedir(**FORMULARIO_OFICIAL).json()["detalle"]["variables"]

        assert variables["discapacidad"]["puntos"] == 0
        assert variables["hijos"]["puntos"] == 0

    def test_la_discapacidad_suma_puntos(self):
        assert puntaje_de(**{**FORMULARIO_OFICIAL, "discapacidad": True}) > puntaje_de(
            **FORMULARIO_OFICIAL
        )

    def test_mas_hijos_dan_mas_puntaje(self):
        sin = puntaje_de(**FORMULARIO_OFICIAL)
        uno = puntaje_de(**{**FORMULARIO_OFICIAL, "tieneHijos": True, "cantidadHijos": "1"})
        varios = puntaje_de(
            **{**FORMULARIO_OFICIAL, "tieneHijos": True, "cantidadHijos": "Más de 1"}
        )

        assert sin < uno < varios

    @pytest.mark.parametrize(
        "campo, de_menor_a_mayor",
        [
            (
                "dependenciaEconomica",
                [
                    "Ambos padres",
                    "De la pareja",
                    "Solo de padre o madre",
                    "Otro familiar",
                    "Independiente",
                ],
            ),
            (
                "ocupacionSosten",
                [
                    "Rentista",
                    "Comerciante mayorista",
                    "Asalariado formal",
                    "Comerciante minorista",
                    "Agricultor",
                    "Asalariado informal",
                ],
            ),
        ],
    )
    def test_las_opciones_mas_necesitadas_dan_mas_puntaje(self, campo, de_menor_a_mayor):
        valores = [
            puntaje_de(**{**FORMULARIO_OFICIAL, campo: opcion}) for opcion in de_menor_a_mayor
        ]

        assert valores == sorted(valores)
        assert valores[0] < valores[-1]

    def test_una_postulacion_sin_esos_datos_no_se_penaliza_y_se_avisa(self):
        cuerpo = pedir().json()
        variables = cuerpo["detalle"]["variables"]

        ponderacion = PONDERACIONES_DEFECTO["discapacidad"]
        assert variables["discapacidad"] == {
            "nivel": None,
            "ponderacion": ponderacion,
            "puntos": 0.0,
        }
        assert any("sin dato" in aviso for aviso in cuerpo["detalle"]["advertencias"])
        # * El caso más vulnerable de las 6 variables originales llega al tope sin las demás.
        assert pedir(**MAS_VULNERABLE).json()["puntaje"] == 70

    def test_una_version_antigua_con_solo_seis_ponderaciones_sigue_funcionando(self):
        antiguas = {
            "ingresos_familiares": 28,
            "dependientes": 20,
            "procedencia": 15,
            "patrimonio": 15,
            "estado_vulnerabilidad": 10,
            "promedio_academico": 12,
        }

        cuerpo = pedir(parametros=antiguas, modelo="v0.9", **FORMULARIO_OFICIAL).json()

        assert cuerpo["modelo"] == "v0.9"
        assert cuerpo["detalle"]["variables"]["hijos"]["puntos"] == 0
        assert cuerpo["detalle"]["variables"]["ingresos_familiares"]["puntos"] > 0

    def test_una_opcion_desconocida_de_la_ocupacion_se_avisa_y_no_rechaza(self):
        cuerpo = pedir(**{**FORMULARIO_OFICIAL, "ocupacionSosten": "Astronauta"}).json()

        assert any("ocupación" in aviso for aviso in cuerpo["detalle"]["advertencias"])


class TestRango:
    def test_el_caso_mas_vulnerable_llega_al_tope_de_70(self):
        # Todas las variables en su nivel máximo: el 100 % de las ponderaciones.
        assert pedir(**MAS_VULNERABLE).json()["puntaje"] == PUNTAJE_MAXIMO == 70

    def test_el_caso_menos_vulnerable_da_cero(self):
        assert pedir(**MENOS_VULNERABLE).json()["puntaje"] == 0

    def test_cualquier_puntaje_queda_entre_0_y_70(self):
        for ingresos in (0, 1000, 2500, 3000, 5000, 7000, 12000):
            for dependientes in (0, 2, 6, 20):
                assert 0 <= puntaje_de(ingresosFamiliares=ingresos, dependientes=dependientes) <= 70

    def test_tiene_a_lo_mas_dos_decimales(self):
        puntaje = puntaje_de()

        assert puntaje == round(puntaje, 2)


class TestMonotonia:
    """Más vulnerable no puede dar menos puntaje (con todo lo demás igual)."""

    def test_menos_ingresos_dan_mas_puntaje(self):
        valores = [
            puntaje_de(ingresosFamiliares=i)
            for i in (1000, 2500, 3000, 4000, 5000, 6000, 7000, 9000)
        ]

        assert valores == sorted(valores, reverse=True)
        assert valores[0] > valores[-1]

    def test_mas_dependientes_dan_mas_puntaje(self):
        valores = [puntaje_de(dependientes=d) for d in range(0, 9)]

        assert valores == sorted(valores)
        assert valores[0] < valores[-1]

    @pytest.mark.parametrize(
        "campo, de_menor_a_mayor",
        [
            ("procedencia", ["Capital", "Provincia", "Otro departamento", "Área rural"]),
            (
                "patrimonio",
                [
                    "Vivienda propia",
                    "Vivienda en anticrético",
                    "Vivienda alquilada",
                    "Sin vivienda propia",
                ],
            ),
            (
                "estadoVulnerabilidad",
                [
                    "Sin situación especial",
                    "Dependiente de otro familiar",
                    "Hogar monoparental",
                    "Ingresos irregulares",
                    "Persona con discapacidad en el hogar",
                ],
            ),
        ],
    )
    def test_las_opciones_mas_necesitadas_dan_mas_puntaje(self, campo, de_menor_a_mayor):
        valores = [puntaje_de(**{campo: opcion}) for opcion in de_menor_a_mayor]

        assert valores == sorted(valores)
        assert valores[0] < valores[-1]

    def test_el_promedio_no_puntua_por_defecto_porque_el_auditor_ya_lo_suma(self):
        assert PONDERACIONES_DEFECTO["promedio_academico"] == 0
        assert puntaje_de(promedioAcademico=100) == puntaje_de(promedioAcademico=0)

    def test_si_la_dubss_le_da_peso_el_promedio_mas_alto_suma_mas(self):
        con_peso = dict(PONDERACIONES_DEFECTO) | {
            "promedio_academico": 10,
            "ingresos_familiares": 18,
        }

        alto = puntaje_de(parametros=con_peso, modelo="v2.0", promedioAcademico=90)
        bajo = puntaje_de(parametros=con_peso, modelo="v2.0", promedioAcademico=60)

        assert alto > bajo

    def test_ignora_mayusculas_y_espacios_en_las_opciones(self):
        assert puntaje_de(procedencia="  PROVINCIA ") == puntaje_de(procedencia="Provincia")


class TestParametros:
    def test_aplica_las_ponderaciones_que_recibe(self):
        solo_ingresos = {clave: 0 for clave in PONDERACIONES_DEFECTO} | {"ingresos_familiares": 100}

        # Ingresos de 1 800 = nivel 1 → 100 % de las ponderaciones → 70.
        assert puntaje_de(parametros=solo_ingresos, modelo="v2.0") == 70
        # Ingresos de 20 000 = nivel 0 → 0.
        assert puntaje_de(parametros=solo_ingresos, modelo="v2.0", ingresosFamiliares=20000) == 0

    def test_otro_juego_de_ponderaciones_cambia_el_puntaje(self):
        mas_peso_a_dependientes = dict(PONDERACIONES_DEFECTO) | {
            "dependientes": 40,
            "ingresos_familiares": 8,
        }

        assert puntaje_de(parametros=mas_peso_a_dependientes, modelo="v2.0") != puntaje_de()

    def test_ponderaciones_que_no_suman_100_se_normalizan(self):
        duplicadas = {clave: peso * 2 for clave, peso in PONDERACIONES_DEFECTO.items()}

        assert puntaje_de(parametros=duplicadas, modelo="v2.0") == puntaje_de()

    def test_una_variable_sin_ponderacion_aporta_cero(self):
        sin_dependientes = {c: p for c, p in PONDERACIONES_DEFECTO.items() if c != "dependientes"}

        cuerpo = pedir(parametros=sin_dependientes, modelo="v2.0").json()

        assert cuerpo["detalle"]["variables"]["dependientes"]["puntos"] == 0

    @pytest.mark.parametrize(
        "parametros",
        [
            {"ingresos_familiares": 50, "variable_inventada": 50},
            {"ingresos_familiares": -10, "dependientes": 110},
            {clave: 0 for clave in PONDERACIONES_DEFECTO},
        ],
        ids=["clave desconocida", "ponderación negativa", "todas en cero"],
    )
    def test_rechaza_ponderaciones_que_no_sirven_con_422(self, parametros):
        respuesta = pedir(parametros=parametros, modelo="v2.0")

        assert respuesta.status_code == 422
        assert respuesta.json()["detail"]


class TestEntradas:
    def test_una_opcion_desconocida_no_rechaza_la_postulacion_y_se_avisa(self):
        cuerpo = pedir(procedencia="Planeta X").json()

        assert 0 <= cuerpo["puntaje"] <= 70
        assert any("procedencia" in aviso for aviso in cuerpo["detalle"]["advertencias"])
        assert cuerpo["detalle"]["variables"]["procedencia"]["nivel"] == 0.5

    @pytest.mark.parametrize(
        "cambios",
        [
            {"promedioAcademico": 101},
            {"promedioAcademico": -1},
            {"ingresosFamiliares": -5},
            {"dependientes": -1},
            {"dependientes": 51},
            {"postulacion": "no-es-un-uuid"},
            {"procedencia": None},
        ],
    )
    def test_datos_fuera_de_rango_se_rechazan_con_422(self, cambios):
        assert pedir(**cambios).status_code == 422

    def test_sin_una_variable_obligatoria_se_rechaza_con_422(self):
        cuerpo = deepcopy(SOLICITUD)
        del cuerpo["patrimonio"]

        assert client.post(RUTA, json=cuerpo).status_code == 422


class TestRendimiento:
    def test_un_calculo_tarda_muy_por_debajo_de_los_5_segundos(self):
        # * RNF: el puntaje individual debe completarse en 5 s (el core corta la espera ahí).
        inicio = time.perf_counter()
        for _ in range(200):
            assert pedir().status_code == 200
        por_calculo = (time.perf_counter() - inicio) / 200

        assert por_calculo < 0.1
