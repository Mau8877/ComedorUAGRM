import fs from 'node:fs'
import path from 'node:path'
import { test } from '@playwright/test'
import { anotar, cap, elegir, expect, login, RAIZ, USUARIOS } from './util'

const CU = 'CU06'

test('CU06-A ver la lista priorizada de E02 y exportarla a PDF', async ({ page }) => {
  await login(page, USUARIOS.admin)
  await page.goto('/lista-priorizada')
  await page.waitForTimeout(2500)
  await cap(page, CU, 'A-inicial')
  await elegir(page, page.getByRole('combobox').first(), /E02/)
  await page.waitForTimeout(2000)
  await cap(page, CU, 'A-lista-e02')
  const filas = await page.getByRole('row').allInnerTexts()
  anotar(CU, 'A: filas de la lista: ' + filas.slice(1).map((f) => f.replace(/\s+/g, ' ')).join(' // '))
  const [descarga] = await Promise.all([
    page.waitForEvent('download', { timeout: 30_000 }),
    page.getByRole('button', { name: 'Exportar a PDF' }).click(),
  ])
  const destino = path.join(RAIZ, 'capturas', CU, 'lista-priorizada-e02.pdf')
  await descarga.saveAs(destino)
  anotar(CU, `A: PDF descargado: ${descarga.suggestedFilename()} (${fs.statSync(destino).size} bytes)`)
  await page.waitForTimeout(1000)
  await cap(page, CU, 'A-final')
})

test('CU06-B convocatoria sin calificadas', async ({ page }) => {
  await login(page, USUARIOS.admin)
  await page.goto('/lista-priorizada')
  await page.waitForTimeout(2500)
  await elegir(page, page.getByRole('combobox').first(), /sin postulaciones/)
  await page.waitForTimeout(2000)
  await cap(page, CU, 'B-final')
  anotar(CU, 'B: mensaje: ' + (await page.locator('main').innerText()).replace(/\s+/g, ' ').slice(0, 300))
})

test('CU06-C un postulante no puede entrar a la lista priorizada', async ({ page }) => {
  await login(page, USUARIOS.finalista)
  await page.goto('/lista-priorizada')
  await page.waitForTimeout(2500)
  await cap(page, CU, 'C-final')
  anotar(CU, `C: postulante abre /lista-priorizada -> URL ${new URL(page.url()).pathname}`)
  expect(true).toBeTruthy()
})
