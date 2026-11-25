import { defineConfig } from '@playwright/test'

// Pruebas de caja negra de la persona A (capítulo 9). Capturas en docs/pruebas-caja-negra/capturas.
export default defineConfig({
  testDir: './caja-negra',
  outputDir: './test-results-cn',
  timeout: 120_000,
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: process.env.WEB_URL || 'http://localhost',
    viewport: { width: 1366, height: 800 },
    locale: 'es-BO',
    colorScheme: 'light',
    trace: 'off',
  },
})
