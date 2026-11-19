import { expect, test } from '@playwright/test'
import { ADMIN, capturar, elegirOpcion, iniciarSesion } from './ayudas'

test('Flujo 3: el administrador audita y adjudica la beca a un finalista (CU-05 a CU-07)', async ({ page }) => {
  await iniciarSesion(page, ADMIN.correo, ADMIN.contrasena)
  await page.goto('/finalistas')
  await elegirOpcion(page, page.getByRole('combobox').first(), /E04/)
  const fila = page.getByRole('row', { name: /Elena/ })
  await expect(fila).toBeVisible()
  await capturar(page, '03a-finalistas-convocatoria-e04')

  await fila.getByLabel('Auditar').click()
  await expect(page.getByText('AUDITAR EXPEDIENTE').first()).toBeVisible()
  await expect(page.getByText('Validado').first()).toBeVisible()
  await capturar(page, '03b-expediente-auditado')

  await page.getByRole('button', { name: 'Adjudicar', exact: true }).click()
  const dialogo = page.getByRole('dialog')
  await expect(dialogo.getByText('La decisión es definitiva')).toBeVisible()
  await capturar(page, '03c-confirmar-adjudicacion')
  await dialogo.getByRole('button', { name: 'Adjudicar' }).click()
  await expect(dialogo).toBeHidden({ timeout: 15_000 })

  await page.goto('/adjudicacion')
  await elegirOpcion(page, page.getByRole('combobox').first(), /E04/)
  await expect(page.getByText('Adjudicados').first()).toBeVisible()
  await capturar(page, '03d-adjudicacion-resumen')
})
