import { defineConfig } from '@playwright/test'

// Pruebas E2E del panel web. Requiere el backend (perfil dev, con datos de
// prueba) y el panel levantados; WEB_URL apunta al panel.
export default defineConfig({
  testDir: './specs',
  outputDir: './test-results',
  timeout: 60_000,
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  use: {
    baseURL: process.env.WEB_URL || 'http://localhost:5173',
    viewport: { width: 1366, height: 800 },
    locale: 'es-BO',
    trace: 'off',
  },
})
