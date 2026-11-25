import { test } from '@playwright/test'
import { anotar, cap, expect, login, USUARIOS } from './util'

const CU = 'CU14'
const n = Date.now()
const correo = `director.${n}@dubss.test`
const ci = String(n).slice(-7)
let temporalCreada = ''

test('CU14-A crear cuenta de directivo, ver contraseña temporal y entrar con ella', async ({ page }) => {
  await login(page, USUARIOS.admin)
  await page.goto('/usuarios/directivos')
  await page.waitForTimeout(2500)
  await cap(page, CU, 'A-inicial')
  await page.getByRole('button', { name: 'Nueva cuenta' }).click()
  const d = page.getByRole('dialog')
  await d.locator('#nombres').fill('Rodrigo')
  await d.locator('#apellidos').fill('Suárez Melgar')
  await d.locator('#ci').fill(ci)
  await d.locator('#correo').fill(correo)
  await cap(page, CU, 'A-formulario')
  await d.getByRole('button', { name: 'Crear cuenta' }).click()
  await page.waitForTimeout(2500)
  await cap(page, CU, 'A-final')
  const texto = (await page.getByRole('dialog').innerText()).replace(/\s+/g, ' ')
  anotar(CU, 'A: diálogo tras crear: ' + texto.slice(0, 400))
  const pass = (texto.match(/\.\s(\S{10,16})\sCopiar/) || ['', ''])[1]
  temporalCreada = pass
  anotar(CU, 'A: contraseña temporal mostrada: ' + (pass ? 'sí (' + pass.length + ' caracteres)' : 'NO localizada'))
  await page.getByRole('button', { name: /Entendido/ }).click()
  await page.context().clearCookies()
  if (pass) {
    await login(page, correo, pass)
    anotar(CU, `A: login con la contraseña temporal -> ${new URL(page.url()).pathname}`)
    await cap(page, CU, 'A-login-temporal')
  }
})

test('CU14-B crear con CI repetido', async ({ page }) => {
  await login(page, USUARIOS.admin)
  await page.goto('/usuarios/directivos')
  await page.waitForTimeout(2500)
  await page.getByRole('button', { name: 'Nueva cuenta' }).click()
  const d = page.getByRole('dialog')
  await d.locator('#nombres').fill('Otro')
  await d.locator('#apellidos').fill('Duplicado')
  await d.locator('#ci').fill('0000002')
  await d.locator('#correo').fill(`dup.${n}@dubss.test`)
  await cap(page, CU, 'B-inicial')
  await d.getByRole('button', { name: 'Crear cuenta' }).click()
  await page.waitForTimeout(2500)
  await cap(page, CU, 'B-final')
  anotar(CU, 'B: mensaje bajo el campo CI: ' + (await page.getByRole('dialog').innerText()).replace(/\s+/g, ' ').match(/Ya existe[^.]*?CI/)?.[0])
})

test('CU14-C desactivar la cuenta creada y comprobar que no entra', async ({ page }) => {
  await login(page, USUARIOS.admin)
  await page.goto('/usuarios/directivos')
  await page.waitForTimeout(2500)
  await page.waitForTimeout(2000)
  const fila = page.getByRole('row').filter({ hasText: correo })
  await expect(fila).toBeVisible({ timeout: 15000 })
  await cap(page, CU, 'C-inicial')
  await fila.getByLabel('Desactivar').click()
  await page.waitForTimeout(1000)
  await cap(page, CU, 'C-confirmacion')
  await page.getByText('¿Desactivar cuenta?').locator('xpath=ancestor::*[contains(@class,"bg-") or @role][1]').first().waitFor()
  await page.locator('button', { hasText: /^Desactivar$/ }).last().click()
  await page.waitForTimeout(2500)
  await cap(page, CU, 'C-final')
  anotar(CU, 'C: fila tras desactivar: ' + (await fila.innerText().catch(() => 'la fila ya no aparece')).replace(/\s+/g, ' '))
  await page.context().clearCookies()
  await page.goto('/login')
  await page.getByLabel('Correo electrónico').fill(correo)
  await page.locator('#contrasena').fill(temporalCreada)
  await page.locator('button[type=submit]').click()
  await page.waitForTimeout(2500)
  await cap(page, CU, 'C-login-desactivada')
  anotar(CU, 'C: login de la cuenta desactivada -> ' + new URL(page.url()).pathname + ' | ' + (await page.locator('body').innerText()).replace(/\s+/g, ' ').match(/[^.]*(desactiv|DUBSS)[^.]*\./i)?.[0])
})
