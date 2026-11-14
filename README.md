# ComedorUAGRM

Sistema de gestión del comedor universitario de la UAGRM (Universidad
Autónoma Gabriel René Moreno). Monorepo con proyectos independientes
(backend con dos módulos, frontend con dos aplicaciones y mobile) más la
infraestructura de desarrollo que los conecta a todos.

## Stack

| Proyecto | Tecnología |
| --- | --- |
| **Backend — core** (`projects/backend/comedor-uagrm-core`) | Java 21 + Spring Boot 4.1 (Maven), PostgreSQL + Flyway, Redis (caché), MinIO (archivos) |
| **Backend — IA** (`projects/backend/comedor-uagrm-ia`) | Python 3.12 + FastAPI (módulo de IA de la preselección; hoy un esqueleto con `/health`) |
| **Frontend — ComedorUAGRM** (`projects/frontend/comedoruagrm`) | React 19 + TypeScript + Vite, TanStack (Query/Router/Form/Table), Tailwind CSS 4 + shadcn |
| **Frontend — Landing Nexware** (`projects/frontend/nexware-landing`) | Astro + TypeScript, landing bilingüe (es/en) de Nexware, la empresa ficticia dueña de este proyecto |
| **Mobile** (`projects/mobile`) | Flutter/Dart, Riverpod (estado), `go_router` (navegación), `dio` (HTTP) |
| **Infraestructura** (`infrastructure/`) | Docker Compose: nginx (proxy), postgres, redis, minio, prometheus, grafana |

`pnpm-workspace.yaml` incluye `projects/*` y
`projects/frontend/comedoruagrm` como workspace de pnpm (`comedoruagrm` y
mobile comparten herramientas de raíz; `comedor-uagrm-core` es un proyecto
Maven y `comedor-uagrm-ia` un proyecto Python, ambos independientes que
viven dentro del mismo repo). `nexware-landing` queda
**fuera** del workspace pnpm a propósito: es un proyecto Astro
independiente que usa npm (ver "Nexware guía de marca y guía técnica.md").

## Estructura del repo

```
ComedorUAGRM/
├── projects/
│   ├── backend/
│   │   ├── comedor-uagrm-core/  # API REST (Spring Boot)
│   │   └── comedor-uagrm-ia/    # Módulo de IA (FastAPI)
│   ├── frontend/
│   │   ├── comedoruagrm/     # Panel web (React) -- se sirve en /
│   │   └── nexware-landing/  # Landing de Nexware (Astro) -- se sirve en /landing-page
│   └── mobile/         # App móvil (Flutter)
├── infrastructure/      # Config de nginx, postgres, redis, minio, prometheus, grafana
├── docs/
│   ├── README.md        # Checklist de puesta en marcha de un proyecto nuevo a partir de esta plantilla
│   ├── errors/           # Catálogo de códigos de error de la API (ver más abajo)
│   ├── CasosDeUso/       # Casos de uso (ver .claude/rules/CASOS_DE_USO.md)
│   └── UnidadesTrabajo/  # Unidades de trabajo por caso de uso
├── .claude/
│   ├── CLAUDE.md         # Instrucciones de proyecto para Claude Code
│   └── rules/            # Reglas de arquitectura y convenciones (ver más abajo)
└── docker-compose.yml    # Levanta el stack completo para desarrollo
```

## Puesta en marcha rápida

### Opción A — Stack completo con Docker Compose

Requiere Docker Desktop. Desde la raíz del repo:

```bash
cp .env.example .env
cp projects/backend/comedor-uagrm-core/.env.example projects/backend/comedor-uagrm-core/.env
cp projects/frontend/comedoruagrm/.env.example projects/frontend/comedoruagrm/.env
docker compose up
```

Esto levanta todos los servicios en modo desarrollo (hot-reload incluido
en los dos frontends, `core` e `ia`):

| Servicio | Puerto | URL |
| --- | --- | --- |
| Proxy (nginx) | `80` | http://localhost (`/` → ComedorUAGRM, `/landing-page` → landing Nexware) |
| ComedorUAGRM (Vite) | `5173` | http://localhost:5173 |
| Landing Nexware (Astro) | `4321` | http://localhost:4321 |
| Backend core (API) | `8080` | http://localhost:8080 |
| Backend IA (FastAPI) | `8000` | http://localhost:8000/health (solo interno: no pasa por el proxy) |
| PostgreSQL | `5432` | — |
| Redis | `6379` | — |
| MinIO (API / consola) | `9000` / `9001` | http://localhost:9001 |
| Prometheus | `9090` | http://localhost:9090 |
| Grafana | `3000` | http://localhost:3000 |

Verificación rápida de que el backend core levantó bien:

```bash
curl http://localhost:8080/health        # {"status":"UP", ...}
curl http://localhost:8080/prometheus | head   # métricas en texto plano
```

Swagger UI de la API: http://localhost:8080/swagger-ui/index.html

### Opción B — Cada proyecto por separado (sin Docker)

**Backend core** (`projects/backend/comedor-uagrm-core`, necesita PostgreSQL/Redis/MinIO corriendo
por su cuenta o vía `docker compose up postgres redis minio`):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # correr en modo dev
./mvnw test                                              # tests
./mvnw clean package                                     # build
```

**Backend IA** (`projects/backend/comedor-uagrm-ia`, requiere Python 3.12+):

```bash
python -m venv .venv && source .venv/bin/activate   # en Windows: .venv\Scripts\activate
pip install -r requirements-dev.txt
uvicorn app.main:app --reload     # correr en modo dev (puerto 8000)
pytest                            # tests
ruff check . ; ruff format --check .   # lint y formato
```

**Frontend — ComedorUAGRM** (`projects/frontend/comedoruagrm`):

```bash
pnpm dev       # servidor de desarrollo (Vite, puerto 5173)
pnpm build     # build de producción
pnpm lint      # ESLint
pnpm test      # Vitest
```

**Frontend — Landing Nexware** (`projects/frontend/nexware-landing`, no es
parte del workspace pnpm, usa npm propio):

```bash
npm install
npm run dev       # servidor de desarrollo (Astro, puerto 4321)
npm run build     # build estático de producción
```

**Mobile** (`projects/mobile`):

```bash
flutter pub get     # instalar dependencias
flutter run          # correr en un emulador/dispositivo conectado
flutter test          # tests
flutter analyze       # análisis estático
```

## Arquitectura

Cada proyecto sigue una organización **feature-based**: el código se
agrupa por funcionalidad de negocio (`features/{modulo}/` en los tres
proyectos), no por tipo técnico — así todo lo relacionado a un mismo tema
(ej. "usuarios") vive junto, y agregar un módulo nuevo no obliga a tocar
carpetas transversales compartidas por toda la app.

**Backend** — flujo de una request:

```
Cliente (frontend/mobile)
   │  HTTP + JSON
   ▼
Controller   -- valida el input, delega a UN método de service, envuelve la respuesta
   ▼
Service       -- lógica de negocio, transacciones, logging
   ▼
Repository     -- acceso a datos (Spring Data JPA)
   ▼
PostgreSQL
```

Toda la API responde con el mismo sobre JSON
(`status`/`data`/`message`/`timestamp`/`error`/`meta`), y todo error de
negocio catalogado trae un código fijo (`ERR_{MODULO}_{NUMERO}`) además
del mensaje en español — ver [docs/errors/](docs/errors/README.md) para
el listado completo de códigos que la API puede devolver hoy.

**Frontend** — Zustand para estado de UI, TanStack Query para todo lo que
viene del backend (nunca mezclados), TanStack Router con file-based
routing y un guard único para rutas autenticadas.

**Mobile** — mismo criterio conceptual que el frontend, pero con Riverpod
para ambos casos (datos de API y estado de UI), distinguidos por
convención de nombre (`{feature}ListProvider` vs. `{feature}UiProvider`).

## Documentación y reglas del proyecto

Las decisiones de arquitectura y las convenciones de código **ya están
tomadas** y viven en `.claude/rules/`, organizadas por proyecto
(`backend/`, `frontend/`, `mobile/`) más las transversales a todo el repo
en la raíz de esa carpeta (convenciones de Git, estándares de código
comunes a los tres lenguajes). No son sugerencias — son la fuente de
verdad de cómo se construye este proyecto específico, y priman sobre
cualquier criterio genérico de "buenas prácticas" cuando entran en
conflicto.

Puntos de entrada útiles:

- [`.claude/CLAUDE.md`](.claude/CLAUDE.md) — resumen del proyecto y cómo
  se trabaja acá (incluye las convenciones de commits de Git).
- [`.claude/rules/CONVENCIONES_GIT.md`](.claude/rules/CONVENCIONES_GIT.md) —
  formato de mensajes de commit (`tipo[SCOPE]: descripción`) y ramas.
- [`.claude/rules/ESTANDARES_CODIGO.md`](.claude/rules/ESTANDARES_CODIGO.md) —
  nombrado y comentarios, comunes a Java/TypeScript/Dart.
- [`docs/errors/README.md`](docs/errors/README.md) — catálogo vivo de
  todos los códigos de error que la API puede devolver, con su HTTP, su
  significado y dónde se lanza cada uno.
- [`docs/README.md`](docs/README.md) — checklist para poner en marcha un
  proyecto nuevo a partir de esta misma plantilla (renombrar paquetes,
  slugs, bundle IDs, etc.).

## Convenciones de Git (resumen)

Formato de commit obligatorio, validado automáticamente por `commitlint`
en cada commit:

```
tipo[SCOPE]: descripción concreta
```

- **Tipos**: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `style`,
  `perf`, `build`, `ci` (Conventional Commits).
- **Scopes**: `BACK`, `FRONT`, `MOBILE`, `INFRA`, `DOCS`, `REPO` — un
  commit nunca toca más de un scope a la vez.

Detalle completo en
[`.claude/rules/CONVENCIONES_GIT.md`](.claude/rules/CONVENCIONES_GIT.md).

## CI/CD

Cada Pull Request contra `main` (y cada push directo a `main`) dispara
[`.github/workflows/ci.yml`](.github/workflows/ci.yml): un job
independiente por proyecto (`core`, `ia`, `frontend`, `landing`, `mobile`) que solo
corre si cambió algo dentro de esa carpeta, y que hace lint + tests +
build — los mismos comandos documentados arriba en "Puesta en marcha",
no algo distinto inventado para CI.

Todavía no hay CD (despliegue automático) — depende de una decisión de
infraestructura pendiente (dónde se aloja el backend/frontend en
producción). El detalle completo, incluyendo cómo se vería CD una vez que
exista ese destino, está en
[`.claude/rules/CI_CD.md`](.claude/rules/CI_CD.md).

## Calidad automática (pre-commit)

`husky` corre `lint-staged` en cada `git commit`, que aplica el linter que
corresponde según qué archivos quedaron staged:

| Lenguaje | Herramienta |
| --- | --- |
| TypeScript (frontend) | ESLint (`--fix`) |
| Java (backend core) | Checkstyle |
| Dart (mobile) | `flutter analyze` |

El pre-commit solo lintea — correr la suite de tests de cada proyecto
(`pnpm test`, `./mvnw test`, `flutter test`) antes de commitear queda a
criterio de cada dev.
