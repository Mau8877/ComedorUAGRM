import { test } from '@playwright/test'
import { anotar, cap, elegir, expect, login, USUARIOS } from './util'

const CU = 'CU07'

async function abrirExpediente(page: import('@playwright/test').Page, conv: RegExp, fila: number) {
  await page.goto('/finalistas')
  await page.waitForTimeout(2500)
  await elegir(page, page.getByRole('combobox').first(), conv)
  await page.waitForTimeout(2500)
  await page.getByLabel('Auditar').nth(fila).click()
  await expect(page.getByText('AUDITAR EXPEDIENTE').first()).toBeVisible()
  await page.waitForTimeout(5000)
}

test('CU07-A registrar entrega y auditar como Validado con puntaje académico', async ({ page }) => {
  await login(page, USUARIOS.admin)
  await abrirExpediente(page, /E03/, 1)
  await cap(page, CU, 'A-inicial')
  anotar(CU, 'A: aviso inicial: ' + (await page.locator('main').innerText()).replace(/\s+/g, ' ').match(/Primero registra la entrega[^.]*\./)?.[0])
  if (await page.getByRole('button', { name: 'Marcar como recibida' }).count()) {
    await page.getByRole('button', { name: 'Marcar como recibida' }).click()
    await page.waitForTimeout(3000)
  }
  await page.locator('label[for=auditoria-resultado-VALIDADO], label:has(#auditoria-resultado-VALIDADO)').first().click()
  await page.waitForTimeout(800)
  const campos = await page.locator('main input:not([type=radio]):not([type=checkbox]), main textarea').evaluateAll((e) => e.map((x) => x.id))
  anotar(CU, 'A: campos tras elegir Validado: ' + campos.join(', '))
  const puntaje = page.locator('main input[id*=untaje]').first()
  await puntaje.fill('25')
  await cap(page, CU, 'A-formulario')
  await page.getByRole('button', { name: 'Registrar auditoría' }).click()
  await cap(page, CU, 'A-confirmacion')
  await page.getByRole('button', { name: 'Confirmar', exact: true }).click()
  await page.waitForTimeout(5000)
  await page.getByText('Historial de auditorías').scrollIntoViewIfNeeded()
  await cap(page, CU, 'A-final')
  const t = (await page.locator('main').innerText()).replace(/\s+/g, ' ')
  anotar(CU, 'A: puntajes: ' + t.match(/Puntaje IA[^]*?Total \(de 100\)[^]*?(\d+\.\d+|—)/)?.[0])
  anotar(CU, 'A: historial: ' + t.match(/Historial de auditorías.*$/)?.[0].slice(0, 300))
})

test('CU07-B observado sin observaciones es rechazado', async ({ page }) => {
  await login(page, USUARIOS.admin)
  await abrirExpediente(page, /E03/, 2)
  if (await page.getByRole('button', { name: 'Marcar como recibida' }).count()) {
    await page.getByRole('button', { name: 'Marcar como recibida' }).click()
    await page.waitForTimeout(3000)
  }
  await page.locator('label[for=auditoria-resultado-OBSERVADO], label:has(#auditoria-resultado-OBSERVADO)').first().click()
  await cap(page, CU, 'B-inicial')
  await page.getByRole('button', { name: 'Registrar auditoría' }).click()
  await page.waitForTimeout(2500)
  await cap(page, CU, 'B-final')
  const t = (await page.locator('main').innerText()).replace(/\s+/g, ' ')
  anotar(CU, 'B: mensaje: ' + t.match(/Observaciones[^]{0,160}/g)?.join(' || '))
  anotar(CU, 'B: historial: ' + t.match(/Historial de auditorías.*$/)?.[0].slice(0, 200))
})

test('CU07-C finalista sin entrega no deja auditar', async ({ page }) => {
  await login(page, USUARIOS.admin)
  await abrirExpediente(page, /E04/, 3)
  await cap(page, CU, 'C-final')
  const t = (await page.locator('main').innerText()).replace(/\s+/g, ' ')
  anotar(CU, 'C: ' + t.match(/Documentación física \S+/)?.[0] + ' | ' + (t.match(/Primero registra la entrega[^.]*\./)?.[0] ?? 'sin aviso') + ' | botón registrar auditoría habilitado: ' + (await page.getByRole('button', { name: 'Registrar auditoría' }).isEnabled()))
})
