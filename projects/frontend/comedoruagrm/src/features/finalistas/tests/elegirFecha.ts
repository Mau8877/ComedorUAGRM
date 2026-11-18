import { screen } from '@testing-library/react'
import type { UserEvent } from '@testing-library/user-event'

/** Abre el calendario del campo y elige el día `YYYY-MM-DD` (navega de mes si hace falta). */
export async function elegirFecha(user: UserEvent, campo: HTMLElement, iso: string) {
  await user.click(campo)
  await screen.findByRole('grid')
  for (let intentos = 0; intentos < 24; intentos++) {
    const celda = document.querySelector<HTMLButtonElement>(`[data-iso="${iso}"]`)
    if (celda) {
      await user.click(celda)
      return
    }
    await user.click(screen.getByRole('button', { name: 'Mes siguiente' }))
  }
  throw new Error(`No se encontró el día ${iso} en el calendario`)
}

/** Abre el calendario del campo y devuelve el botón del día `YYYY-MM-DD` sin pulsarlo. */
export async function abrirYBuscarDia(user: UserEvent, campo: HTMLElement, iso: string) {
  await user.click(campo)
  await screen.findByRole('grid')
  for (let intentos = 0; intentos < 24; intentos++) {
    const celda = document.querySelector<HTMLButtonElement>(`[data-iso="${iso}"]`)
    if (celda) return celda
    const siguiente = screen.getByRole('button', { name: 'Mes siguiente' })
    if ((siguiente as HTMLButtonElement).disabled) break
    await user.click(siguiente)
  }
  return null
}

/** `YYYY-MM-DD` como lo muestra el campo de fecha: `dd/mm/aaaa`. */
export function formatoVisible(iso: string): string {
  const [anio, mes, dia] = iso.split('-')
  return `${dia}/${mes}/${anio}`
}
