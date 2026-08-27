#!/bin/sh
# Esperar a que MinIO levante
sleep 5;
# Configurar el cliente (mc) con las credenciales por defecto (cámbialas según tu .env)
/usr/bin/mc config host add myminio http://minio:9000 minioadmin minioadmin;
# Crear el bucket si no existe
/usr/bin/mc mb myminio/app-media --ignore-existing;
# Hacer el bucket público para lectura de imágenes
/usr/bin/mc anonymous set download myminio/app-media;
exit 0;