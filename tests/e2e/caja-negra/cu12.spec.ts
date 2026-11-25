import fs from 'node:fs'
import path from 'node:path'
import { test } from '@playwright/test'
import { anotar, cap, login, RAIZ, USUARIOS } from './util'

const CU = 'CU12'

async function abrirDashboard(page: import('@playwright/test').Page) {
  await login(page, USUARIOS.directivo)
  await page.goto('/dashboard')
  await page.waitForTimeout(5000)
}

async function elegirGestion(page: import('@playwright/test').Page, nombre: string) {
  await page.locator('main [role=combobox]').first().click()
  await page.getByRole('option', { name: nombre, exact: true }).click()
  await page.getByRole('button', { name: 'Aplicar' }).click()
  await page.waitForTimeout(5000)
}

test('CU12-A el directivo abre el dashboard (selección por defecto)', async ({ page }) => {
  await abrirDashboard(page)
  await cap(page, CU, 'A-inicial')
  anotar(CU, 'A: por defecto: ' + (await page.locator('main').innerText()).replace(/\s+/g, ' ').slice(0, 600))
})

test('CU12-B filtrar por la gestión [PRUEBA] 2026-II y ver los indicadores', async ({ page }) => {
  await abrirDashboard(page)
  await cap(page, CU, 'B-inicial')
  await elegirGestion(page, '[PRUEBA] 2026-II')
  await cap(page, CU, 'B-final')
  anotar(CU, 'B: gestión 2026-II: ' + (await page.locator('main').innerText()).replace(/\s+/g, ' ').slice(0, 1200))
  await page.getByText(/Análisis BI/i).first().scrollIntoViewIfNeeded().catch(() => {})
  await page.waitForTimeout(1500)
  await cap(page, CU, 'B-analisis-bi')
})

test('CU12-C descargar el PDF de indicadores de la gestión con datos', async ({ page }) => {
  await abrirDashboard(page)
  await elegirGestion(page, '[PRUEBA] 2026-II')
  await cap(page, CU, 'C-inicial')
  const [descarga] = await Promise.all([
    page.waitForEvent('download', { timeout: 30_000 }),
    page.getByRole('button', { name: /Descargar PDF/ }).click(),
  ])
  const destino = path.join(RAIZ, 'capturas', CU, 'indicadores-2026-II.pdf')
  await descarga.saveAs(destino)
  anotar(CU, `C: PDF ${descarga.suggestedFilename()} (${fs.statSync(destino).size} bytes)`)
  await cap(page, CU, 'C-final')
})

test('CU12-D el administrador no accede al dashboard', async ({ page }) => {
  await login(page, USUARIOS.admin)
  await page.goto('/dashboard')
  await page.waitForTimeout(4000)
  await cap(page, CU, 'D-final')
  anotar(CU, `D: admin en /dashboard -> URL ${new URL(page.url()).pathname}; ` + (await page.locator('main').innerText()).replace(/\s+/g, ' ').slice(0, 200))
})
