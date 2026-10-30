# Nexware: guía de marca y guía técnica

Oct 27, 2026 · @Mau

## Resumen

Nexware es una empresa ficticia de desarrollo web y móvil a medida, de Santa Cruz de la Sierra (Bolivia), creada para la materia Ingeniería de Software 2. Este documento fija la marca, el diseño y la arquitectura de su landing en Astro para que Claude Code la construya.

Qué construir: una landing bilingüe (español e inglés) con modo claro y oscuro, más cuatro páginas legales. La referencia visual son los tres mockups del lienzo de diseño (escritorio claro, escritorio oscuro y móvil).

Cómo usarlo: guardar este documento como `SPEC.md` (o `CLAUDE.md`) en la raíz del proyecto y pasarle a Claude Code los mockups junto con el prompt de la última sección.

| Decisión | Valor |
| --- | --- |
| Nombre | Nexware |
| Eslogan principal | Si no existe, lo programamos. / If it doesn't exist, we build it. |
| Frase de apoyo | Tu idea, plasmada. / Your idea, made real. |
| Logo | N de píxeles en cuadrícula 5×5, píxel central en color de acento |
| Paleta | Pizarra y teal, en claro y oscuro |
| Tipografías | Space Grotesk y JetBrains Mono |
| Framework | Astro, salida estática, TypeScript estricto |
| Idiomas | Español (por defecto) e inglés, con i18n nativo de Astro |
| Hosting | Local; opcional AWS (S3 + CloudFront) |

## Marca

Nexware viene de *nexo* + *software*: conectamos una idea con el software exacto que necesita. El logotipo siempre va en minúscula (`nexware`); en texto corrido se escribe «Nexware».

La voz es directa y atrevida: frases cortas, tuteo, técnica sin jerga innecesaria. Ejemplo: «Si no existe, lo programamos.»

### Logo

Es una **N hecha de píxeles** sobre una cuadrícula de 5×5. Las columnas izquierda y derecha van completas, la diagonal ocupa las celdas (1,1), (2,2) y (3,3), y el píxel central (2,2) va en color de acento: es el punto de conexión.

- Celdas de 12 unidades con separación de 3 y esquinas de 2,5 (viewBox 72×72).
- Variante horizontal: la N a la izquierda y «nexware» a la derecha, en Space Grotesk 600 con espaciado de letras −0,03 em.
- Variante ícono (favicon, app): cuadrado de esquinas redondeadas (rx 20 sobre 88) con los píxeles dentro. En claro el cuadrado es del color de texto; en oscuro, al revés.
- Espacio libre alrededor: dos celdas. Tamaño mínimo: 16 px.
- No deformar, no cambiar el color del píxel central, no usar sobre fondos de bajo contraste.

SVG base del logo (la N usa `currentColor` y el píxel central usa la variable de acento):

```html
<svg width="80" height="80" viewBox="0 0 72 72" role="img" aria-label="Nexware">
  <g fill="currentColor">
    <rect x="0" y="0" width="12" height="12" rx="2.5"/>
    <rect x="0" y="15" width="12" height="12" rx="2.5"/>
    <rect x="0" y="30" width="12" height="12" rx="2.5"/>
    <rect x="0" y="45" width="12" height="12" rx="2.5"/>
    <rect x="0" y="60" width="12" height="12" rx="2.5"/>
    <rect x="60" y="0" width="12" height="12" rx="2.5"/>
    <rect x="60" y="15" width="12" height="12" rx="2.5"/>
    <rect x="60" y="30" width="12" height="12" rx="2.5"/>
    <rect x="60" y="45" width="12" height="12" rx="2.5"/>
    <rect x="60" y="60" width="12" height="12" rx="2.5"/>
    <rect x="15" y="15" width="12" height="12" rx="2.5"/>
    <rect x="45" y="45" width="12" height="12" rx="2.5"/>
  </g>
  <rect x="30" y="30" width="12" height="12" rx="2.5" style="fill: var(--accent)"/>
</svg>
```

## Paleta Pizarra y teal

Sobria, sin colores chillones: grises azulados con un solo acento verde azulado. Hay un valor por token en modo claro y otro en oscuro; el acento se aclara en oscuro para mantener el contraste.

| Token | Uso | Claro | Oscuro |
| --- | --- | --- | --- |
| `--bg` | Fondo de página | `#F2F5F6` | `#11181D` |
| `--surface` | Tarjetas, pie, campos | `#FFFFFF` | `#18222A` |
| `--ink` | Texto principal | `#1E2A33` | `#E9EFF1` |
| `--soft` | Texto secundario | `#54616B` | `#96A4AD` |
| `--border` | Bordes de 1 px | `#D3DBDF` | `#26323A` |
| `--accent` | Enlaces, etiquetas, botón principal | `#266B66` | `#5BB5AC` |
| `--accent-ink` | Texto sobre el acento | `#F2F5F6` | `#11181D` |
| `--tint` | Fondos suaves teñidos (íconos, panel de contacto) | `#E1EDEC` | `#16302F` |

Los pares de texto fueron elegidos para superar 4,5:1 de contraste. Antes de entregar, verifiquen cada par con una herramienta de contraste.

Tokens listos para `src/styles/tokens.css`:

```css
:root {
  --bg: #F2F5F6;
  --surface: #FFFFFF;
  --ink: #1E2A33;
  --soft: #54616B;
  --border: #D3DBDF;
  --accent: #266B66;
  --accent-ink: #F2F5F6;
  --tint: #E1EDEC;
}

:root[data-theme="dark"] {
  --bg: #11181D;
  --surface: #18222A;
  --ink: #E9EFF1;
  --soft: #96A4AD;
  --border: #26323A;
  --accent: #5BB5AC;
  --accent-ink: #11181D;
  --tint: #16302F;
}
```

### Modo claro y oscuro

1. Sin preferencia guardada, seguir `prefers-color-scheme` del sistema.
2. Un botón (`ThemeToggle`) alterna el tema y guarda la elección en `localStorage` dentro de `try/catch`.
3. Un script pequeño en línea dentro de `<head>` pone `data-theme` en `<html>` antes de pintar, para evitar el parpadeo.
4. Todos los colores de los componentes salen de los tokens; nada de hex sueltos.

## Tipografía, espaciado y componentes

Space Grotesk (pesos 400, 500, 600 y 700) para títulos y texto; JetBrains Mono (400 y 500) para etiquetas y detalles técnicos. Cárguenlas con `@fontsource` (autoalojadas) y `font-display: swap`, no desde Google Fonts: es más rápido y no comparte datos del visitante con terceros.

| Elemento | Tamaño | Interlineado | Espaciado de letras | Peso |
| --- | --- | --- | --- | --- |
| H1 | `clamp(44px, 7vw, 88px)` | 1,02 | −0,04 em | 700 |
| Frase de apoyo (hero) | `clamp(22px, 2.6vw, 32px)` | 1,2 | −0,02 em | 500, color acento |
| H2 | `clamp(32px, 4.4vw, 52px)` | 1,08 | −0,03 em | 600 |
| H3 | 22 a 26 px | 1,2 | −0,02 em | 600 |
| Texto | 16 a 18 px | 1,55 a 1,6 | normal | 400 |
| Etiqueta (eyebrow) | 12 px, JetBrains Mono, mayúsculas | normal | 0,1 em | 400, color acento |

**Medidas**

- Contenedor: ancho máximo 1200 px, centrado.
- Padding horizontal de sección: `clamp(20px, 5vw, 80px)`. Vertical: `clamp(56px, 7vw, 96px)`.
- Separación entre tarjetas: 20 px. Entre bloques de una sección: 40 a 48 px.
- Radios: tarjetas 16 px, botones y campos 12 px, panel de contacto 24 px, etiquetas 999 px.
- Botones y campos: altura mínima de 48 px (el mínimo táctil es 44 px). Borde de 1 px con `--border`.
- Diseño fluido: filas con `flex-wrap`, y tarjetas con `flex: 1 1 300px` (servicios) o `1 1 440px` (stack). Sin saltos de diseño por breakpoint fijos, salvo que haga falta.

**Componentes**

| Componente | Qué hace |
| --- | --- |
| `Logo` | N de píxeles y «nexware», variantes horizontal e ícono |
| `Header` | Logo, navegación por anclas, selector de idioma, `ThemeToggle` y botón «Hablemos» |
| `Hero` | Título, frase de apoyo, texto, dos botones y `PixelN` |
| `PixelN` | Cuadrícula de 5×5 decorativa (`aria-hidden`) con el patrón de la N |
| `ServiceCard` | Ícono de trazo, título, texto y etiqueta con tecnologías |
| `StackCard` | Categoría en mono, tecnología y por qué la usamos |
| `ProjectCard` | Imagen, etiqueta «Proyecto demo», título, descripción, tecnologías y enlace |
| `TeamCard` | Inicial, nombre y rol |
| `ContactForm` | Nombre, correo y mensaje, con `<label>` reales |
| `Footer` | Marca, enlaces de empresa y legales, aviso de proyecto académico |
| `LanguageSwitcher` | Cambia a la misma página en el otro idioma |

## Stack y estructura del proyecto

La landing es un sitio estático hecho solo con Astro: HTML y CSS, con JavaScript mínimo (tema e idioma). React, Spring Boot, Flutter y FastAPI son el stack de la empresa que se presenta en la web, no dependencias de este proyecto.

| Pieza | Decisión |
| --- | --- |
| Framework | Astro, `output: 'static'`, TypeScript con `strict` |
| Estilos | CSS con variables (`tokens.css` y `global.css`) y estilos dentro de cada componente `.astro` |
| Fuentes | `@fontsource/space-grotesk` y `@fontsource/jetbrains-mono` |
| Contenido legal | Colecciones de contenido de Astro, un Markdown por página e idioma |
| Sitemap | `@astrojs/sitemap` con la opción `i18n` |
| Node | 20 LTS o superior |

Estructura de carpetas:

```text
nexware-landing/
├─ astro.config.mjs
├─ package.json
├─ tsconfig.json
├─ SPEC.md
├─ public/
│  ├─ favicon.svg
│  ├─ apple-touch-icon.png
│  ├─ og-image.png
│  └─ robots.txt
└─ src/
   ├─ components/        Logo, Header, Hero, PixelN, Services, ServiceCard,
   │                     Stack, StackCard, Portfolio, ProjectCard, Team,
   │                     TeamCard, Contact, Footer, ThemeToggle, LanguageSwitcher
   ├─ content/
   │  ├─ config.ts       colección "legal" (título, slug, idioma, actualizado)
   │  └─ legal/
   │     ├─ es/          terminos.md, privacidad.md, cookies.md, aviso-legal.md
   │     └─ en/          terms.md, privacy.md, cookies.md, legal-notice.md
   ├─ i18n/
   │  ├─ es.json         textos de la interfaz en español
   │  ├─ en.json         textos de la interfaz en inglés
   │  ├─ routes.ts       mapa de rutas por idioma (para el selector y los enlaces)
   │  └─ utils.ts        useTranslations(lang), getLangFromUrl(url)
   ├─ layouts/
   │  ├─ BaseLayout.astro   <html lang>, SEO, hreflang, fuentes, script del tema
   │  └─ LegalLayout.astro  índice lateral y contenido del Markdown
   ├─ pages/
   │  ├─ index.astro        redirige a /es/
   │  ├─ es/index.astro y es/[legal].astro
   │  ├─ en/index.astro y en/[legal].astro
   │  └─ 404.astro          bilingüe
   └─ styles/
      ├─ tokens.css
      └─ global.css
```

## i18n: español e inglés

Se usa el enrutado de idiomas integrado en Astro: español por defecto, con prefijo de idioma en todas las rutas (`/es/` y `/en/`), y los textos en diccionarios JSON.

`astro.config.mjs`:

```js
import { defineConfig } from 'astro/config';
import sitemap from '@astrojs/sitemap';

export default defineConfig({
  site: 'https://nexware.example',
  output: 'static',
  i18n: {
    defaultLocale: 'es',
    locales: ['es', 'en'],
    routing: { prefixDefaultLocale: true },
  },
  integrations: [
    sitemap({ i18n: { defaultLocale: 'es', locales: { es: 'es-BO', en: 'en-US' } } }),
  ],
});
```

`src/i18n/utils.ts`:

```ts
import es from './es.json';
import en from './en.json';

const dictionaries = { es, en } as const;
export type Lang = keyof typeof dictionaries;

export function getLangFromUrl(url: URL): Lang {
  const [, lang] = url.pathname.split('/');
  return lang in dictionaries ? (lang as Lang) : 'es';
}

export function useTranslations(lang: Lang) {
  return (key: keyof typeof es): string => dictionaries[lang][key] ?? dictionaries.es[key];
}
```

**Reglas**

1. Ningún texto visible va escrito dentro de un componente: todo sale de `es.json` y `en.json` con la misma lista de claves. Un script de comprobación (`npm run check:i18n`) debe fallar si falta una clave en algún idioma.
2. Cada página declara `<html lang="es">` o `<html lang="en">`, y enlaces `<link rel="alternate" hreflang>` hacia el otro idioma y hacia `x-default` (español).
3. `LanguageSwitcher` lleva a la misma página en el otro idioma. Las páginas legales cambian de dirección entre idiomas, así que se resuelven con `routes.ts`.
4. `src/pages/index.astro` redirige a `/es/`. No se detecta el idioma del navegador de forma automática en esta primera versión.
5. Las fechas y los números se formatean con `Intl` según el idioma.
6. Verificar las opciones exactas de `i18n` y `sitemap` contra la versión de Astro instalada: pueden variar entre versiones.

Rutas:

| Página | Español | Inglés |
| --- | --- | --- |
| Inicio | `/es/` | `/en/` |
| Términos y condiciones | `/es/terminos/` | `/en/terms/` |
| Política de privacidad | `/es/privacidad/` | `/en/privacy/` |
| Política de cookies | `/es/cookies/` | `/en/cookies/` |
| Aviso legal | `/es/aviso-legal/` | `/en/legal-notice/` |

## Páginas, secciones y textos

La landing es una sola página con secciones ancladas, en este orden: `#inicio`, `#servicios`, `#stack`, `#portafolio`, `#nosotros` y `#contacto`, más el pie. Cada sección va dentro de un contenedor de 1200 px; las filas pasan a columnas solas cuando falta ancho. El diseño exacto está en los mockups.

Estos son los textos para `es.json` y `en.json`. Las claves en `[corchetes]` son contenido pendiente de rellenar.

| Clave | Español | English |
| --- | --- | --- |
| `nav.services` | Servicios | Services |
| `nav.stack` | Stack | Stack |
| `nav.portfolio` | Portafolio | Portfolio |
| `nav.about` | Nosotros | About |
| `nav.contact` | Contacto | Contact |
| `nav.cta` | Hablemos | Let's talk |
| `hero.eyebrow` | WEB · MÓVIL · A MEDIDA — SANTA CRUZ, BOLIVIA | WEB · MOBILE · CUSTOM-BUILT — SANTA CRUZ, BOLIVIA |
| `hero.title` | Si no existe, lo programamos. | If it doesn't exist, we build it. |
| `hero.tagline` | Tu idea, plasmada. | Your idea, made real. |
| `hero.text` | Diseñamos y desarrollamos sitios web y aplicaciones móviles a medida. Sin plantillas: cada línea de código responde a tu problema. | We design and build custom websites and mobile apps. No templates: every line of code answers your problem. |
| `hero.cta1` | Cuéntanos tu idea | Tell us your idea |
| `hero.cta2` | Ver proyecto demo | See demo project |
| `services.eyebrow` | SERVICIOS | SERVICES |
| `services.title` | A medida, de punta a punta. | Built to fit, end to end. |
| `services.web.title` | Web a medida | Custom web |
| `services.web.text` | Sitios y aplicaciones web con React y TypeScript: rápidas, accesibles y hechas para crecer contigo. | Websites and web apps with React and TypeScript: fast, accessible and built to grow with you. |
| `services.mobile.title` | Apps móviles | Mobile apps |
| `services.mobile.text` | Una sola base de código para Android e iOS con Flutter, con la experiencia que tu usuario espera. | A single codebase for Android and iOS with Flutter, with the experience your users expect. |
| `services.backend.title` | Backend e IA | Backend and AI |
| `services.backend.text` | APIs sólidas con Spring Boot y servicios de inteligencia artificial con FastAPI, integrados a lo que ya tienes. | Solid APIs with Spring Boot and AI services with FastAPI, integrated with what you already have. |
| `stack.eyebrow` | STACK | STACK |
| `stack.title` | Nuestro stack, y por qué. | Our stack, and why. |
| `stack.frontend.text` | Componentes reutilizables y tipado estático: menos errores y código que otros pueden mantener. | Reusable components and static typing: fewer bugs and code others can maintain. |
| `stack.backend.text` | Un ecosistema maduro y seguro, probado en producción, para sistemas que no pueden fallar. | A mature, secure ecosystem proven in production, for systems that can't fail. |
| `stack.mobile.text` | Una base de código y rendimiento nativo en Android e iOS: menos costo y tiempos más cortos. | One codebase and native-level performance on Android and iOS: lower cost, shorter timelines. |
| `stack.ai.text` | Python para modelos y automatización, con APIs rápidas y documentación generada automáticamente. | Python for models and automation, with fast APIs and automatically generated docs. |
| `portfolio.eyebrow` | PORTAFOLIO | PORTFOLIO |
| `portfolio.title` | Lo que construimos. | What we build. |
| `portfolio.label` | PROYECTO DEMO | DEMO PROJECT |
| `portfolio.name` | ComedorUAGRM | ComedorUAGRM |
| `[portfolio.description]` | \[Qué problema resuelve, para quién y qué resultado logra\] | \[What problem it solves, for whom and with what result\] |
| `[portfolio.tags]` | \[Tecnologías usadas\] | \[Technologies used\] |
| `[portfolio.cta]` | \[Ver demo\] | \[View demo\] |
| `about.eyebrow` | NOSOTROS | ABOUT |
| `about.title` | Dos socios, una forma de trabajar. | Two partners, one way of working. |
| `about.text1` | Somos desarrolladores full stack en Santa Cruz de la Sierra, Bolivia. Trabajamos de cerca contigo, de la idea al despliegue. | We're full stack developers based in Santa Cruz de la Sierra, Bolivia. We work closely with you, from idea to deployment. |
| `about.text2` | ¿Por qué Nexware? Nexo + software. El logo, hecho de píxeles, representa cada pieza encajando en su lugar. | Why Nexware? Nexus + software. The pixel logo stands for every piece fitting into place. |
| `about.role` | CEO · Full Stack Developer | CEO · Full Stack Developer |
| `contact.eyebrow` | CONTACTO | CONTACT |
| `contact.title` | Cuéntanos tu idea. | Tell us your idea. |
| `contact.text` | Escríbenos y te respondemos pronto. Sin compromiso. | Write to us and we'll get back to you soon. No strings attached. |
| `contact.name` | Nombre | Name |
| `contact.email` | Correo | Email |
| `contact.message` | ¿Qué quieres construir? | What do you want to build? |
| `contact.send` | Enviar mensaje | Send message |
| `footer.slogan` | Si no existe, lo programamos. | If it doesn't exist, we build it. |
| `footer.company` | EMPRESA | COMPANY |
| `footer.legal` | LEGAL | LEGAL |
| `footer.disclaimer` | © 2026 Nexware, Santa Cruz de la Sierra, Bolivia. Proyecto académico ficticio de Ingeniería de Software 2: cualquier parecido con una empresa real es coincidencia. | © 2026 Nexware, Santa Cruz de la Sierra, Bolivia. Fictional academic project for Software Engineering 2: any resemblance to a real company is coincidental. |

**Datos del equipo**

- Mauro `[Apellido]` y Leonel `[Apellido]`: socios, ambos CEO y desarrolladores full stack. Cada tarjeta muestra la inicial en un cuadrado de color `--tint`.
- Correo de contacto de ejemplo: `contacto@nexware.example` (el dominio `.example` está reservado y no existe).

**Comportamiento**

- El formulario valida en el navegador (campos obligatorios y formato de correo) y muestra un mensaje de éxito. Esta versión no envía datos a ningún servidor.
- El píxel central de `PixelN` puede tener una animación suave de parpadeo; debe respetar `prefers-reduced-motion`.

## Páginas legales

Son cuatro páginas en español y en inglés, escritas como documentos profesionales completos pero marcadas como ejemplo. Cada una lleva arriba un aviso: «Documento de ejemplo de un proyecto académico ficticio; no constituye asesoría legal.» (y su versión en inglés). Claude Code redacta los textos completos a partir de este esquema y los guarda como Markdown en `src/content/legal/`.

Cada archivo lleva este encabezado:

```yaml
---
title: Términos y condiciones
slug: terminos
lang: es
updated: 2026-10-27
---
```

| Página | Contenido mínimo |
| --- | --- |
| Términos y condiciones | 1. Aceptación. 2. Descripción de los servicios. 3. Cotizaciones, plazos y pagos. 4. Propiedad intelectual del código entregado. 5. Uso aceptable del sitio. 6. Limitación de responsabilidad. 7. Cambios en los términos. 8. Ley aplicable (Bolivia) y jurisdicción (Santa Cruz de la Sierra). 9. Contacto. |
| Política de privacidad | Qué datos se recogen (nombre, correo y mensaje del formulario) y para qué. Base y conservación. Derechos de acceso, rectificación y eliminación. Con quién se comparten (proveedor de hosting, si se despliega en AWS). Seguridad. Menores. Cambios. Contacto. |
| Política de cookies | Qué es una cookie. Qué usa el sitio: solo almacenamiento local esencial para guardar el tema y el idioma, sin analítica ni publicidad. Cómo borrarlo. Si algún día se agrega analítica, habrá que añadir un banner de consentimiento. |
| Aviso legal | Titular: Nexware (proyecto académico ficticio), con Mauro y Leonel como socios. Domicilio: Santa Cruz de la Sierra, Bolivia. Contacto de ejemplo. Registro mercantil e identificación tributaria: no aplican por ser una empresa ficticia. Propiedad del sitio. Enlaces externos. |

**Notas de redacción**

- Escribir en lenguaje claro: frases cortas y sin remitirse a leyes que no se puedan citar bien.
- Como referencia para el contexto boliviano, revisar en fuentes oficiales la Constitución Política del Estado (el artículo sobre la acción de protección de privacidad) y la Ley 164 de Telecomunicaciones y TIC. Verificar el texto vigente antes de citarlas.
- Si el sitio se publicara de verdad, habría que revisarlo con un abogado y registrar la empresa.
- Maquetación: `LegalLayout` con un índice a la izquierda (se oculta en móvil), título, fecha de actualización y el texto en una columna de máximo 720 px.

## SEO, accesibilidad y rendimiento

**SEO**

- Un `<title>` y una `meta description` propios por página e idioma, con claves en los diccionarios.
- Etiquetas Open Graph y Twitter con `og-image.png` (1200×630, la N de píxeles y el eslogan).
- `hreflang` entre idiomas, `canonical` en cada página, `sitemap-index.xml` y `robots.txt`.
- Datos estructurados JSON-LD de tipo `Organization` en el inicio.

**Accesibilidad (meta: WCAG 2.2 nivel AA)**

- Estructura semántica: `header`, `nav`, `main`, `section` con título, `footer`. Un solo `h1` por página.
- Enlace «Saltar al contenido» visible al recibir el foco por teclado.
- Foco visible en todo lo interactivo, con un contorno de 2 px en `--accent`.
- Todos los botones y enlaces son elementos reales (`a`, `button`), con área táctil de 44 px como mínimo.
- Campos de formulario con `<label>` asociado y mensajes de error en texto, no solo con color.
- `PixelN` y los íconos decorativos con `aria-hidden="true"`; el logo con `role="img"` y `aria-label`.
- Respetar `prefers-reduced-motion` y `prefers-color-scheme`.

**Rendimiento (meta: 90 o más en Lighthouse, en las cuatro categorías)**

- Cero JavaScript de framework: solo el script del tema y el del selector de idioma.
- Fuentes autoalojadas con `font-display: swap` y precarga de las dos más usadas.
- `og-image` y capturas del proyecto optimizadas (WebP o AVIF) con `width` y `height` declarados, para evitar saltos de diseño.

## Despliegue

Lo normal será correrla en local. Si se publica, al ser un sitio estático basta con S3 y CloudFront.

**En local**

```bash
npm install
npm run dev       # http://localhost:4321
npm run build     # genera la carpeta dist/
npm run preview   # sirve dist/ para probar el resultado final
```

**En AWS (opcional)**

1. Crear un bucket de S3 privado, sin acceso público.
2. Crear una distribución de CloudFront con ese bucket como origen y control de acceso al origen (OAC), de modo que solo CloudFront pueda leerlo.
3. Pedir un certificado en AWS Certificate Manager (para CloudFront debe crearse en la región `us-east-1`) y asociar el dominio, por ejemplo con Route 53.
4. Agregar una función de CloudFront que complete las rutas con `index.html`, porque con OAC el bucket no lo hace solo (`/es/` debe servir `/es/index.html`).
5. Subir el sitio e invalidar la caché en cada despliegue.

```js
// CloudFront Function (viewer request)
function handler(event) {
  var request = event.request;
  var uri = request.uri;
  if (uri.endsWith('/')) {
    request.uri += 'index.html';
  } else if (!uri.includes('.')) {
    request.uri += '/index.html';
  }
  return request;
}
```

```bash
aws s3 sync dist/ s3://NOMBRE-DEL-BUCKET --delete
aws cloudfront create-invalidation --distribution-id ID-DE-LA-DISTRIBUCION --paths "/*"
```

- Caché larga (un año, `immutable`) para los archivos con hash en `_astro/`; caché corta para el HTML.
- Opcional: un flujo de GitHub Actions que ejecute `npm ci`, `npm run build`, la sincronización y la invalidación al hacer push a `main`.
- Si se publica, habrá que cambiar `site` en `astro.config.mjs` por el dominio real.

## Pendientes y mejoras

Lo que falta por rellenar antes de entregar:

- [ ] ComedorUAGRM: descripción, capturas, tecnologías usadas y enlace al demo (sección Portafolio).
- [ ] Apellidos de Mauro y Leonel.
- [ ] Correo de contacto real, si lo quieren (hoy es `contacto@nexware.example`).
- [ ] Textos legales completos en español e inglés.
- [ ] Exportar el favicon (`favicon.svg`, y PNG de 32 y 180 px) y la imagen `og-image.png` de 1200×630.
- [ ] Comprobar el contraste de los pares de color y pasar Lighthouse.
- [ ] Revisar con Claude Code las opciones de `i18n` y `sitemap` según la versión de Astro instalada.

Mejoras que se pueden pedir después:

- Una sección «Cómo trabajamos» con los pasos de un proyecto (descubrimiento, diseño, desarrollo, entrega).
- Más proyectos en el portafolio.
- Detección automática del idioma del navegador en la primera visita.
- Un formulario que envíe correos reales mediante un servicio externo.
- Una página 404 con la N de píxeles.

## Prompt para Claude Code

Copien este texto en Claude Code, con `SPEC.md` en la raíz del proyecto y las capturas de los tres mockups adjuntas:

```text
Lee SPEC.md completo y las capturas de los mockups (escritorio claro, escritorio oscuro y móvil).

Construye la landing de Nexware con Astro y TypeScript, siguiendo SPEC.md al pie de la letra:
1. Crea el proyecto con la estructura de carpetas indicada.
2. Implementa tokens.css (claro y oscuro) y el cambio de tema sin parpadeo.
3. Configura i18n (es por defecto, en) con es.json y en.json, y el script check:i18n.
4. Construye los componentes y la página de inicio fieles a los mockups, con diseño fluido.
5. Redacta las cuatro páginas legales en español e inglés como colecciones de contenido, con el aviso de documento de ejemplo.
6. Deja las secciones marcadas con [corchetes] (ComedorUAGRM, apellidos) como marcadores visibles.
7. Añade SEO, hreflang, sitemap y accesibilidad según SPEC.md.

Al terminar: ejecuta npm run build y npm run check:i18n, y dime qué quedó pendiente.
No agregues librerías de interfaz ni JavaScript de framework.
```
