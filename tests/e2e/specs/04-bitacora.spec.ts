import { expect, test } from '@playwright/test'
import { ADMIN, capturar, iniciarSesion } from './ayudas'

test('Flujo 4: el administrador consulta la bitácora y ve los eventos registrados (CU-25)', async ({ page }) => {
  await iniciarSesion(page, ADMIN.correo, ADMIN.contrasena)
  await page.goto('/bitacora')
  await expect(page.getByText('BITÁCORA').first()).toBeVisible()
  // Hay al menos un inicio de sesión: el del propio administrador.
  await expect(page.getByRole('row').nth(1)).toBeVisible({ timeout: 15_000 })
  await expect(page.getByText(/Inicio de sesión|INICIO_SESION/i).first()).toBeVisible()
  await capturar(page, '04a-bitacora')
})
