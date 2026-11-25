import { test } from '@playwright/test'
import { anotar, cap, elegir, login, USUARIOS } from './util'

const CU = 'CU22'
const dlg = (page: import('@playwright/test').Page) => page.locator('[role=dialog],[role=alertdialog]').last()

test('CU22-A publicar los resultados de E05', async ({ page }) => {
  await login(page, USUARIOS.admin)
  await page.goto('/publicar-resultados')
  await page.waitForTimeout(2500)
  await elegir(page, page.getByRole('combobox').first(), /E05/)
  await page.waitForTimeout(3000)
  await cap(page, CU, 'A-inicial')
  anotar(CU, 'A: resumen antes: ' + (await page.locator('main').innerText()).replace(/\s+/g, ' ').slice(0, 500))
  await page.getByRole('button', { name: 'Publicar resultados' }).first().click()
  await page.waitForTimeout(1000)
  await cap(page, CU, 'A-confirmacion')
  anotar(CU, 'A: ventana: ' + (await dlg(page).innerText()).replace(/\s+/g, ' ').slice(0, 500))
  await dlg(page).getByRole('button', { name: /Publicar|Confirmar/ }).last().click()
  await page.waitForTimeout(6000)
  await cap(page, CU, 'A-final')
  anotar(CU, 'A: después: ' + (await page.locator('body').innerText()).replace(/\s+/g, ' ').match(/Publicad[^.]*\./i)?.[0])
  const r = await (await page.request.get('http://localhost:8025/api/v1/messages?limit=8')).json()
  anotar(CU, 'A: correos recientes: ' + r.messages.map((m: any) => m.Subject + ' -> ' + m.To.map((x: any) => x.Address).join(',')).join(' || '))
})

test('CU22-B no se pueden publicar resultados con decisiones pendientes', async ({ page }) => {
  await login(page, USUARIOS.admin)
  await page.goto('/publicar-resultados')
  await page.waitForTimeout(2500)
  await elegir(page, page.getByRole('combobox').first(), /Con pendientes/)
  await page.waitForTimeout(3000)
  await cap(page, CU, 'B-inicial')
  const boton = page.getByRole('button', { name: 'Publicar resultados' }).first()
  anotar(CU, 'B: botón Publicar habilitado=' + (await boton.isEnabled()) + '; texto: ' + (await page.locator('main').innerText()).replace(/\s+/g, ' ').slice(0, 400))
  await boton.click({ force: true, timeout: 3000 }).catch(() => {})
  await page.waitForTimeout(1500)
  await cap(page, CU, 'B-final')
})

test('CU22-C el postulante consulta los resultados publicados', async ({ page }) => {
  await login(page, USUARIOS.finalista)
  await page.goto('/resultados')
  await page.waitForTimeout(3500)
  await cap(page, CU, 'C-inicial')
  anotar(CU, 'C: ' + (await page.locator('main').innerText()).replace(/\s+/g, ' ').slice(0, 500))
  const combos = page.getByRole('combobox')
  if (await combos.count()) {
    await elegir(page, combos.first(), /E06|Publicada/)
    await page.waitForTimeout(3000)
  }
  await cap(page, CU, 'C-final')
  anotar(CU, 'C: tras elegir convocatoria: ' + (await page.locator('main').innerText()).replace(/\s+/g, ' ').slice(0, 600))
})
