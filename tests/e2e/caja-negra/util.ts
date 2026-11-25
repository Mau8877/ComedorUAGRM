import { expect, type Page } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'

export const RAIZ = path.resolve(__dirname, '../../../docs/pruebas-caja-negra')
export const PASS = '123123123'
export const USUARIOS = {
  admin: 'admin@dubss.com',
  directivo: 'directivo@dubss.com',
  finalista: 'postulante.finalista@prueba.local',
}

/** Captura de pantalla completa de la ventana en docs/pruebas-caja-negra/capturas/CUxx/. */
export async function cap(page: Page, cu: string, nombre: string) {
  await page.waitForLoadState('networkidle', { timeout: 15_000 }).catch(() => {})
  await page.waitForTimeout(1500)
  const dir = path.join(RAIZ, 'capturas', cu)
  fs.mkdirSync(dir, { recursive: true })
  await page.screenshot({ path: path.join(dir, `${nombre}.png`) })
}

/** Anota un resultado observado (se usa luego para redactar el md). */
export function anotar(cu: string, linea: string) {
  const f = path.join(RAIZ, 'capturas', cu, 'observado.txt')
  fs.mkdirSync(path.dirname(f), { recursive: true })
  fs.appendFileSync(f, linea + '\n')
}

export async function login(page: Page, correo: string, pass = PASS) {
  await page.goto('/login')
  await page.getByLabel('Correo electrónico').fill(correo)
  await page.locator('#contrasena').fill(pass)
  await page.locator('button[type=submit]').click()
  await page.waitForURL((u) => !u.pathname.endsWith('/login'), { timeout: 20_000 })
}

export async function elegir(page: Page, combo: ReturnType<Page['getByRole']>, texto: RegExp | string) {
  await combo.click()
  await page.getByRole('option', { name: texto }).first().click()
}

export { expect }
