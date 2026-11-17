"""Cálculo del puntaje de vulnerabilidad (CU03).

Cada variable se traduce a un *nivel* entre 0 y 1 (1 = el caso más necesitado) y se pondera con los
puntos máximos de esa variable (CU26). La suma, llevada a la escala de la IA, es el puntaje. Es un
cálculo determinista y explicable: el `detalle` dice cuántos puntos aportó cada variable.

* Las ponderaciones las decide la DUBSS (CU26); la IA aplica las que recibe y no guarda estado.
* Los niveles de cada opción (qué tan vulnerable es «Área rural» frente a «Capital») son el
  criterio adoptado: cuanto mayor el nivel, mayor la necesidad. Ver el criterio general en
  `app/config.py`.
* Cada nivel intenta medir una cosa distinta: lo que ya mide otra variable no se vuelve a premiar
  (por ejemplo «Hogar monoparental» pesa menos porque la dependencia económica ya lo cuenta).
? A validar con la DUBSS; el catálogo de opciones también está por definir con ella.
"""

from collections.abc import Sequence

from app.config import PONDERACIONES_DEFECTO, PUNTAJE_MAXIMO, VERSION_MODELO_DEFECTO
from app.schemas import PuntajeCalculado, SolicitudPuntaje

# Ingresos mensuales de la familia (Bs) → nivel. Los puntos de quiebre son los rangos del formulario
# oficial de la DUBSS (2 500 / 4 000 / 6 000): hasta 2 500 es el nivel más alto y desde 8 000 no
# suma nada.
_INGRESOS: Sequence[tuple[float, float]] = (
    (2500.0, 1.0),
    (4000.0, 0.66),
    (6000.0, 0.33),
    (8000.0, 0.0),
)

# Con 6 dependientes o más se alcanza el nivel máximo.
_DEPENDIENTES_TOPE = 6

# Los textos son los de `OpcionesSocioeconomicas` del core.
# Quien viene de lejos paga vivienda y comida aparte: cuanto más lejos, más necesita el comedor.
_PROCEDENCIA = {
    "capital": 0.0,
    "provincia": 0.5,
    "otro departamento": 0.8,
    "área rural": 1.0,
}
# Tener vivienda propia alivia el gasto; el anticrético exige un capital que ya se pagó, así que
# pesa menos que el alquiler, que se paga cada mes.
_PATRIMONIO = {
    "vivienda propia": 0.0,
    "vivienda en anticrético": 0.4,
    "vivienda alquilada": 0.7,
    "sin vivienda propia": 1.0,
}
# Cuenta lo que no mide otra variable: el hogar monoparental pesa menos porque la dependencia
# económica ya lo cuenta, y la discapacidad en el hogar es distinta de la del propio postulante.
_ESTADO_VULNERABILIDAD = {
    "sin situación especial": 0.0,
    "dependiente de otro familiar": 0.5,
    "hogar monoparental": 0.6,
    "ingresos irregulares": 0.8,
    "persona con discapacidad en el hogar": 1.0,
}

# Del formulario oficial de la DUBSS (FORM-DUBS-001). Quien no tiene el apoyo de su familia o vive
# de un ingreso inestable es más vulnerable. «Independiente» no llega a 1: puede tener ingresos
# propios, y eso ya lo mide la variable de ingresos.
_DEPENDENCIA_ECONOMICA = {
    "independiente": 0.9,
    "otro familiar": 0.7,
    "solo de padre o madre": 0.6,
    "de la pareja": 0.3,
    "ambos padres": 0.0,
}
_OCUPACION_SOSTEN = {
    "asalariado informal": 0.8,
    "agricultor": 0.7,
    "comerciante minorista": 0.6,
    "asalariado formal": 0.3,
    "comerciante mayorista": 0.2,
    "rentista": 0.1,
}

# Lo que se asume cuando llega una opción que el modelo no conoce (el catálogo del core cambió): un
# nivel neutro, y se avisa en el detalle en vez de rechazar la postulación.
_NIVEL_DESCONOCIDO = 0.5


class ParametrosInvalidos(ValueError):
    """Las ponderaciones recibidas no sirven (clave desconocida, negativa o todas en cero)."""


def _interpolar(x: float, puntos: Sequence[tuple[float, float]]) -> float:
    if x <= puntos[0][0]:
        return puntos[0][1]
    for (x0, y0), (x1, y1) in zip(puntos, puntos[1:], strict=False):
        if x <= x1:
            return y0 + (y1 - y0) * (x - x0) / (x1 - x0)
    return puntos[-1][1]


def _nivel_categoria(
    valor: str, tabla: dict[str, float], variable: str, avisos: list[str]
) -> float:
    nivel = tabla.get(valor.strip().casefold())
    if nivel is None:
        avisos.append(
            f"{variable}: la opción «{valor}» no está en el modelo; se usó un nivel neutro"
        )
        return _NIVEL_DESCONOCIDO
    return nivel


def _nivel_promedio(promedio: float) -> float:
    # * Mérito: a mayor promedio, más nivel. Pesa 0 por defecto (ver `app/config.py`).
    return promedio / 100.0


def _nivel_hijos(tiene_hijos: bool | None, cantidad: str | None) -> float | None:
    """Sin hijos 0; con 1 hijo 0,6; con más de uno 1. Sin la respuesta, no hay dato."""
    if tiene_hijos is None:
        return None
    if not tiene_hijos:
        return 0.0
    return 1.0 if (cantidad or "").strip().casefold() == "más de 1" else 0.6


def niveles(s: SolicitudPuntaje, avisos: list[str]) -> dict[str, float | None]:
    """El nivel (0 a 1) de cada variable del modelo; `None` si la postulación no trae ese dato."""
    return {
        "ingresos_familiares": _interpolar(s.ingresos_familiares, _INGRESOS),
        "dependientes": min(s.dependientes, _DEPENDIENTES_TOPE) / _DEPENDIENTES_TOPE,
        "procedencia": _nivel_categoria(s.procedencia, _PROCEDENCIA, "procedencia", avisos),
        "patrimonio": _nivel_categoria(s.patrimonio, _PATRIMONIO, "patrimonio", avisos),
        "estado_vulnerabilidad": _nivel_categoria(
            s.estado_vulnerabilidad, _ESTADO_VULNERABILIDAD, "estado de vulnerabilidad", avisos
        ),
        "promedio_academico": _nivel_promedio(s.promedio_academico),
        "dependencia_economica": (
            None
            if s.dependencia_economica is None
            else _nivel_categoria(
                s.dependencia_economica, _DEPENDENCIA_ECONOMICA, "dependencia económica", avisos
            )
        ),
        "ocupacion_sosten": (
            None
            if s.ocupacion_sosten is None
            else _nivel_categoria(s.ocupacion_sosten, _OCUPACION_SOSTEN, "ocupación", avisos)
        ),
        "discapacidad": None if s.discapacidad is None else float(s.discapacidad),
        "hijos": _nivel_hijos(s.tiene_hijos, s.cantidad_hijos),
    }


def _ponderaciones(parametros: dict[str, float] | None) -> dict[str, float]:
    """Las ponderaciones a aplicar: las recibidas (CU26) o las del modelo por defecto."""
    if parametros is None:
        return dict(PONDERACIONES_DEFECTO)
    desconocidas = sorted(set(parametros) - set(PONDERACIONES_DEFECTO))
    if desconocidas:
        raise ParametrosInvalidos(f"Variables desconocidas: {', '.join(desconocidas)}")
    if any(p < 0 for p in parametros.values()):
        raise ParametrosInvalidos("Las ponderaciones no pueden ser negativas")
    pesos = {clave: float(parametros.get(clave, 0.0)) for clave in PONDERACIONES_DEFECTO}
    if sum(pesos.values()) <= 0:
        raise ParametrosInvalidos("Las ponderaciones deben sumar más de cero")
    return pesos


def calcular(s: SolicitudPuntaje) -> PuntajeCalculado:
    """El puntaje (0 a `PUNTAJE_MAXIMO`) de una postulación, con lo que aportó cada variable."""
    pesos = _ponderaciones(s.parametros)
    avisos: list[str] = []
    nivel = niveles(s, avisos)

    # * Una variable sin dato (postulación enviada antes de que el formulario la pidiera) no entra
    # * al cálculo: se reparte el 100 % entre las que sí tienen. Así un dato que falta no baja el
    # * puntaje por el solo hecho de faltar.
    # * Se divide por la suma de las ponderaciones de las variables con dato (en el core suman 100):
    # * así el resultado queda en 0-100 aunque no sumen exactamente 100.
    con_dato = {clave: peso for clave, peso in pesos.items() if nivel[clave] is not None}
    sin_dato = [clave for clave, peso in pesos.items() if nivel[clave] is None and peso > 0]
    if sin_dato:
        avisos.append("sin dato (no entran al puntaje): " + ", ".join(sin_dato))
    suma = sum(con_dato.values())
    if suma <= 0:
        raise ParametrosInvalidos("Las variables con dato no tienen ponderación")

    variables: dict[str, dict[str, float | None]] = {}
    total_100 = 0.0
    for clave, peso in pesos.items():
        valor = nivel[clave]
        if valor is None:
            variables[clave] = {"nivel": None, "ponderacion": round(peso, 2), "puntos": 0.0}
            continue
        puntos = valor * peso / suma * 100.0
        total_100 += puntos
        variables[clave] = {
            "nivel": round(valor, 4),
            "ponderacion": round(peso, 2),
            "puntos": round(puntos * PUNTAJE_MAXIMO / 100.0, 2),
        }
    puntaje = min(max(round(total_100 * PUNTAJE_MAXIMO / 100.0, 2), 0.0), PUNTAJE_MAXIMO)

    return PuntajeCalculado(
        puntaje=puntaje,
        modelo=s.modelo or VERSION_MODELO_DEFECTO,
        detalle={
            "variables": variables,
            "puntaje_sobre_100": round(total_100, 2),
            "puntaje_maximo": PUNTAJE_MAXIMO,
            "advertencias": avisos,
        },
    )
