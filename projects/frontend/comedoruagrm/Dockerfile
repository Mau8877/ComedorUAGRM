# Este proyecto es un workspace pnpm (ver /pnpm-workspace.yaml) -- el build
# de esta imagen SE HACE CON CONTEXTO EN LA RAÍZ DEL REPO, no en
# projects/frontend/comedoruagrm, porque `pnpm install` necesita
# pnpm-workspace.yaml y pnpm-lock.yaml de la raíz para instalar con el
# lockfile real (reproducible) en vez de resolver versiones nuevas cada
# build.
#
#   docker build -f projects/frontend/comedoruagrm/Dockerfile -t comedor-uagrm-frontend .
#
# (la versión vieja de este Dockerfile copiaba solo projects/frontend/package.json
# con contexto en projects/frontend/ -- nunca se probó un build real esta
# sesión porque todo el desarrollo pasa por docker-compose.yml en modo dev,
# y de hecho fallaba: sin el lockfile de la raíz, pnpm no podía resolver el
# workspace correctamente)

# ==========================================
# ETAPA 1: CONSTRUCCIÓN (Builder)
# ==========================================
FROM node:22-alpine AS builder
RUN corepack enable pnpm

WORKDIR /app

# 1. Copiamos primero solo los manifiestos (cache de Docker: si no cambian
# las dependencias, no se reinstala nada aunque cambie el código fuente)
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY packages/palette/package.json ./packages/palette/package.json
COPY projects/frontend/comedoruagrm/package.json ./projects/frontend/comedoruagrm/package.json

RUN pnpm install --frozen-lockfile --filter comedor-uagrm-frontend...

# 2. Copiamos el código fuente del frontend y compilamos
COPY packages/palette ./packages/palette
COPY projects/frontend/comedoruagrm ./projects/frontend/comedoruagrm
# Vite fija la URL de la API al compilar. Con la ruta relativa (default) el panel le habla al mismo sitio que lo
# sirve (detrás del proxy, /api/), así la misma imagen sirve en cualquier dominio.
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN pnpm --filter comedor-uagrm-frontend build

# ==========================================
# ETAPA 2: SERVIR CON NGINX (producción)
# ==========================================
FROM nginx:alpine
# Fallback de SPA (las rutas del router devuelven index.html) y cache de los assets con hash.
COPY infrastructure/aws/panel-nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/projects/frontend/comedoruagrm/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
