"""Constantes del modelo de preselección.

El puntaje de vulnerabilidad lo consume `comedor-uagrm-core` (CU03 y CU26). Todo lo que la DUBSS
todavía tiene que definir vive acá, en un solo lugar, para cambiarlo sin tocar la lógica.
"""

import os

# Versión del modelo que se usa cuando el core todavía no guardó ninguna (CU26). Debe existir en la
# tabla `modelo_preseleccion` del core: si no, el puntaje se rechaza allá. Se responde con esa misma
# versión.
VERSION_MODELO_DEFECTO = "v1.0"

# Tope del puntaje que aporta la IA. La base de datos lo limita a 0-70
# (`puntaje_postulacion.puntaje_total`): el auditor suma hasta 30 más en el CU07, para un total
# de 100.
# * Criterio adoptado: las ponderaciones suman 100 y el resultado se escala linealmente (x 0,7).
# * Una escala lineal conserva las proporciones entre postulantes (quien tiene el doble de
# * necesidad tiene el doble de puntaje) y no cambia el orden; truncar en 70 empataría a los casos
# * más necesitados justo donde más importa distinguirlos.
# ? A validar con la DUBSS.
PUNTAJE_MAXIMO = float(os.environ.get("PUNTAJE_MAXIMO", "70"))

# Las variables del modelo (las mismas claves de `parametro_modelo.clave` en el core) y sus
# ponderaciones por defecto, en puntos máximos que suman 100. Son las de la versión `v1.0` que el
# core siembra al migrar (`sembrar_modelo_preseleccion_v1_0`): si se cambian acá, se cambian allá.
#
# * Criterio adoptado: la beca de comedor es para quien más la necesita, así que el puntaje mide
# * necesidad económica y familiar, repartida por dimensión:
# *   - Capacidad económica del hogar (45): ingresos 28, vivienda 9, ocupación de quien sostiene 8.
# *   - Carga familiar (25): dependientes 12, estado de vulnerabilidad 7, hijos 6.
# *   - Apoyo familiar y situación personal (20): dependencia económica 10, discapacidad 10.
# *   - Costo de estudiar lejos de casa (10): procedencia.
# * El promedio académico pesa 0: el auditor ya suma hasta 30 puntos de rendimiento académico en el
# * CU07, y sumarlo también acá lo contaría dos veces. La DUBSS puede darle peso desde el CU26.
# ? A validar con la DUBSS, que los fija en el CU26.
PONDERACIONES_DEFECTO: dict[str, float] = {
    "ingresos_familiares": 28.0,
    "dependientes": 12.0,
    "procedencia": 10.0,
    "patrimonio": 9.0,
    "estado_vulnerabilidad": 7.0,
    "promedio_academico": 0.0,
    # Del formulario oficial de la DUBSS (FORM-DUBS-001).
    "dependencia_economica": 10.0,
    "ocupacion_sosten": 8.0,
    "discapacidad": 10.0,
    "hijos": 6.0,
}
