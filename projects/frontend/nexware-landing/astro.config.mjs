import { defineConfig } from 'astro/config'
import sitemap from '@astrojs/sitemap'

// `base: '/landing-page'` porque nginx (ver infrastructure/nginx/nginx.conf)
// reenvía ese prefijo tal cual al dev server de Astro, sin recortarlo --
// así los links/assets que genera Astro ya incluyen el prefijo y no se
// rompen al navegar detrás del proxy. Las rutas de idioma quedan entonces
// en /landing-page/es/ y /landing-page/en/.
//
// `site` es un dominio de ejemplo (el TLD .example está reservado): se
// cambia por el real si la landing se publica.
export default defineConfig({
  site: 'https://nexware.example',
  base: '/landing-page',
  output: 'static',
  server: {
    host: true,
    port: 4321,
  },
  i18n: {
    defaultLocale: 'es',
    locales: ['es', 'en'],
    routing: { prefixDefaultLocale: true },
  },
  integrations: [
    sitemap({
      // La raíz solo redirige a /es/, no es una página con contenido propio.
      filter: (page) => page !== 'https://nexware.example/landing-page/',
      i18n: { defaultLocale: 'es', locales: { es: 'es-BO', en: 'en-US' } },
    }),
  ],
})
