import { test } from '@playwright/test'
import { anotar, cap, elegir, expect, login, USUARIOS } from './util'

const CU = 'CU05'
const dlg = (page: import('@playwright/test').Page) => page.locator('[role=dialog],[role=alertdialog]').last()

async function abrir(page: import('@playwright/test').Page, fila: number, conv: RegExp = /E04/) {
  await page.goto('/finalistas')
  await page.waitForTimeout(2500)
  await elegir(page, page.getByRole('combobox').first(), conv)
  await page.waitForTimeout(2500)
  await page.getByLabel('Auditar').nth(fila).click()
  await expect(page.getByText('AUDITAR EXPEDIENTE').first()).toBeVisible()
  await page.waitForTimeout(5000)
}

test('CU05-A adjudicar la beca al finalista validado de E04', async ({ page }) => {
  await login(page, USUARIOS.admin)
  await page.goto('/adjudicacion')
  await page.waitForTimeout(2500)
  await elegir(page, page.getByRole('combobox').first(), /E04/)
  await page.waitForTimeout(3000)
  await cap(page, CU, 'A-inicial')
  anotar(CU, 'A: panel antes: ' + (await page.locator('main').innerText()).replace(/\s+/g, ' ').match(/Cupos.*?Sin seleccionar \d+/)?.[0])
  await abrir(page, 0)
  anotar(CU, 'A: expediente: ' + (await page.locator('main').innerText()).replace(/\s+/g, ' ').match(/Estado de la postulación \S+/)?.[0])
  await page.getByRole('button', { name: 'Adjudicar', exact: true }).click()
  await page.waitForTimeout(1000)
  await cap(page, CU, 'A-confirmacion')
  anotar(CU, 'A: ventana: ' + (await dlg(page).innerText()).replace(/\s+/g, ' ').slice(0, 400))
  await dlg(page).getByRole('button', { name: 'Adjudicar' }).click()
  await page.waitForTimeout(5000)
  anotar(CU, 'A: estado tras adjudicar: ' + (await page.locator('main').innerText()).replace(/\s+/g, ' ').match(/Estado de la postulación \S+/)?.[0])
  await page.goto('/adjudicacion')
  await page.waitForTimeout(2500)
  await elegir(page, page.getByRole('combobox').first(), /E04/)
  await page.waitForTimeout(3000)
  await cap(page, CU, 'A-final')
  anotar(CU, 'A: panel después: ' + (await page.locator('main').innerText()).replace(/\s+/g, ' ').match(/Cupos.*?Sin seleccionar \d+/)?.[0])
})

test('CU05-B no se puede decidir sobre un finalista sin auditoría', async ({ page }) => {
  await login(page, USUARIOS.admin)
  await abrir(page, 3)
  await cap(page, CU, 'B-inicial')
  const t = (await page.locator('main').innerText()).replace(/\s+/g, ' ')
  const hab = await page.getByRole('button', { name: 'Adjudicar', exact: true }).isEnabled()
  const habNo = await page.getByRole('button', { name: 'No adjudicar', exact: true }).isEnabled()
  await page.getByRole('button', { name: 'Adjudicar', exact: true }).click({ timeout: 4000, force: true }).catch(() => {})
  await page.waitForTimeout(1500)
  await cap(page, CU, 'B-final')
  anotar(CU, `B: botón Adjudicar habilitado=${hab}; No adjudicar habilitado=${habNo}; aviso: ${t.match(/Primero [^.]*\./g)?.join(' ')}`)
})

test('CU05-C no adjudicar sin motivo (E03, finalista auditado)', async ({ page }) => {
  await login(page, USUARIOS.admin)
  await abrir(page, 1, /E03/)
  await cap(page, CU, 'C-inicial')
  await page.getByRole('button', { name: 'No adjudicar', exact: true }).click()
  await page.waitForTimeout(1000)
  anotar(CU, 'C: ventana: ' + (await dlg(page).innerText()).replace(/\s+/g, ' ').slice(0, 300))
  await dlg(page).getByRole('button', { name: /No adjudicar/ }).last().click()
  await page.waitForTimeout(1500)
  await cap(page, CU, 'C-final')
  anotar(CU, 'C: tras confirmar sin motivo: ' + (await dlg(page).innerText()).replace(/\s+/g, ' ').slice(0, 300))
})
