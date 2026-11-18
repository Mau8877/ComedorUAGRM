# Despliegue en AWS (demo universitaria)

Una instancia EC2 corre todo el stack con `docker-compose.prod.yml`; CloudFront pone el HTTPS.

```
Navegador ──HTTPS──> CloudFront ──HTTP:80──> EC2 (nginx) ─┬─> panel   (React, nginx)
                                                           ├─> landing (Astro estático, /landing-page)
                                                           ├─> core    (Spring Boot, /api) ─> postgres, redis, ia
                                                           └─> S3 (documentos), SES (correo), FCM (push)
```

| Recurso | Valor |
| --- | --- |
| Región / cuenta | `sa-east-1` / `564589597625` |
| Instancia | `i-0bed8c5e07f5680ac` (`c7i-flex.large`, plan gratuito), IP elástica `56.126.91.250` |
| CloudFront | `E3E9NXWVNEQ8SX`, `https://d38nteg7p7v4qz.cloudfront.net` |
| Imágenes | ECR `comedor-core`, `comedor-ia`, `comedor-panel`, `comedor-landing` |
| Archivos | S3 `comedor-uagrm-storage-<cuenta>` (privado) |
| Paquete de despliegue | S3 `comedor-uagrm-deploy-<cuenta>/bundle/` (compose, nginx, `.env`, `secrets/`) |

Sin SSH: se entra a la instancia con Session Manager (`aws ssm start-session --target <id>`). El puerto 80 solo
acepta tráfico de CloudFront.

## Actualizar una versión

```bash
# 1. Construir y subir las imágenes (desde la raíz del repo)
docker build -f projects/frontend/comedoruagrm/Dockerfile -t comedor-panel .
docker build -t comedor-core projects/backend/comedor-uagrm-core      # idem ia, landing
docker tag comedor-core 564589597625.dkr.ecr.sa-east-1.amazonaws.com/comedor-core:latest
aws ecr get-login-password --region sa-east-1 | docker login --username AWS --password-stdin 564589597625.dkr.ecr.sa-east-1.amazonaws.com
docker push 564589597625.dkr.ecr.sa-east-1.amazonaws.com/comedor-core:latest

# 2. En la instancia (SSM): bajar y reiniciar
cd /opt/comedor && docker compose -f docker-compose.prod.yml --env-file .env pull && \
  docker compose -f docker-compose.prod.yml --env-file .env up -d
```

## Notas

- El core corre con el perfil `dev` **a propósito**: es el que activa los datos de demostración. Todas las cuentas
  de demo comparten `DEMO_PASSWORD` (en el `.env` de la instancia, no se versiona).
- El `.env` y `secrets/firebase-sa.json` no están en el repo; viven en el bucket de despliegue (privado).
- **SES está en modo sandbox:** solo envía a direcciones verificadas. Para correos a cualquiera hay que pedir
  acceso a producción en la consola de SES.
- El rol de la instancia no sirve para S3 (el cliente de MinIO no completa IMDSv2): se usa un usuario de IAM
  acotado a ese bucket (`STORAGE_CREDENTIALS=static`).
- El rate limit cuenta por IP del proxy (todos comparten cupo), por eso se subió a 3000/min.
- Apagar para no gastar: `aws ec2 stop-instances` (la IP elástica sin instancia asociada sí cobra).
