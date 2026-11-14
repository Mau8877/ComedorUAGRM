---
globs: projects/frontend/nexware-landing/**/*
---

# Arquitectura Landing (Nexware, Astro)

Mismo criterio que [ARQUITECTURA_FRONTEND.md](ARQUITECTURA_FRONTEND.md):
**feature-based**. Cada sección o pantalla de la landing es una feature
(`features/{feature}/`); lo transversal vive en carpetas de nivel superior.
Es un proyecto independiente (npm propio, fuera del workspace pnpm, se sirve
en `/landing-page`) — ver `CLAUDE.md`.

## Estructura de `src/`

```
src/
├── pages/              # Solo ruteo: cada archivo monta UNA pantalla, sin lógica
├── layouts/            # BaseLayout (head, SEO, tema, header/footer)
├── features/
│   └── {feature}/
│       ├── components/ # Componentes de ESTA feature (.astro)
│       ├── screens/    # Páginas completas que se montan desde pages/
│       ├── lib/        # Lógica pura de la feature (testeable, sin Astro)
│       ├── tests/      # Tests de la feature
│       └── index.ts    # Barrel: lo único que otras partes pueden importar
├── components/ui/      # Primitivas compartidas (Button, Section, Logo, ...)
├── i18n/               # Núcleo (utils.ts, routes.ts) + diccionarios por feature
│   └── {Feature}/      # es.json + en.json de esa feature
├── config/site.ts      # Datos del sitio usados por varias features
├── lib/                # Utilidades transversales (seo.ts)
├── content/            # Markdown de las páginas legales (colección `legal`)
└── styles/             # tokens.css (paleta) y global.css
```

- Alias **`@/`** = `src/` (en `tsconfig.json`). Se importa siempre con alias,
  nunca con `../../..`.
- Una carpeta de feature solo se crea cuando hay algo que poner (mismo
  criterio de "no anticipar" del panel React).

## Regla de dependencia

`features/{feature}/` puede importar de `components/ui/`, `i18n/`, `config/`,
`lib/` y `layouts/` — nunca al revés. Una feature no importa los archivos
internos de otra: usa su barrel (`@/features/hero`), igual que en el panel.
`pages/` solo importa barrels de features.

## i18n: diccionarios por feature

- Los textos viven en `src/i18n/{Feature}/es.json` y `en.json`, **nunca**
  escritos dentro de un componente, y **no** en un JSON único.
- El nombre de la carpeta (`Hero`) es el namespace (`hero.title`); los JSON
  pueden anidar y se aplanan con punto (`services.web.title`).
- Las dos lenguas llevan exactamente las mismas claves: lo valida
  `npm run check:i18n` y `src/i18n/dictionaries.test.ts`.
- Los enlaces internos pasan por `withBase()`/`homePath()`/`legalPath()`
  (`i18n/routes.ts`): el sitio vive bajo `/landing-page`.

## Estilos

Todo color sale de los tokens de `styles/tokens.css` (claro/oscuro); nada de
hex sueltos en un componente. Estilos con `<style>` dentro de cada `.astro`.
Sin librerías de UI ni JavaScript de framework: solo scripts pequeños
(tema, menú, validación del formulario).

## Tests, lint y formato

- Vitest (`npm test`): lógica pura y contenido (i18n, rutas, SEO, validación,
  legales). Código transversal: junto al archivo (`routes.ts` +
  `routes.test.ts`); código de una feature: en su carpeta `tests/`.
- ESLint (`npm run lint`), Prettier (`npm run format:check`) y
  `npm run typecheck` (`astro check`). CI corre todo en el job `landing`.

## Docker y `astro dev`

Detrás del proxy (`http://localhost/landing-page/`) el contenedor sirve el
**build** (`astro build` + `astro preview`): el dev server de Astro emite sus
assets desde la raíz (`/src/...`, `/@vite/...`) y nginx los manda al panel
(404). Para desarrollar con recarga en caliente se corre `npm run dev` en el
host y se abre `http://localhost:4321/landing-page/` (con el contenedor
parado, comparten puerto).
