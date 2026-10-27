# Landing page de la empresa

> Estado: **idea / pendiente de implementar**. Nada de esto existe todavía en el repo.

## Contexto

Se necesita una landing page de la "empresa" que desarrolla ComedorUAGRM.
Es una **demo para la presentación de una materia**: no hay datos legales ni
empresariales reales, todo el contenido es de ejemplo (y debe quedar marcado
como tal, especialmente las secciones legales).

Contenido previsto:

- Presentación de la empresa (nosotros).
- Aspectos legales: política de privacidad, términos y condiciones.
- Preguntas frecuentes.
- Contacto.
- Una sección que presente el trabajo de **ComedorUAGRM** y enlace al panel.

## Decisión de arquitectura: opción A

Se evaluaron cuatro opciones para integrarla al monorepo:

| Opción | Descripción | Veredicto |
| --- | --- | --- |
| **A** | Nuevo proyecto `projects/landing`, independiente | **Elegida** |
| B | Rutas públicas dentro de `projects/frontend` | Descartada: SPA con peor SEO y primera carga, mezcla marketing con el panel |
| C | Microfrontend (Module Federation / single-spa) | Descartada: complejidad excesiva, no resuelve nada que A no resuelva |
| D | Repositorio separado | Descartada: se pierde el CI y las convenciones unificadas |

Razones de A: es el mismo patrón que backend, frontend y mobile (cada uno con
su build y su job de CI); `pnpm-workspace.yaml` ya cubre `projects/*`; el
bundle del panel no se mezcla con contenido de marketing; y la landing se
despliega sin tocar la app.

## Decisión de herramienta: Astro + Tailwind 4 (recomendada, a confirmar)

| | Astro | Vite + React | Next.js |
| --- | --- | --- | --- |
| Curva de aprendizaje | Baja | Ninguna (ya se usa en `frontend`) | Media |
| Páginas de contenido (legales, FAQ) | Muy cómodo, admite Markdown | Router a mano o con TanStack | Cómodo |
| Rendimiento y SEO | Excelente (HTML estático) | Peor (render en cliente) | Excelente |
| Reusar Tailwind 4 / paleta | Tailwind sí; shadcn solo con React dentro | Todo directo | Todo directo |
| Complejidad | Mínima | Mínima | Excesiva para un sitio estático |

Se recomienda **Astro + Tailwind 4**: el sitio es casi todo contenido, las
páginas legales y la FAQ se pueden escribir en Markdown, y es fácil de
defender en la presentación (sitio estático, liviano y con buen SEO). La
alternativa es Vite + React si se prefiere no sumar ninguna herramienta nueva
y reusar shadcn tal cual.

## Estructura propuesta

```
projects/landing/
├── src/
│   ├── layouts/        # layout común: header, footer
│   ├── components/     # hero, secciones, tarjetas
│   ├── pages/
│   │   ├── index.astro
│   │   ├── nosotros.astro
│   │   ├── proyectos/comedor-uagrm.astro   # presenta el sistema y enlaza al panel
│   │   ├── preguntas-frecuentes.astro
│   │   ├── privacidad.md
│   │   ├── terminos.md
│   │   └── contacto.astro
│   └── styles/         # tokens de marca
└── astro.config.mjs
```

## Trabajo que implica en el monorepo

1. Crear `projects/landing` con el scaffolding de Astro + Tailwind 4.
2. Agregar un job `landing` en `.github/workflows/ci.yml` con su filtro de
   paths (lint/build, igual que el resto).
3. Agregar el scope `LANDING` en `commitlint.config.js` y en
   [CONVENCIONES_GIT.md](../../.claude/rules/CONVENCIONES_GIT.md) (hoy solo
   existen `BACK`, `FRONT`, `MOBILE`, `INFRA`, `DOCS`, `REPO`).
4. Agregar el servicio a `docker-compose.yml` y una regla en nginx:
   `/` → landing, `/app` → panel (hoy nginx manda `/*` al frontend).
5. Escribir una regla `.claude/rules/ARQUITECTURA_LANDING.md` y enlazarla
   desde `CLAUDE.md`.
6. Actualizar `CLAUDE.md` y la documentación de infraestructura con el nuevo
   proyecto.

## Preguntas abiertas

- **Nombre y marca de la empresa**: ¿tiene nombre y colores propios, o se
  reutiliza la paleta de ComedorUAGRM?
- **Acceso al panel**: ¿`/` para la landing y `/app` para el panel, o un
  subdominio? En local con nginx, `/app` es lo más simple.
- **Idioma**: se asume español.
- **Herramienta**: confirmar Astro frente a Vite + React.
