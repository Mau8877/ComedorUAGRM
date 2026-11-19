import { expect, test } from '@playwright/test'
import { capturar } from './ayudas'

test('Flujo 2: un estudiante se registra y guarda el borrador de su postulación (CU-01, CU-16, CU-17)', async ({ page }) => {
  const n = Date.now()
  await page.goto('/registro')
  await page.fill('#nombres', 'Ana')
  await page.fill('#apellidos', 'Prueba')
  await page.fill('#ci', String(n).slice(-8))
  await page.fill('#registroUniversitario', '2' + String(n).slice(-8))
  await page.getByRole('combobox').click()
  await page.getByRole('option').first().click()
  await page.fill('#telefono', '70000000')
  await page.fill('#correo', `e2e${n}@prueba.test`)
  await page.fill('#contrasena', 'Clave123')
  await page.fill('#confirmarContrasena', 'Clave123')
  await capturar(page, '02a-registro-completo')
  await page.getByRole('button', { name: 'Crear cuenta' }).click()
  await expect(page).toHaveURL(/\/panel/, { timeout: 20_000 })
  await capturar(page, '02b-panel-postulante')

  await page.goto('/mis-postulaciones')
  await expect(page.getByText('Todavía no enviaste ninguna postulación')).toBeVisible()
  await page.locator('main').getByText('Postular', { exact: true }).click()
  await expect(page.getByText('Paso 1 de 3')).toBeVisible()
  await capturar(page, '02c-postular-paso-1')
  await page.getByRole('button', { name: 'Continuar' }).click()

  // Paso 2: datos socioeconómicos
  await page.fill('#ingresosFamiliares', '1500')
  await page.fill('#dependientes', '3')
  await page.fill('#promedioAcademico', '70')
  const combos = page.locator('main [role=combobox], main button[id^="base-ui-"][type=button]')
  const total = await combos.count()
  for (let i = 0; i < total; i++) {
    const c = combos.nth(i)
    if (/Selecciona una opción/.test((await c.innerText()).trim())) {
      await c.click()
      await page.getByRole('option').first().click()
    }
  }
  await capturar(page, '02d-postular-paso-2')
  await page.getByRole('button', { name: 'Guardar borrador' }).click()
  const aviso = page.getByText(/Borrador guardado ·/)
  await expect(aviso).toBeVisible({ timeout: 15_000 })
  await aviso.scrollIntoViewIfNeeded()
  await capturar(page, '02e-borrador-guardado')
})
