# secrets/ — credenciales que NO se versionan

Todo lo que hay en esta carpeta, salvo este `README.md` y el `.gitkeep`, está
en el `.gitignore`. Docker la monta en el contenedor `core` como solo lectura
en `/run/secrets`.

## `firebase-sa.json` — cuenta de servicio de Firebase (CU-EXTRA-01)

Credencial con la que el backend envía notificaciones push (FCM).

1. Firebase Console → tu proyecto → Configuración del proyecto → Cuentas de
   servicio → «Generar nueva clave privada».
2. Guarda el JSON descargado aquí como **`firebase-sa.json`**.
3. El backend la lee desde `FIREBASE_CREDENTIALS_PATH` (ver `.env.example`).
   En Docker ya apunta a `/run/secrets/firebase-sa.json`; al correr el
   backend fuera de Docker, apunta a la ruta de este archivo.

**Sin este archivo el backend arranca igual**, y el push solo queda escrito en
el log (nada se envía a Firebase). Los demás compañeros pueden trabajar así.

Ese JSON da permisos sobre el proyecto de Firebase: no lo subas a git, no lo
pegues en un chat grupal y compártelo solo por un canal privado. Para
ver la app móvil recibir un push real, el `google-services.json` de la app
y este archivo deben salir del **mismo** proyecto de Firebase.
