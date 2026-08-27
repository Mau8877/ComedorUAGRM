# Documentación

Documentación oficial del proyecto.

## Poniendo en marcha un proyecto nuevo a partir de esta plantilla

Este repo es un esqueleto de arquitectura (Spring Boot + React/Vite + Flutter + Docker). Todos los identificadores están puestos como placeholder genérico (`app`, `com.example`, etc.) para que se reemplacen por el nombre real del proyecto nuevo. Sigue esta lista en orden.

### 1. Elegí el nombre del proyecto

Antes de tocar nada, definí:

- **Nombre del proyecto** (ej: `Acme`) → se usa en textos y README.
- **Slug en minúsculas con guiones** (ej: `acme`) → se usa en nombres de contenedores, red, artefactos.
- **Paquete reverso de dominio** (ej: `com.acme`) → se usa en Java (backend) y en bundle IDs (mobile).

En los ejemplos de abajo se usa `acme` / `com.acme` como referencia.

### 2. Raíz del repo

- [ ] `README.md`: cambiá el título y la descripción.
- [ ] `package.json`: cambiá `"name": "developlayout"` por el slug del proyecto.
- [ ] `docker-compose.yml`: reemplazá todas las ocurrencias de `app` por el slug elegido:
  - `name: app` (nombre del stack de compose)
  - `container_name: app-*` (proxy, frontend, backend, postgres, redis, minio, minio-init, prometheus, grafana)
  - `networks: app-network`
- [ ] `.env.example` (raíz): actualizá `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` con el nuevo slug, y generá tu propio `.env` real (no se commitea).

### 3. Backend (Spring Boot)

- [ ] Renombrá el paquete Java `com.example.backend` → `com.acme.backend` en:
  - `projects/backend/src/main/java/com/example/...` (mover carpetas)
  - `projects/backend/src/test/java/com/example/...` (mover carpetas)
  - El `package` / `import` dentro de cada `.java`
- [ ] Renombrá la clase principal `AppBackendApplication` (y su test) por el nombre que prefieras (ej: `AcmeBackendApplication`).
- [ ] `pom.xml`: `groupId` (`com.example` → `com.acme`) y `artifactId` (`app-backend` → `acme-backend`).
- [ ] `application.properties`:
  - `spring.application.name`
  - URL de datasource (`app_db` → tu nombre de base de datos)
  - namespace `app.minio.*` (y actualizá el prefijo si cambiás el nombre)
  - `app.minio.bucket-name`
- [ ] `projects/backend/.env.example`: mismo ajuste de `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.
- [ ] `src/main/resources/db/migration/V1__init_schema.sql`: cambiá el valor `'App'` de `project_name` (o borrá esa migración de ejemplo cuando tengas tu esquema real).

### 4. Frontend (React + Vite)

- [ ] `projects/frontend/package.json`: `"name": "frontend"` → el nombre que quieras.
- [ ] `projects/frontend/index.html`: `<title>frontend</title>`.
- [ ] `projects/frontend/src/App.tsx`: es la pantalla de bienvenida del boilerplate — reemplazala por tu primera vista real.
- [ ] `projects/frontend/.env.example`: revisá `VITE_API_BASE_URL` si cambiás rutas/puertos.

### 5. Mobile (Flutter)

- [ ] `projects/mobile/pubspec.yaml`: `name: mobile` y `description`.
- [ ] Bundle ID / application ID `com.example.mobile` → `com.acme.mobile` en:
  - `android/app/build.gradle.kts` (`namespace`, `applicationId`)
  - `android/app/src/main/kotlin/com/example/mobile/MainActivity.kt` (mover el paquete y actualizar el `package` declarado)
  - `ios/Runner.xcodeproj/project.pbxproj` (`PRODUCT_BUNDLE_IDENTIFIER`, todas las ocurrencias)
  - `macos/Runner.xcodeproj/project.pbxproj` y `macos/Runner/Configs/AppInfo.xcconfig` (`PRODUCT_BUNDLE_IDENTIFIER`, `PRODUCT_NAME`, `PRODUCT_COPYRIGHT`)
  - `linux/CMakeLists.txt` (`APPLICATION_ID`)
  - `windows/runner/Runner.rc` (`CompanyName`, `LegalCopyright`)
- [ ] Íconos y splash (`android/.../mipmap-*`, `ios/Runner/Assets.xcassets`, `macos/Runner/Assets.xcassets`, `web/icons`, `windows/runner/resources`): reemplazalos por los del proyecto real.
- [ ] `lib/core/` y `lib/features/auth/` están vacíos a propósito (solo `.gitkeep`) — son la estructura base para arrancar.

> Nota: después de renombrar el bundle ID, corré `flutter pub get` y, si usás Android Studio/Xcode, reabrí el proyecto para que regenere los archivos `ephemeral`/`Generated.xcconfig` (se excluyen del repo a propósito, son generados).

### 6. Infraestructura Docker

- [ ] `infrastructure/nginx/nginx.conf`: `upstream` apunta a `app-frontend` / `app-backend` — actualizá si cambiaste esos nombres de contenedor.
- [ ] `infrastructure/prometheus/prometheus.yml`: `job_name` y `targets: ['app-backend:8080']`.
- [ ] `infrastructure/grafana/provisioning/datasources/datasource.yml`: `url: http://app-prometheus:9090`.
- [ ] `infrastructure/minio/create-buckets.sh`: nombre del bucket `app-media`.

### 7. Verificación final

Buscá que no quede ningún rastro del placeholder:

```bash
grep -ril "com\.example\|app-network\|app_db\|app-backend\|app-frontend\|app-storage\|app-media" . --exclude-dir=.git
```

Si el comando no devuelve nada, ya no queda nada del template. Después corré:

```bash
cp .env.example .env
docker compose up
```

para confirmar que el stack completo (proxy, frontend, backend, postgres, redis, minio, prometheus, grafana) levanta correctamente con los nuevos nombres.
