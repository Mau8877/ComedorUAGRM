# Almacenamiento de archivos: MinIO en desarrollo, Amazon S3 en producción

Los archivos (documentos de las postulaciones, CU16, y los del expediente, CU07) no viven en la base de datos:
la tabla guarda solo la ruta, el tipo y el tamaño. El binario se guarda en un almacenamiento de objetos.

- **Desarrollo:** MinIO, el contenedor `minio` del `docker-compose.yml`.
- **Producción:** Amazon S3.

Es **el mismo código** en los dos: el cliente de MinIO habla el protocolo S3. Pasar de uno a otro es cambiar variables de entorno,
no tocar código (`config/MinioConfig.java`, `common/storage/AlmacenamientoService.java`).

## Variables

| Variable | Desarrollo (MinIO) | Producción (S3) |
| --- | --- | --- |
| `MINIO_URL` | `http://comedor-uagrm-minio:9000` (o `http://localhost:9000` fuera de Docker) | `https://s3.<región>.amazonaws.com` |
| `MINIO_PUBLIC_URL` | `http://localhost:9000` (lo que ve el navegador) | no se define: usa `MINIO_URL` |
| `MINIO_REGION` | `us-east-1` (da igual) | la región **real** del bucket, p. ej. `sa-east-1` |
| `STORAGE_CREDENTIALS` | `static` | `aws-iam` |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | la clave y el secreto de MinIO | no se usan |
| `STORAGE_BUCKET` | `comedor-uagrm-storage` | el nombre del bucket |

`STORAGE_CREDENTIALS=aws-iam` toma las credenciales del rol IAM de la máquina o del contenedor (EC2, ECS, EKS) o de las
variables `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` y `AWS_SESSION_TOKEN`. **No se guardan claves en la configuración.**

## Pasos para pasar a S3

1. Crear el bucket (en la región elegida), con **Block all public access** activado: los documentos llevan datos personales y
   se abren solo con una URL temporal firmada, nunca con una ruta pública.
2. Dar al rol del backend un permiso mínimo sobre ese bucket: `s3:PutObject`, `s3:GetObject` y `s3:DeleteObject` sobre
   `arn:aws:s3:::<bucket>/*`, y `s3:ListBucket` sobre `arn:aws:s3:::<bucket>`. Este último hace falta: sin él, S3 responde
   `403` (y no `404`) cuando se consulta un objeto que no existe, y `AlmacenamientoService.existe()` lo tomaría como un fallo.
3. Definir las variables de la tabla de arriba en el entorno de producción.
4. Si el navegador sube o descarga desde otro dominio, configurar CORS en el bucket. Hoy **no hace falta**: la subida pasa por el
   backend y el navegador solo hace `GET` de la URL firmada (una descarga o una imagen, sin CORS).
5. Probar: subir un documento desde la app, abrirlo con su URL temporal y eliminarlo.

## Qué conviene saber

- **La región tiene que ser la real.** La firma de las URL temporales la incluye; con una región distinta de la del bucket,
  S3 rechaza el enlace.
- **Las URL temporales duran 5 minutos** (`AlmacenamientoService.VIGENCIA_ACCESO`).
- **En producción no hay dos direcciones** (interna y pública, como en Docker): `MINIO_PUBLIC_URL` queda igual a `MINIO_URL`.
- **El bucket de desarrollo también es privado.** `infrastructure/minio/create-buckets.sh` ya no lo deja con lectura
  anónima. Si tu volumen `minio_data` es antiguo, vuelve a correr `docker compose up minio-init` para que se aplique.
- **Sin probar contra AWS real.** El soporte de S3 está cubierto con pruebas unitarias (la firma con la región y la selección
  de credenciales), pero no se ha ejecutado contra una cuenta de AWS: la primera vez conviene probar el flujo completo en un
  bucket de pruebas.
