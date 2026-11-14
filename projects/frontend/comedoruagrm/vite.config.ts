/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { tanstackRouter } from '@tanstack/router-plugin/vite'
import { fileURLToPath } from 'url'
import { dirname, resolve } from 'path'
import { execFileSync } from 'child_process'

const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)

// * Regenera palette.css al editar packages/palette/palette.json con el dev
// * server levantado -- Vite ya hace HMR del CSS generado, así el cambio de
// * colores se ve sin reiniciar nada (y mobile queda sincronizado de paso).
const paletteDir = resolve(__dirname, '../../../packages/palette')
const paletteBuild = resolve(paletteDir, 'scripts/build.mjs')
const palettePlugin = {
  name: 'comedoru-palette',
  configureServer(server: { watcher: { add: (p: string) => void; on: (e: string, cb: (f: string) => void) => void } }) {
    const json = resolve(paletteDir, 'palette.json')
    server.watcher.add(json)
    server.watcher.on('change', (file) => {
      if (resolve(file) === json) execFileSync(process.execPath, [paletteBuild])
    })
  },
}

export default defineConfig({
  // tanstackRouter() va antes que react() -- genera routeTree.gen.ts a partir
  // de src/routes/ y el plugin de React necesita verlo ya generado.
  plugins: [
    palettePlugin,
    tanstackRouter({
      target: 'react',
      autoCodeSplitting: true,
      routesDirectory: './src/routes',
      generatedRouteTree: './src/routeTree.gen.ts',
    }),
    react(),
    tailwindcss(),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, './src'),
    },
  },
  server: {
    host: true,
    port: Number(process.env.PORT) || 5173,
    watch: {
      usePolling: true,
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/setupTests.ts',
  },
})
