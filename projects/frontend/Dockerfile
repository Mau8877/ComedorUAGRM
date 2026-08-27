# Etapa 1: Construcción (Build)
FROM node:20-alpine AS builder
RUN npm install -g pnpm

WORKDIR /app

# Copiar archivos de dependencias
COPY package.json ./
RUN pnpm install

# Copiar el código fuente y compilar
COPY . .
RUN pnpm build

# Etapa 2: Servir con Nginx para producción
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]