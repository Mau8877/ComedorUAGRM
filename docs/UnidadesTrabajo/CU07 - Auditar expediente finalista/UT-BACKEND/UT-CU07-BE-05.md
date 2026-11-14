# UT-CU07-BE-05 — Almacenamiento en MinIO y acceso temporal a archivos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-BE-05` |
| **Nombre** | Almacenamiento en MinIO y acceso temporal a archivos |
| **Caso de uso** | CU07-BE |
| **Depende de** | — |

## Antes de implementar

- `docs/README.md` §2.6 (MinIO) y [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).

## Solicitud de negocio

Los documentos del expediente viven en MinIO y el administrador debe poder abrirlos sin que existan rutas públicas permanentes.

## Objetivo

Existe un servicio transversal que sube un objeto a MinIO y genera una URL temporal para abrirlo.

## Alcance

- Dependencia `io.minio:minio` en `pom.xml` con la versión fijada en una property (verificar contra Maven Central antes de fijarla, como se hizo con Bucket4j).
- `B/config/MinioConfig.java`: crea el `MinioClient` con `app.minio.url`, `app.minio.access-key`, `app.minio.secret-key` y `app.minio.bucket-name` (ya están en `application.properties`), con la **región fija** (`us-east-1`) para que firmar una URL no consulte la red.
- `B/common/storage/AlmacenamientoService.java` (transversal: lo usará también el CU-16): `subir(String ruta, byte[] contenido, String mimeType)`, `existe(String ruta)` y `urlTemporal(String ruta, Duration vigencia)`.
- La URL temporal se firma con el host público `app.minio.public-url` (por defecto `http://localhost:9000`), porque `http://comedor-uagrm-minio:9000` solo se ve dentro de Docker. Vigencia de **5 minutos**. *(Decisión del equipo.)*
- Un fallo de MinIO se loguea como `ERROR` (sin la ruta completa ni la URL) y sube como excepción de infraestructura: el `GlobalExceptionHandler` responde `500` genérico.
- Tests unitarios: la URL contiene el bucket, la ruta y la firma, y vence a los 5 minutos; fallo de MinIO.

Archivos esperados (crear): `MinioConfig`, `AlmacenamientoService` y sus tests. (Tocar): `pom.xml`, `application.properties`, `.env.example` y `docs/README.md` (variable `MINIO_PUBLIC_URL`).

## Fuera de alcance (no tocar)

- Subir archivos desde el postulante (CU-16).
- Usar el servicio desde el expediente (UT-09).
- Cambiar la configuración del contenedor de MinIO.

## Resultado esperado

Con MinIO levantado, se puede subir un objeto de prueba y abrir su URL temporal en el navegador.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
