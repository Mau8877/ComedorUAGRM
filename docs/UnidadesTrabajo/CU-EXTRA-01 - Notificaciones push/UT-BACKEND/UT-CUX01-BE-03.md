# UT-CUX01-BE-03 — Configuración de Firebase Admin

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CUX01-BE-03` |
| **Nombre** | Configuración de Firebase Admin |
| **Caso de uso** | CU-EXTRA-01-BE |
| **Depende de** | UT-CUX01-BE-01 |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (`config/`)
- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md)

## Solicitud de negocio

El backend debe poder hablar con Firebase si tiene la credencial, y arrancar con normalidad si no la tiene.

## Objetivo

Existe un `FirebaseMessaging` listo para inyectar **solo cuando** la credencial está disponible; si no, no hay bean y el arranque no falla.

## Alcance

- `pom.xml`: dependencia `com.google.firebase:firebase-admin` con una **versión fija verificada** en Maven Central (no se asume una versión de memoria).
- `B/config/FirebaseConfig.java`: crea `FirebaseApp` y expone `FirebaseMessaging` leyendo `app.firebase.credentials-path`. Una `Condition` propia (`FirebaseConfigurado`) activa el bean solo si la propiedad no está vacía y el archivo existe y se puede leer. En el arranque escribe un INFO («Firebase configurado») o un WARN («Firebase sin credenciales: el push queda en modo log»). **Nunca** escribe el contenido de la credencial; la ruta tampoco hace falta.
- Un JSON ilegible o corrupto no tumba el arranque: se registra un WARN con el tipo de la excepción y se sigue sin Firebase.
- Tests: `T/config/FirebaseConfigTest` (condición verdadera con un archivo temporal válido de prueba y falsa con ruta vacía o inexistente; sin llamar a Firebase). `src/test/resources/config/application.properties` deja la ruta vacía.

Archivos esperados (crear): `FirebaseConfig.java`, `FirebaseConfigurado.java` y su test. (Tocar): `pom.xml`.

## Fuera de alcance (no tocar)

- Enviar mensajes (`UT-CUX01-BE-04`).
- Volúmenes de Docker y variables (`UT-CUX01-BE-01`).

## Resultado esperado

Con `firebase-sa.json` el log muestra «Firebase configurado»; sin él, el WARN y el backend sigue en pie.

## Validación

1. `./mvnw test` pasa sin credenciales de Firebase (CI incluido).
2. Con la credencial real en `secrets/`, `docker compose up core` muestra el INFO; sin ella, el WARN.
3. `./mvnw checkstyle:check` sin errores.
