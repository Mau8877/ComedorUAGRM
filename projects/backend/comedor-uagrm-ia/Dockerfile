# ==========================================
# IMAGEN DE EJECUCIÓN (producción)
# ==========================================
# El docker-compose de desarrollo NO usa este archivo: corre la app con el
# código montado y recarga en caliente (ver el servicio `ia`). Esta imagen es
# la que se usará cuando se defina el despliegue (ver .claude/rules/CI_CD.md).
FROM python:3.12-slim

WORKDIR /app

# Instalar solo las dependencias de ejecución primero aprovecha el caché de
# Docker: si requirements.txt no cambia, no se vuelven a descargar.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app

# Práctica de seguridad: no ejecutar la app como root.
RUN useradd --system --no-create-home iauser
USER iauser

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
