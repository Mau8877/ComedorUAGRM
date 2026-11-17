# comedor-uagrm-ia

Módulo de IA de ComedorUAGRM (Python 3.12 + FastAPI): el cálculo del puntaje de
vulnerabilidad de la preselección de la Beca Comedor, que consume
`comedor-uagrm-core` por la red interna (CU03 y, al reprocesar con otra versión
del modelo, CU26).

## Cómo calcula el puntaje

No es una caja negra: es un **puntaje ponderado y explicable**.

1. Cada variable del formulario se traduce a un *nivel* de 0 a 1 (1 = el caso
   más necesitado): ingresos familiares, dependientes, procedencia, patrimonio,
   estado de vulnerabilidad y promedio académico, más cuatro del formulario
   oficial de la DUBSS: dependencia económica, ocupación de quien sostiene,
   discapacidad e hijos (`app/scoring.py`).
2. Cada nivel se multiplica por la **ponderación** de esa variable. Las
   ponderaciones las decide la DUBSS en el CU26 y el core las manda en cada
   solicitud (`parametros`): la IA no guarda estado. Si todavía no hay ninguna
   versión guardada, usa las del modelo por defecto (`app/config.py`), que son las
   de la versión `v1.0` que el core siembra al migrar.
3. La suma, llevada a 0-70, es el puntaje. El `detalle` de la respuesta dice
   cuántos puntos aportó cada variable.

**El tope de 70 es de la documentación de grado:** la IA aporta hasta 70 puntos y
el auditor suma hasta 30 de rendimiento académico en el CU07, para un total de 100.

**Datos que faltan:** las cuatro variables del formulario oficial son opcionales.
Una postulación enviada antes de que el formulario las pidiera no las trae y esas
variables no entran al puntaje: el 100 % se reparte entre las que sí tienen dato
(`detalle.advertencias` lo dice). Una versión antigua de seis ponderaciones sigue
funcionando: lo que no trae cuenta como 0.

## Criterio de puntaje

La beca de comedor es para quien más la necesita, así que el puntaje mide
**necesidad económica y familiar**. Las ponderaciones por defecto (versión `v1.0`)
se reparten por dimensión:

| Dimensión | Puntos | Variables |
| --- | --- | --- |
| Capacidad económica del hogar | 45 | ingresos familiares 28, vivienda 9, ocupación de quien sostiene 8 |
| Carga familiar | 25 | dependientes 12, estado de vulnerabilidad 7, hijos 6 |
| Apoyo familiar y situación personal | 20 | dependencia económica 10, discapacidad 10 |
| Costo de estudiar lejos de casa | 10 | procedencia |
| Mérito académico | 0 | promedio académico |

- **El promedio académico pesa 0** porque el auditor ya suma hasta 30 puntos de
  rendimiento académico en el CU07; sumarlo también acá lo contaría dos veces. La
  DUBSS puede darle peso desde el CU26.
- **Cada nivel mide una cosa distinta:** lo que ya mide otra variable no se vuelve a
  premiar (el hogar monoparental pesa menos en «estado de vulnerabilidad» porque la
  dependencia económica ya lo cuenta; la discapacidad *en el hogar* es distinta de la
  del propio postulante).
- **Los ingresos** se miden con los rangos del formulario oficial de la DUBSS
  (2 500 / 4 000 / 6 000 Bs) con interpolación lineal, para que no haya saltos
  bruscos: hasta 2 500 es el nivel máximo y desde 8 000 no suma nada.
- **Escala 100 → 70:** lineal (`× 0,7`). Conserva las proporciones entre postulantes
  (quien tiene el doble de necesidad tiene el doble de puntaje) y no cambia el orden;
  truncar en 70 empataría a los casos más necesitados justo donde más importa
  distinguirlos.

Ejemplos con las ponderaciones por defecto (de 70):

| Perfil | Puntaje |
| --- | --- |
| Familia estable de la ciudad (ingresos 7 000, vivienda propia) | 6,31 |
| Clase media con alquiler (ingresos 4 500) | 21,89 |
| Hogar monoparental, ingresos 2 500, alquila | 39,83 |
| Viene de provincia, ingresos 2 000, alquila, comerciante | 40,67 |
| Independiente con un hijo, de provincia, ingresos 1 500 | 42,21 |
| Del área rural, ingresos 1 200, 5 dependientes, agricultor | 48,72 |
| El caso más necesitado (rural, sin ingresos, discapacidad, hijos) | 68,18 |

**Es un criterio adoptado, no una norma de la DUBSS:** los pesos, el nivel de cada
opción y la escala se pueden cambiar sin tocar la lógica (`app/config.py` y
`app/scoring.py`) y los pesos desde el CU26. El catálogo de opciones de las listas
sigue *por definir con la DUBSS*.

> El módulo **no** usa OCR ni lee los documentos adjuntos: recibe solo las
> respuestas del formulario que arma el core. Tampoco hay un dataset real de la
> DUBSS, así que no hay un modelo entrenado en producción.

## API

| Método y ruta | Qué hace |
| --- | --- |
| `GET /health` | `{"status": "UP"}` |
| `POST /api/v1/puntaje` | Calcula el puntaje de una postulación |
| `POST /api/v1/segunda-opinion` | Contrasta ese puntaje con el de un modelo aprendido (no lo usa el core) |
| `GET /api/v1/segunda-opinion/metricas` | Métricas del entrenamiento del modelo aprendido |

`POST /api/v1/puntaje` recibe lo mismo que `SolicitudPuntaje` del core (camelCase)
y responde `{ "puntaje", "modelo", "detalle" }`:

```json
// Solicitud
{ "postulacion": "uuid", "ingresosFamiliares": 1800.00, "dependientes": 4,
  "procedencia": "Provincia", "carrera": "Ing. Informática",
  "patrimonio": "Sin vivienda propia", "estadoVulnerabilidad": "Hogar monoparental",
  "promedioAcademico": 72.5,
  "dependenciaEconomica": "Solo de padre o madre", "ocupacionSosten": "Comerciante minorista",
  "tieneHijos": false, "cantidadHijos": null, "discapacidad": false, "modelo": "v1.4",
  "parametros": { "ingresos_familiares": 28, "dependientes": 12, "procedencia": 10,
                  "patrimonio": 9, "estado_vulnerabilidad": 7, "promedio_academico": 0,
                  "dependencia_economica": 10, "ocupacion_sosten": 8, "discapacidad": 10,
                  "hijos": 6 } }
// Respuesta 200
{ "puntaje": 55.67, "modelo": "v1.4", "detalle": { "variables": { "...": "..." },
  "puntaje_sobre_100": 79.53, "puntaje_maximo": 70.0, "advertencias": [] } }
```

- `puntaje` va de 0 a 70 con 2 decimales (la base lo rechaza por encima de 70).
- `modelo` repite la versión pedida (el reprocesamiento del CU26 lo exige). Sin
  versión se responde `v1.0` (`VERSION_MODELO_DEFECTO`): el core la registra al migrar
  (inactiva, con estas mismas ponderaciones); sin ella el puntaje se rechaza allá.
- Una opción que el modelo no conoce (el catálogo del core cambió) no rechaza la
  postulación: se usa un nivel neutro y se avisa en `detalle.advertencias`.
- **`422`** si faltan datos o están fuera de rango, o si las ponderaciones no
  sirven (clave desconocida, negativa o todas en cero). El core lo trata como «la
  IA rechazó los datos».

## Segunda opinión: modelo de aprendizaje automático

El puntaje oficial es el ponderado de arriba. Además hay un **Random Forest** (scikit-learn) que
lo acompaña como contraste: `POST /api/v1/segunda-opinion` devuelve `puntajePonderado` (el oficial),
`puntajeAprendido` (0 a 70), su `diferencia` y una `advertencia`. El core no lo llama ni se guarda.

> **Está entrenado con datos sintéticos.** No hay postulaciones reales de la DUBSS; los
> postulantes los inventa `training/generar_datos.py` (con semilla fija) y su «necesidad real» sale de
> otra lógica que la regla ponderada (mira el ingreso por persona, interacciones y ruido). Las
> métricas miden qué tan bien reproduce *esa* necesidad inventada, **no** qué tan bien acierta con
> postulantes de verdad: demuestran el proceso, no validan el modelo. Con datos reales de la DUBSS
> se reemplaza el generador por su lectura y se vuelve a entrenar.

```bash
python -m training.entrenar          # genera 20 000 postulantes, entrena y guarda app/modelos/
```

Guarda `app/modelos/ml_v1.joblib` y `ml_v1.metricas.json` (R², MAE, correlación de orden, cuánto
del 10 % más necesitado acierta, importancia de cada variable). Si falta el archivo, la ruta
responde `503`.

## Estructura

```
comedor-uagrm-ia/
├── app/
│   ├── main.py            # FastAPI: /health y /api/v1/puntaje
│   ├── schemas.py         # contrato con el core (SolicitudPuntaje, PuntajeCalculado)
│   ├── scoring.py         # niveles por variable y puntaje ponderado
│   ├── config.py          # versión por defecto, tope de 70 y ponderaciones por defecto
│   ├── ml.py              # segunda opinión: carga y predicción del modelo aprendido
│   └── modelos/           # ml_v1.joblib y sus métricas (generados por training/)
├── training/              # datos sintéticos y entrenamiento del modelo aprendido
├── tests/
│   ├── test_health.py
│   ├── test_puntaje.py    # contrato, rango 0-70, monotonía, parámetros y rechazos
│   └── test_segunda_opinion.py
├── Dockerfile             # imagen de ejecución (producción); el compose de desarrollo no la usa
├── pyproject.toml         # configuración de pytest y ruff
├── requirements.txt       # dependencias de ejecución
└── requirements-dev.txt   # + pytest, httpx2, ruff, pandas y scipy (entrenamiento)
```

## Comandos

Desde esta carpeta, con Python 3.12 o superior:

```bash
python -m venv .venv                       # una vez
source .venv/bin/activate                  # en Windows: .venv\Scripts\activate
pip install -r requirements-dev.txt
uvicorn app.main:app --reload              # http://localhost:8000/health
pytest                                     # tests
ruff check . ; ruff format --check .       # lint y formato
```

Con Docker Compose, desde la raíz del repo: `docker compose up ia` (recarga en
caliente, puerto `8000`; no pasa por el proxy).
