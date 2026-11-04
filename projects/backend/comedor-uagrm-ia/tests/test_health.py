from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_responde_up():
    respuesta = client.get("/health")

    assert respuesta.status_code == 200
    assert respuesta.json() == {"status": "UP"}
