#!/bin/sh
set -e

# * `mc config host add` (el comando viejo) ya no existe en versiones
# * actuales de mc -- falla como "not a recognized command" pero el script
# * seguía igual (sin `set -e`) e imprimía falsos "Bucket created
# * successfully" en los pasos siguientes, porque sin un alias real
# * registrado, `mc mb myminio/...` cae a operar sobre una ruta de archivo
# * local del contenedor en vez de MinIO real. El bucket nunca se creaba.
# * `mc alias set` es el comando correcto y vigente.
mc alias set myminio http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"

# Crear el bucket si no existe. Nombre debe coincidir con
# app.minio.bucket-name en projects/backend/comedor-uagrm-core/src/main/resources/application.properties.
mc mb myminio/comedor-uagrm-storage --ignore-existing

# El bucket es PRIVADO: los documentos de las postulaciones (carnet, ingresos, notas) llevan datos personales y
# solo se abren con una URL temporal firmada que genera el backend. Se fija explícitamente porque una versión
# anterior de este script lo dejaba con lectura pública anónima, y esa configuración queda guardada en el volumen
# `minio_data` aunque el script cambie.
mc anonymous set none myminio/comedor-uagrm-storage

exit 0
