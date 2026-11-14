# UT-CUX01-BE-01 — Docker, secretos y variables de Firebase

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CUX01-BE-01` |
| **Nombre** | Docker, secretos y variables de Firebase |
| **Caso de uso** | CU-EXTRA-01-BE |
| **Depende de** | — |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md)
- [`CI_CD.md`](../../../../.claude/rules/CI_CD.md) (el CI no usa secretos)
- [`docs/README.md`](../../../README.md) (variables de entorno y puesta en marcha)
- [`secrets/README.md`](../../../../projects/backend/comedor-uagrm-core/secrets/README.md)

## Solicitud de negocio

Que `docker compose up` deje al backend con todo lo necesario para enviar push reales cuando la persona tiene las credenciales de Firebase, y que quien no las tiene pueda trabajar igual, sin pasos extra ni errores.

## Objetivo

El contenedor `core` ve la credencial de Firebase como archivo de solo lectura, el backend sabe dónde buscarla por una variable de entorno, y todo queda documentado, sin que ningún secreto llegue a git ni a una imagen.

## Alcance

- `.gitignore`, `secrets/README.md`, `secrets/.gitkeep` y `google-services.json.example`: **ya hechos** al redactar el CU (verificar que siguen vigentes).
- `docker-compose.yml`, servicio `core`: volumen `./projects/backend/comedor-uagrm-core/secrets:/run/secrets:ro` (se monta la **carpeta**, no el archivo: si el archivo no existe, Docker no crea una carpeta con su nombre) y variable `FIREBASE_CREDENTIALS_PATH=${FIREBASE_CREDENTIALS_PATH:-/run/secrets/firebase-sa.json}`. Comentario en el compose explicando que sin el archivo el push queda en modo log.
- `application.properties`: `app.firebase.credentials-path=${FIREBASE_CREDENTIALS_PATH:}` y `app.notificaciones.android-channel-id=${NOTIFICACIONES_CANAL_ANDROID:notificaciones}`.
- `projects/backend/comedor-uagrm-core/.env.example`: bloque comentado `FIREBASE_CREDENTIALS_PATH` (en Docker ya trae el valor por defecto; fuera de Docker se apunta a la ruta local del JSON).
- Verificar que el `Dockerfile` de `comedor-uagrm-core` y su `.dockerignore` (crearlo si no existe) **no copian `secrets/`** dentro de una imagen.
- `docs/README.md`: sección «Notificaciones push (Firebase)» con: qué es cada archivo y de dónde se baja, dónde se deja, que el archivo debe llamarse `firebase-sa.json`, que sin él todo funciona con el push en modo log, cómo pasárselo a un compañero (canal privado, nunca git ni chat grupal) o que cada quien use su proyecto Firebase (con ambos archivos del mismo proyecto), y que el CI no necesita secretos. Mismo aviso en `CLAUDE.md` (una línea en la lista de variables/servicios).

Archivos esperados (tocar): `docker-compose.yml`, `application.properties`, `.env.example` del core, `docs/README.md`, `CLAUDE.md`. (Crear si falta): `projects/backend/comedor-uagrm-core/.dockerignore`.

## Fuera de alcance (no tocar)

- Leer la credencial desde el código y crear el cliente de Firebase (`UT-CUX01-BE-03`).
- Despliegue o secretos de producción (depende de la decisión de CD, ver `CI_CD.md`).
- Los cambios de Gradle y del `google-services.json` en la app móvil (`UT-CUX01-MOB-01`).

## Resultado esperado

Con `secrets/firebase-sa.json` presente, dentro del contenedor `core` existe `/run/secrets/firebase-sa.json` y la variable apunta a él; sin el archivo, `docker compose up` arranca igual. Ningún secreto aparece en `git status`.

## Validación

1. `docker compose config` muestra el volumen de solo lectura y la variable en `core`.
2. `docker compose up -d core` y `docker exec comedor-uagrm-core ls /run/secrets` lista `firebase-sa.json` (con el archivo puesto) o la carpeta vacía (sin él), y el contenedor arranca en ambos casos.
3. `git status` no muestra nada de `secrets/` ni `google-services.json`; `git check-ignore -v` los reconoce.
4. `docker build` del core no incluye `secrets/` (revisar la capa o el `.dockerignore`).
