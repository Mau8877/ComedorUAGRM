import { expect, type Page, type TestInfo } from '@playwright/test'

export const ADMIN = { correo: 'admin@dubss.com', contrasena: '123123123' }

/** Guarda una captura en tests/e2e/capturas con un nombre legible. */
export async function capturar(page: Page, nombre: string) {
  await page.waitForTimeout(500)
  await page.screenshot({ path: `capturas/${nombre}.png` })
}

export async function iniciarSesion(page: Page, correo: string, contrasena: string) {
  await page.goto('/login')
  await page.getByLabel('Correo electrónico').fill(correo)
  await page.locator('#contrasena').fill(contrasena)
  await page.locator('button[type=submit]').click()
  await expect(page).toHaveURL(/\/panel/, { timeout: 20_000 })
}

/** Elige una opción de un combobox de shadcn/base-ui por el texto de la opción. */
export async function elegirOpcion(page: Page, combo: ReturnType<Page['getByRole']>, texto: RegExp | string) {
  await combo.click()
  await page.getByRole('option', { name: texto }).first().click()
}

export type { TestInfo }
