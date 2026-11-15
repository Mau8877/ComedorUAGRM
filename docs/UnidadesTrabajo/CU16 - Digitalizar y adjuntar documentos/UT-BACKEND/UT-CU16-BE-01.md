# UT-CU16-BE-01 — Almacenamiento listo para S3 (región, credenciales, `eliminar`, bucket privado)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-BE-01` |
| **Nombre** | Almacenamiento listo para S3 (región, credenciales, `eliminar`, bucket privado) |
| **Caso de uso** | CU16-BE |
| **Depende de** | — |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) y [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- `AlmacenamientoService` y `MinioConfig` del CU07

## Solicitud de negocio

El proyecto desarrolla con MinIO y debe poder pasar a Amazon S3 en producción sin cambiar código, y los documentos con datos personales no pueden quedar con lectura pública.

## Objetivo

El almacenamiento se configura para MinIO o S3 solo con variables, puede eliminar objetos y el bucket de desarrollo es privado.

## Alcance

- `B/config/MinioConfig.java`: región (`app.minio.region`) y tipo de credenciales (`app.minio.credentials`: `static` con clave y secreto, `aws-iam` con `AwsEnvironmentProvider` + `IamAwsProvider`); cualquier otro valor falla al arrancar. `minioClientPublico` usa la misma configuración.
- `B/common/storage/AlmacenamientoService.java`: `eliminar(ruta)` (idempotente, un fallo sube como `AlmacenamientoException` sin la ruta).
- `B/common/exception/GlobalExceptionHandler.java`: `MaxUploadSizeExceededException` → `400` `ERR_SYS_06`; `MissingServletRequestPartException` y `MultipartException` → `400` `ERR_SYS_05`.
- `application.properties`: `app.minio.region`, `app.minio.credentials`, bucket por variable, `public-url` que cae en `url`, `app.documentos.*` y los límites de multipart.
- `infrastructure/minio/create-buckets.sh`: bucket privado (`mc anonymous set none`). `.env.example` del backend con las variables de S3.
- `docs/ALMACENAMIENTO_S3.md`: variables por entorno, pasos para pasar a S3 y permisos mínimos.
- Tests: `T/config/MinioConfigTest.java` (proveedor de credenciales y firma con la región) y los casos de `eliminar` en `T/common/storage/AlmacenamientoServiceTest.java`.

Archivos esperados (crear/tocar): los de la lista.

## Fuera de alcance (no tocar)

- Subir o validar documentos (`UT-CU16-BE-04`).
- Cambiar de cliente (no se agrega el SDK de AWS).
- Probar contra una cuenta de AWS real.

## Resultado esperado

Con variables de entorno distintas el mismo código usa MinIO o S3, y el bucket local ya no es público.

## Validación

1. `./mvnw test -Dtest=MinioConfigTest,AlmacenamientoServiceTest` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. El texto de `docs/ALMACENAMIENTO_S3.md` y los comentarios de `application.properties` coinciden con las variables reales.
