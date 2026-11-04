# ComedorUAGRM

Sistema de gestión del comedor universitario de la UAGRM. Monorepo con
pnpm workspaces (`pnpm-workspace.yaml`) que contiene tres grupos de
proyectos independientes más la infraestructura de desarrollo:

- **`projects/backend`** — contiene dos proyectos de backend
  independientes, cada uno con su propio stack:
  - **`comedor-uagrm-core/`** — API REST en Java 21 + Spring Boot 4.1
    (Maven), PostgreSQL + Flyway para persistencia, Redis para caché, MinIO
    para almacenamiento de archivos. Es el backend que consumen el panel web
    y la app mobile, y al que le aplican las rules de `.claude/rules/backend/`.
  - **`comedor-uagrm-ia/`** — módulo de IA en Python + FastAPI (el puntaje de
    vulnerabilidad de la preselección, que consumirá `comedor-uagrm-core` por
    la red interna). Hoy es un esqueleto vacío que solo expone `/health`; no
    tiene rules propias todavía (se definen junto con su primer caso de uso).
- **`projects/frontend`** — contiene dos proyectos de frontend
  independientes, cada uno con su propio stack:
  - **`comedoruagrm/`** — panel web en React 19 + TypeScript + Vite,
    TanStack (Query/Router/Form/Table), Tailwind CSS 4 + shadcn. Es parte
    del workspace pnpm. Se sirve en `/` detrás del proxy.
  - **`nexware-landing/`** — landing institucional de Nexware (la empresa
    ficticia dueña de este proyecto), en Astro + TypeScript, bilingüe
    (es/en). Proyecto independiente fuera del workspace pnpm (usa npm
    propio) — ver "Nexware guía de marca y guía técnica.md" en la raíz.
    Se sirve en `/landing-page` detrás del proxy.
- **`projects/mobile`** — App en Flutter/Dart, Riverpod para estado,
  `go_router` para navegación, `dio` como cliente HTTP.
- **`infrastructure/`** — configuración de Docker Compose: nginx (proxy),
  postgres, redis, minio, prometheus, grafana.

Cuando una regla o un comando dice «backend» a secas, es
`projects/backend/comedor-uagrm-core` (el Spring Boot); el módulo de IA se
nombra siempre como `comedor-uagrm-ia`.

## Fuente de verdad del dominio: documentación de grado

El sistema (**postulación y preselección de la Beca Comedor de la DUBSS,
con IA y reportes BI**) está especificado en la documentación de grado, que
vive **fuera de este repositorio**:

`D:\UAGRM\Taller De Grado - Junior\doc-comedor-univesitario\`

Lo que ahí figura —**diseño lógico de la base de datos**
(`images/091_Diseno_Logico_Base_de_Datos.jpg`), **casos de uso**
(`casos_de_uso_filtrados.md`, 26 CU), diagramas de **análisis**, de
**secuencia** y **mockups** (`images/`)— es lo que el proyecto debe
cumplir. Antes de implementar un caso de uso hay que mirar sus diagramas
(ver [CASOS_DE_USO.md](rules/CASOS_DE_USO.md)); el modelo de datos tiene su
transcripción versionada en `docs/DiagramaConceptual.mermaid` (ver
[PERSISTENCIA_BD_BACKEND.md](rules/backend/PERSISTENCIA_BD_BACKEND.md)). La
autenticación la resuelve el propio backend con **JWT** (token de acceso +
refresh token —en cookies `HttpOnly` en la web, `Bearer` en mobile—, contraseñas con BCrypt; ver
[SEGURIDAD_AUTH_BACKEND.md](rules/backend/SEGURIDAD_AUTH_BACKEND.md)).
Firebase se reserva para las notificaciones push (FCM) de mobile.
Si algo en el código contradice esa documentación, se avisa en vez de
elegir en silencio.

El trabajo se organiza por **casos de uso** (`docs/CasosDeUso/`) y se
descompone en **unidades de trabajo** (`docs/UnidadesTrabajo/`), ver
[CASOS_DE_USO.md](rules/CASOS_DE_USO.md) y
[UNIDADES_DE_TRABAJO.md](rules/UNIDADES_DE_TRABAJO.md).

## Comandos básicos

### Backend core (`projects/backend/comedor-uagrm-core`)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # correr en modo dev
./mvnw test                                              # tests
./mvnw clean package                                     # build
```

### Backend IA (`projects/backend/comedor-uagrm-ia`)

```bash
python -m venv .venv                              # entorno virtual (una vez)
pip install -r requirements-dev.txt               # instalar dependencias
uvicorn app.main:app --reload                     # correr en modo dev (puerto 8000)
pytest                                            # tests
ruff check . ; ruff format --check .              # lint y formato
```

### Frontend — ComedorUAGRM (`projects/frontend/comedoruagrm`)

```bash
pnpm dev       # servidor de desarrollo (Vite, puerto 5173)
pnpm build     # build de producción
pnpm lint      # ESLint
```

### Frontend — Landing de Nexware (`projects/frontend/nexware-landing`)

```bash
npm install
npm run dev          # servidor de desarrollo (Astro, puerto 4321)
npm run build        # build estático de producción
npm run lint         # ESLint
npm run typecheck    # astro check
npm run check:i18n   # es.json y en.json con las mismas claves, por feature
npm test             # Vitest
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
`.env.example` correspondientes (raíz, `projects/backend/comedor-uagrm-core/`,
`projects/frontend/comedoruagrm/`):

```bash
docker compose up
```

Levanta proxy (puerto 80), comedoruagrm (5173), nexware-landing (4321),
core (8080, el backend Spring Boot), ia (8000, FastAPI; no pasa por el
proxy), postgres (5432), redis (6379), minio (9000/9001), prometheus (9090)
y grafana (3000). El proxy sirve ComedorUAGRM en `/`, la API en `/api/` y la
landing de Nexware en `/landing-page`. Ver [docs/README.md](../docs/README.md)
para el detalle completo de variables de entorno y el checklist de puesta
en marcha.

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

## Commits de Git — Claude no commitea el trabajo final, salvo pruebas que deshace

**Claude nunca deja un commit permanente sin que la persona lo haya
pedido.** Decidir cuándo y qué commitear como entrega final del trabajo es
responsabilidad exclusiva de la persona que está usando Claude.

Cuando el trabajo esté listo para commitear, Claude debe:

1. Seguir [CONVENCIONES_GIT.md](rules/CONVENCIONES_GIT.md) al armar el
   mensaje: formato `tipo[SCOPE]: descripción concreta`, y **un commit por
   scope** (si el cambio toca backend y frontend, son dos mensajes
   separados, no uno).
2. Proponerle a la persona el/los mensajes de commit listos para usar
   (idealmente en un bloque de código para copiar/pegar), explicando
   brevemente qué agrupa cada uno si hay más de uno.
3. Dejar que la persona misma corra `git add`/`git commit` — Claude no lo
   ejecuta como entrega final salvo que se lo pidan explícitamente.

**Excepción: probar que un hook de git dispara de verdad.** Claude sí puede
hacer un `git commit` puntual con el único fin de confirmar que
`pre-commit`/`commit-msg` disparan en un commit real (no simulado) —
**siempre y cuando lo deshaga inmediatamente después** con
`git reset --soft HEAD~1` (deja los cambios de vuelta como estaban,
staged, sin perder nada) y avise a la persona que hizo esto. Nunca se deja
como commit definitivo sin que se lo pidan.

Para todo lo demás que no necesite un commit real de por medio (probar que
un mensaje cumple el formato, o que `lint-staged` corre el linter
correcto), usar los mecanismos que ni siquiera tocan el historial de git:

```bash
pnpm exec lint-staged                     # simula el pre-commit sobre lo que esté staged
sh .husky/commit-msg archivo_con_mensaje.txt  # simula el commit-msg sin commitear
echo "feat[BACK]: mensaje de prueba" | pnpm exec commitlint  # valida un mensaje suelto
```
