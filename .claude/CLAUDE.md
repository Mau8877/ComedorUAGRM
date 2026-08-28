# ComedorUAGRM

Sistema de gestión del comedor universitario de la UAGRM. Monorepo con
pnpm workspaces (`pnpm-workspace.yaml`: `projects/*`) que contiene tres
proyectos independientes más la infraestructura de desarrollo:

- **`projects/backend`** — API REST en Java 21 + Spring Boot 4.1 (Maven),
  PostgreSQL + Flyway para persistencia, Redis para caché, MinIO para
  almacenamiento de archivos.
- **`projects/frontend`** — Panel web en React 19 + TypeScript + Vite,
  TanStack (Query/Router/Form/Table), Tailwind CSS 4 + shadcn.
- **`projects/mobile`** — App en Flutter/Dart, Riverpod para estado,
  `go_router` para navegación, `dio` como cliente HTTP.
- **`infrastructure/`** — configuración de Docker Compose: nginx (proxy),
  postgres, redis, minio, prometheus, grafana.

## Comandos básicos

### Backend (`projects/backend`)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # correr en modo dev
./mvnw test                                              # tests
./mvnw clean package                                     # build
```

### Frontend (`projects/frontend`)

```bash
pnpm dev       # servidor de desarrollo (Vite, puerto 5173)
pnpm build     # build de producción
pnpm lint      # ESLint
```

### Mobile (`projects/mobile`)

```bash
flutter pub get     # instalar dependencias
flutter run          # correr en un emulador/dispositivo conectado
flutter test          # tests
flutter analyze       # análisis estático
```

### Stack completo (Docker Compose)

Desde la raíz del repo, con los `.env` ya creados a partir de sus
`.env.example` correspondientes (raíz, `projects/backend/`,
`projects/frontend/`):

```bash
docker compose up
```

Levanta proxy (puerto 80), frontend (5173), backend (8080), postgres (5432),
redis (6379), minio (9000/9001), prometheus (9090) y grafana (3000). Ver
[docs/README.md](../docs/README.md) para el detalle completo de variables de
entorno y el checklist de puesta en marcha.

## Reglas del proyecto (`.claude/rules/`)

**Antes de trabajar en cualquier archivo del proyecto, hay que respetar las
reglas correspondientes en `.claude/rules/`** según la carpeta/proyecto en
el que se esté trabajando (backend, frontend, mobile, o transversal a todo
el repo). No es opcional ni una sugerencia — son las decisiones ya tomadas
sobre cómo se construye este proyecto específico, y anulan cualquier
default genérico de "buenas prácticas" cuando entran en conflicto.

El propio frontmatter `globs:` de cada archivo de regla determina en qué
carpetas aplica automáticamente — no hace falta ir a buscarlas a mano, pero
si algo no queda claro para el archivo que se está por tocar, se revisa la
carpeta `.claude/rules/` correspondiente antes de asumir un criterio propio.
