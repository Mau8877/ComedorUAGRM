# comedor-uagrm-ia

Módulo de IA de ComedorUAGRM (Python 3.12 + FastAPI): el cálculo del puntaje de
vulnerabilidad de la preselección de la Beca Comedor, que consumirá
`comedor-uagrm-core` por la red interna.

**Hoy es un esqueleto vacío**: solo expone `GET /health`. La lógica se agrega
con el primer caso de uso que la necesite, y las rules de este proyecto se
definen en ese momento (por ahora no tiene).

## Estructura

```
comedor-uagrm-ia/
├── app/
│   └── main.py            # FastAPI: app y /health
├── tests/
│   └── test_health.py
├── Dockerfile             # imagen de ejecución (producción); el compose de desarrollo no la usa
├── pyproject.toml         # configuración de pytest y ruff
├── requirements.txt       # dependencias de ejecución
└── requirements-dev.txt   # + pytest, httpx2 y ruff
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
