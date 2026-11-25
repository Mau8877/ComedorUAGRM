import { test } from '@playwright/test'
import { anotar, cap, expect, login, PASS } from './util'

const CU = 'CU15'

async function registrar(page: import('@playwright/test').Page, correo: string) {
  const n = Date.now()
  await page.goto('/registro')
  await page.fill('#nombres', 'Mario')
  await page.fill('#apellidos', 'Peña Lijerón')
  await page.fill('#ci', String(n).slice(-8))
  await page.fill('#registroUniversitario', '2' + String(n).slice(-8))
  await page.getByRole('combobox').click()
  await page.getByRole('option').first().click()
  await page.fill('#telefono', '70000001')
  await page.fill('#correo', correo)
  await page.fill('#contrasena', 'Clave123')
  await page.fill('#confirmarContrasena', 'Clave123')
  await page.getByRole('button', { name: 'Crear cuenta' }).click()
  await page.waitForURL(/\/panel/, { timeout: 20_000 })
  await page.context().clearCookies()
}

test('CU15-A recuperar y restablecer la contraseña por correo', async ({ page }) => {
  const correo = `recupera.${Date.now()}@prueba.test`
  await registrar(page, correo)
  await page.goto('/recuperar-contrasena')
  await page.getByLabel('Correo electrónico').fill(correo)
  await cap(page, CU, 'A-inicial')
  await page.getByRole('button', { name: /enviar|solicitar|recuperar/i }).first().click()
  await page.waitForTimeout(2500)
  await cap(page, CU, 'A-aviso-neutro')
  anotar(CU, 'A: aviso: ' + (await page.locator('main, body').first().innerText()).replace(/\s+/g, ' ').slice(0, 400))

  // Correo recibido en Mailpit
  let enlace = ''
  for (let i = 0; i < 10 && !enlace; i++) {
    const msgs = await (await page.request.get('http://localhost:8025/api/v1/search?query=' + encodeURIComponent('to:' + correo))).json()
    if (msgs.messages?.length) {
      const m = await (await page.request.get('http://localhost:8025/api/v1/message/' + msgs.messages[0].ID)).json()
      enlace = (m.Text.match(/https?:\/\/\S*restablecer-contrasena\?token=[\w-]+/) || [''])[0]
      anotar(CU, `A: asunto del correo en Mailpit: ${m.Subject}`)
      await page.goto('http://localhost:8025/view/' + msgs.messages[0].ID)
      await cap(page, CU, 'A-correo-mailpit')
    } else await page.waitForTimeout(1500)
  }
  anotar(CU, 'A: enlace recibido: ' + (enlace ? 'sí' : 'NO'))
  expect(enlace).toBeTruthy()

  await page.goto(enlace)
  await cap(page, CU, 'A-formulario-restablecer')
  const campos = page.locator('input[type=password]')
  await campos.nth(0).fill('NuevaClave456')
  await campos.nth(1).fill('NuevaClave456')
  await page.getByRole('button', { name: 'Guardar contraseña' }).click()
  await page.waitForTimeout(3000)
  await cap(page, CU, 'A-final')
  anotar(CU, `A: tras restablecer URL ${new URL(page.url()).pathname}`)
  await login(page, correo, 'NuevaClave456')
  anotar(CU, `A: login con la contraseña nueva -> ${new URL(page.url()).pathname}`)
  await cap(page, CU, 'A-login-nueva')
  // Reutilizar el enlace
  await page.context().clearCookies()
  await page.goto(enlace)
  const c2 = page.locator('input[type=password]')
  await c2.nth(0).fill('OtraClave789')
  await c2.nth(1).fill('OtraClave789')
  await page.getByRole('button', { name: 'Guardar contraseña' }).click()
  await page.waitForTimeout(2500)
  await cap(page, CU, 'B-enlace-reutilizado')
  anotar(CU, 'B: reuso del enlace: ' + (await page.locator('body').innerText()).replace(/\s+/g, ' ').slice(0, 400))
})

test('CU15-C perfil: ver datos y cambiar contraseña con la actual incorrecta', async ({ page }) => {
  const correo = `perfil.${Date.now()}@prueba.test`
  await registrar(page, correo)
  await login(page, correo, 'Clave123')
  await page.goto('/perfil')
  await page.waitForTimeout(2000)
  await cap(page, CU, 'C-inicial')
  await page.fill('#cambiar-contrasenaActual', 'EstaNoEs1')
  await page.fill('#cambiar-contrasenaNueva', 'Nueva12345')
  await page.fill('#cambiar-confirmarContrasena', 'Nueva12345')
  await page.getByRole('button', { name: 'Cambiar contraseña' }).click()
  await page.waitForTimeout(2500)
  await cap(page, CU, 'C-final')
  anotar(CU, 'C: contraseña actual incorrecta -> ' + (await page.locator('main').innerText()).replace(/\s+/g, ' ').slice(-300))
  await page.fill('#cambiar-contrasenaActual', 'Clave123')
  await page.getByRole('button', { name: 'Cambiar contraseña' }).click()
  await page.waitForTimeout(2500)
  await cap(page, CU, 'C-cambio-correcto')
  anotar(CU, 'C: con la actual correcta -> ' + (await page.locator('body').innerText()).replace(/\s+/g, ' ').slice(-300))
})
