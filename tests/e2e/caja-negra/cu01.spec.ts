import { test } from '@playwright/test'
import { anotar, cap, expect, login, PASS, USUARIOS } from './util'

const CU = 'CU01'

test('CU01-A registro de un postulante nuevo', async ({ page }) => {
  const n = Date.now()
  await page.goto('/registro')
  await page.fill('#nombres', 'Lucía')
  await page.fill('#apellidos', 'Rojas Vaca')
  await page.fill('#ci', String(n).slice(-8))
  await page.fill('#registroUniversitario', '2' + String(n).slice(-8))
  await page.getByRole('combobox').click()
  await page.getByRole('option').first().click()
  await page.fill('#telefono', '70000000')
  await page.fill('#correo', `lucia.${n}@prueba.test`)
  await page.fill('#contrasena', 'Clave123')
  await page.fill('#confirmarContrasena', 'Clave123')
  await cap(page, CU, 'A-inicial')
  await page.getByRole('button', { name: 'Crear cuenta' }).click()
  await page.waitForURL(/\/(panel|mis-postulaciones)/, { timeout: 20_000 })
  await cap(page, CU, 'A-final')
  await page.waitForTimeout(1500)
  anotar(CU, `A: URL final ${new URL(page.url()).pathname}; correo lucia.${n}@prueba.test`)
  anotar(CU, 'A: texto visible: ' + (await page.locator('main').innerText()).replace(/\s+/g, ' ').slice(0, 300))
})

test('CU01-B login con contraseña incorrecta', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('Correo electrónico').fill(USUARIOS.admin)
  await page.locator('#contrasena').fill('incorrecta1')
  await cap(page, CU, 'B-inicial')
  await page.locator('button[type=submit]').click()
  await page.waitForTimeout(2000)
  await cap(page, CU, 'B-final')
  anotar(CU, `B: URL ${new URL(page.url()).pathname}; texto: ` + (await page.locator('body').innerText()).replace(/\s+/g, ' ').slice(0, 400))
})

test('CU01-C login correcto del administrador y cierre de sesión', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('Correo electrónico').fill(USUARIOS.admin)
  await page.locator('#contrasena').fill(PASS)
  await cap(page, CU, 'C-inicial')
  await page.locator('button[type=submit]').click()
  await page.waitForURL((u) => !u.pathname.endsWith('/login'), { timeout: 20_000 })
  await cap(page, CU, 'C-final')
  anotar(CU, `C: URL tras login ${new URL(page.url()).pathname}`)
  await page.goto('/panel')
  await expect(page).toHaveURL(/panel/)
})

test('CU01-D postulante y directivo van a su módulo', async ({ page }) => {
  await login(page, USUARIOS.directivo)
  anotar(CU, `D: directivo -> ${new URL(page.url()).pathname}`)
  await cap(page, CU, 'D-directivo')
})
