import { expect, test } from '@playwright/test'
import { ADMIN, capturar, iniciarSesion } from './ayudas'

test.describe('Flujo 1: inicio de sesión (CU-01)', () => {
  test('credenciales incorrectas muestran un error y no dejan entrar', async ({ page }) => {
    await page.goto('/login')
    await page.getByLabel('Correo electrónico').fill(ADMIN.correo)
    await page.locator('#contrasena').fill('clave-incorrecta')
    await page.locator('button[type=submit]').click()
    await expect(page).toHaveURL(/\/login/)
    await expect(page.getByText(/incorrect|inválid|no coinciden/i).first()).toBeVisible()
    await capturar(page, '01a-login-credenciales-incorrectas')
  })

  test('el administrador entra a su panel', async ({ page }) => {
    await page.goto('/login')
    await capturar(page, '01b-pantalla-login')
    await iniciarSesion(page, ADMIN.correo, ADMIN.contrasena)
    await expect(page.getByText('Administrador DUBSS').first()).toBeVisible()
    await capturar(page, '01c-panel-administrador')
  })
})
