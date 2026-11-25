import fs from 'node:fs'
import path from 'node:path'
import { test } from '@playwright/test'
import { anotar, cap, elegir, login, RAIZ, USUARIOS } from './util'

const CU = 'CU08'
const dlg = (page: import('@playwright/test').Page) => page.locator('[role=dialog],[role=alertdialog]').last()

test('CU08-A generar el acta de adjudicación de E06', async ({ page }) => {
  await login(page, USUARIOS.admin)
  await page.goto('/actas')
  await page.waitForTimeout(3000)
  await cap(page, CU, 'A-inicial')
  await elegir(page, page.getByRole('combobox').first(), /E06/)
  await page.getByRole('button', { name: 'Vista previa' }).click()
  await page.waitForTimeout(4000)
  await cap(page, CU, 'A-vista-previa')
  anotar(CU, 'A: vista previa: ' + (await page.locator('body').innerText()).replace(/\s+/g, ' ').match(/ACTA[^]{0,300}/i)?.[0])
  await page.getByRole('button', { name: 'Generar PDF' }).click()
  await page.waitForTimeout(1200)
  await cap(page, CU, 'A-confirmacion')
  anotar(CU, 'A: ventana: ' + (await dlg(page).innerText()).replace(/\s+/g, ' ').slice(0, 400))
  await dlg(page).getByRole('button', { name: /Generar|Confirmar/ }).last().click()
  await page.waitForTimeout(7000)
  await cap(page, CU, 'A-final')
  anotar(CU, 'A: tabla de actas: ' + (await page.locator('main').innerText()).replace(/\s+/g, ' ').match(/Actas generadas[^]*?Filas/)?.[0])
})

test('CU08-B descargar el acta generada', async ({ page }) => {
  await login(page, USUARIOS.admin)
  await page.goto('/actas')
  await page.waitForTimeout(3500)
  const [descarga] = await Promise.all([
    page.waitForEvent('download', { timeout: 30_000 }),
    page.getByLabel(/Descargar/).first().click(),
  ])
  const destino = path.join(RAIZ, 'capturas', CU, 'acta-e06.pdf')
  await descarga.saveAs(destino)
  anotar(CU, `B: PDF descargado ${descarga.suggestedFilename()} (${fs.statSync(destino).size} bytes)`)
  await cap(page, CU, 'B-final')
})

test('CU08-C convocatoria con resultados sin publicar no aparece para generar acta', async ({ page }) => {
  await login(page, USUARIOS.admin)
  await page.goto('/actas')
  await page.waitForTimeout(3000)
  await page.getByRole('combobox').first().click()
  await page.waitForTimeout(800)
  await cap(page, CU, 'C-final')
  anotar(CU, 'C: opciones del selector: ' + (await page.getByRole('option').allInnerTexts()).map((o) => o.slice(0, 60)).join(' || '))
})
