import { test } from '@playwright/test'
import { anotar, cap, login, USUARIOS } from './util'

const CU = 'CU04'

test('CU04-A consultar la lista de mis postulaciones y el detalle', async ({ page }) => {
  await login(page, USUARIOS.finalista)
  await page.goto('/mis-postulaciones')
  await page.waitForTimeout(4000)
  await cap(page, CU, 'A-inicial')
  anotar(CU, 'A: lista: ' + (await page.locator('main').innerText()).replace(/\s+/g, ' ').slice(0, 900))
  const ver = page.getByLabel(/Ver/).first()
  await ver.click().catch(() => {})
  await page.waitForTimeout(4000)
  await cap(page, CU, 'A-final')
  anotar(CU, `A: URL detalle ${new URL(page.url()).pathname}; texto: ` + (await page.locator('main').innerText()).replace(/\s+/g, ' ').slice(0, 900))
})

test('CU04-B cuenta sin postulaciones', async ({ page }) => {
  const n = Date.now()
  await page.goto('/registro')
  await page.fill('#nombres', 'Pablo')
  await page.fill('#apellidos', 'Mamani Quispe')
  await page.fill('#ci', String(n).slice(-8))
  await page.fill('#registroUniversitario', '2' + String(n).slice(-8))
  await page.getByRole('combobox').click()
  await page.getByRole('option').first().click()
  await page.fill('#telefono', '70000002')
  await page.fill('#correo', `sinpost.${n}@prueba.test`)
  await page.fill('#contrasena', 'Clave123')
  await page.fill('#confirmarContrasena', 'Clave123')
  await page.getByRole('button', { name: 'Crear cuenta' }).click()
  await page.waitForURL(/\/panel/, { timeout: 20_000 })
  await page.goto('/mis-postulaciones')
  await page.waitForTimeout(3500)
  await cap(page, CU, 'B-final')
  anotar(CU, 'B: ' + (await page.locator('main').innerText()).replace(/\s+/g, ' ').slice(0, 300))
})
