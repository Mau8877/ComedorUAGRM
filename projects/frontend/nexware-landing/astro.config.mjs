import { defineConfig } from 'astro/config'

// `base: '/landing-page'` porque nginx (ver infrastructure/nginx/nginx.conf)
// reenvía ese prefijo tal cual al dev server de Astro, sin recortarlo --
// así los links/assets que genera Astro ya incluyen el prefijo y no se
// rompen al navegar detrás del proxy.
export default defineConfig({
  base: '/landing-page',
  output: 'static',
  server: {
    host: true,
    port: 4321,
  },
})
