import { test } from '@playwright/test'
import { anotar, cap, elegir, expect, login, USUARIOS } from './util'

async function abrir(page: import('@playwright/test').Page, fila: number) {
  await page.goto('/finalistas')
  await page.waitForTimeout(2500)
  await elegir(page, page.getByRole('combobox').first(), /E04/)
  await page.waitForTimeout(2500)
  await page.getByLabel('Auditar').nth(fila).click()
  await expect(page.getByText('AUDITAR EXPEDIENTE').first()).toBeVisible()
  await page.waitForTimeout(5000)
}

const dlg = (page: import('@playwright/test').Page) => page.locator('[role=dialog],[role=alertdialog]').last()

test('CU19-A devolver para corrección una postulación auditada (E04, finalista 2)', async ({ page }) => {
  const CU = 'CU19'
  await login(page, USUARIOS.admin)
  await abrir(page, 1)
  const t0 = (await page.locator('main').innerText()).replace(/\s+/g, ' ')
  anotar(CU, 'A: estado inicial: ' + t0.match(/Ana[^]*?Estado de la postulación \S+/)?.[0].slice(0, 200))
  await cap(page, CU, 'A-inicial')
  await page.getByRole('button', { name: 'Devolver para corrección' }).click()
  await page.waitForTimeout(800)
  anotar(CU, 'A: ventana: ' + (await dlg(page).innerText()).replace(/\s+/g, ' ').slice(0, 300))
  await dlg(page).locator('textarea').fill('Falta la fotocopia legible del certificado de ingresos; vuelva a presentarla.')
  await cap(page, CU, 'A-motivo')
  await dlg(page).getByRole('button', { name: /Devolver/ }).last().click()
  await page.waitForTimeout(5000)
  await cap(page, CU, 'A-final')
  const t = (await page.locator('main').innerText()).replace(/\s+/g, ' ')
  anotar(CU, 'A: estado final: ' + t.match(/Estado de la postulación \S+/)?.[0])
})

test('CU19-B motivo vacío no se acepta', async ({ page }) => {
  const CU = 'CU19'
  await login(page, USUARIOS.admin)
  await abrir(page, 2)
  await page.getByRole('button', { name: 'Devolver para corrección' }).click()
  await page.waitForTimeout(800)
  await cap(page, CU, 'B-inicial')
  await dlg(page).getByRole('button', { name: /Devolver/ }).last().click()
  await page.waitForTimeout(1500)
  await cap(page, CU, 'B-final')
  anotar(CU, 'B: ' + (await dlg(page).innerText()).replace(/\s+/g, ' ').slice(0, 300))
})

test('CU20-A archivar una postulación (E04, finalista 3)', async ({ page }) => {
  const CU = 'CU20'
  await login(page, USUARIOS.admin)
  await abrir(page, 2)
  await cap(page, CU, 'A-inicial')
  await page.getByRole('button', { name: 'Archivar' }).click()
  await page.waitForTimeout(800)
  anotar(CU, 'A: ventana: ' + (await dlg(page).innerText()).replace(/\s+/g, ' ').slice(0, 300))
  await dlg(page).locator('textarea').fill('El postulante no cumple el requisito de matrícula vigente.')
  await cap(page, CU, 'A-motivo')
  await dlg(page).getByRole('button', { name: /Archivar/ }).last().click()
  await page.waitForTimeout(5000)
  await cap(page, CU, 'A-final')
  const t = (await page.locator('main').innerText()).replace(/\s+/g, ' ')
  anotar(CU, 'A: estado final: ' + t.match(/Estado de la postulación \S+/)?.[0])
})

test('CU20-B archivar sin motivo', async ({ page }) => {
  const CU = 'CU20'
  await login(page, USUARIOS.admin)
  await abrir(page, 3)
  await page.getByRole('button', { name: 'Archivar' }).click()
  await page.waitForTimeout(800)
  await cap(page, CU, 'B-inicial')
  await dlg(page).getByRole('button', { name: /Archivar/ }).last().click()
  await page.waitForTimeout(1500)
  await cap(page, CU, 'B-final')
  anotar(CU, 'B: ' + (await dlg(page).innerText()).replace(/\s+/g, ' ').slice(0, 300))
})
