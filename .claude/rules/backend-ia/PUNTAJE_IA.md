---
globs: projects/backend/comedor-uagrm-ia/**
---

# Puntaje de vulnerabilidad — módulo de IA

Reglas de `projects/backend/comedor-uagrm-ia` (Python + FastAPI), definidas con su primer caso de uso
(el cálculo del puntaje de CU03 y CU26). El detalle de cómo se calcula está en su
[README](../../../projects/backend/comedor-uagrm-ia/README.md); acá solo lo que no se negocia.

## Contrato con el core

- Una sola ruta de negocio: `POST /api/v1/puntaje`. Su cuerpo y su respuesta son los de
  `SolicitudPuntaje` y `PuntajeCalculado` del core (`features/preseleccion/dto/`), en camelCase. Si uno
  cambia, cambia el otro **en el mismo cambio**, junto con `app/schemas.py` y los tests.
- **El puntaje va de 0 a 70** (`PUNTAJE_MAXIMO`). Lo fija la documentación de grado (la IA aporta hasta 70
  y el auditor hasta 30 en el CU07) y la base de datos lo rechaza por encima. Nunca se devuelve más.
- `modelo` repite siempre la versión pedida; sin versión se responde `VERSION_MODELO_DEFECTO`, que debe
  existir en `modelo_preseleccion` del core.
- **Sin estado:** no se guardan ponderaciones ni resultados. Se aplican los `parametros` que llegan en cada
  solicitud (los define la DUBSS en el CU26).
- Los errores de datos son `422` (el core los toma como «la IA rechazó los datos»). Una ruta que no existe
  es `404` y el core la toma como «no disponible»: no se cambia esa semántica.

## Qué es el puntaje

- Es determinista y **explicable**: cada variable aporta puntos y el `detalle` los muestra. No se agrega
  una caja negra al camino del puntaje oficial.
- Mide **necesidad económica y familiar** (quién más necesita la beca), no mérito: el auditor ya suma hasta
  30 puntos de rendimiento académico en el CU07, así que el promedio pesa 0 por defecto para no contarlo dos
  veces. Cada nivel mide algo distinto: lo que ya mide otra variable no se vuelve a premiar.
- La escala de 100 a 70 es lineal (`x 0,7`): conserva las proporciones entre postulantes.
- Es un criterio adoptado, a validar con la DUBSS; los pesos, los niveles y la escala viven solo en
  `app/config.py` y `app/scoring.py` (y las mismas ponderaciones en la migración que siembra `v1.0` en el
  core: un test lo comprueba). No se inventan datos institucionales en otro lugar.
- Una opción que el modelo no conoce **no rechaza** la postulación: nivel neutro y aviso en el `detalle`.

## Segunda opinión (aprendizaje automático)

- `POST /api/v1/segunda-opinion` es un contraste: **no** es el puntaje oficial, el core no lo usa ni lo
  guarda, y no cambia el contrato de `/api/v1/puntaje`.
- El modelo entregado está entrenado con **datos sintéticos** (`training/`). Sus métricas no se citan
  como validez del modelo; siempre viajan con la advertencia. Con datos reales de la DUBSS se
  reemplaza el generador y se reentrena.
- Un cambio a las variables del puntaje obliga a reentrenar (`python -m training.entrenar`) y a
  versionar de nuevo el archivo de `app/modelos/`.

## Privacidad

- La solicitud trae el código público de la postulación y las variables del modelo, nunca nombre, CI ni
  contacto. No se loguea el cuerpo de la solicitud ni el de la respuesta.

## Tests y estilo

- `pytest` (contrato, rango 0-70, monotonía: más vulnerable nunca da menos puntaje, parámetros y rechazos)
  y `ruff check . ; ruff format --check .`, líneas de 100 columnas. El job `ia` del CI corre ambos.
- Un cambio al cálculo actualiza sus tests en el mismo cambio.
