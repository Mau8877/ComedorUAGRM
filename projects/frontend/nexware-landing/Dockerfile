# Imagen de PRODUCCIÓN de la landing de Nexware (el docker-compose de desarrollo no la usa: corre
# `astro build` + `astro preview` con el código montado).
#
#   docker build -t comedor-landing projects/frontend/nexware-landing
#
# El sitio se compila con `base: '/landing-page'` (astro.config.mjs): el build queda en la raíz de `dist/`
# y se sirve bajo ese prefijo, que es el que el proxy reenvía sin recortar.

FROM node:22-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html/landing-page
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
