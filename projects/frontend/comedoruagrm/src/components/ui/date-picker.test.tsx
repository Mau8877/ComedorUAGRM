import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { DatePicker, type DatePickerProps } from './date-picker'

function Ejemplo({ inicial = '', ...props }: { inicial?: string } & Partial<DatePickerProps>) {
  const [valor, setValor] = useState(inicial)
  return (
    <>
      <label htmlFor="fecha">Fecha</label>
      <DatePicker id="fecha" value={valor} onChange={setValor} {...props} />
      <output data-testid="valor">{valor}</output>
    </>
  )
}

describe('DatePicker', () => {
  beforeEach(() => {
    // * Solo el reloj: los temporizadores reales siguen sirviendo a userEvent y a la animación.
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 10, 16, 12, 0, 0)) // 16/11/2026
  })
  afterEach(() => vi.useRealTimers())

  it('muestra el placeholder cuando no hay fecha', () => {
    render(<Ejemplo />)
    expect(screen.getByLabelText('Fecha')).toHaveTextContent('dd/mm/aaaa')
  })

  it('muestra la fecha elegida con formato dd/mm/aaaa', () => {
    render(<Ejemplo inicial="2026-12-05" />)
    expect(screen.getByLabelText('Fecha')).toHaveTextContent('05/12/2026')
  })

  it('abre en el mes del valor y elige un día con un clic', async () => {
    const user = userEvent.setup()
    render(<Ejemplo inicial="2026-12-05" />)

    await user.click(screen.getByLabelText('Fecha'))
    expect(await screen.findByRole('grid', { name: 'diciembre 2026' })).toBeInTheDocument()

    await user.click(screen.getByRole('gridcell', { name: '20 de diciembre de 2026' }))
    expect(screen.getByTestId('valor')).toHaveTextContent('2026-12-20')
    expect(screen.getByLabelText('Fecha')).toHaveTextContent('20/12/2026')
    expect(screen.queryByRole('grid')).not.toBeInTheDocument()
  })

  it('sin valor abre en el mes actual y marca el día de hoy', async () => {
    const user = userEvent.setup()
    render(<Ejemplo />)

    await user.click(screen.getByLabelText('Fecha'))
    expect(await screen.findByRole('grid', { name: 'noviembre 2026' })).toBeInTheDocument()
    expect(screen.getByRole('gridcell', { name: '16 de noviembre de 2026' })).toHaveAttribute('aria-current', 'date')
  })

  it('la semana empieza en lunes: el 1 de noviembre de 2026 (domingo) cae en la última columna', async () => {
    const user = userEvent.setup()
    render(<Ejemplo />)
    await user.click(screen.getByLabelText('Fecha'))
    const celdas = await screen.findAllByRole('gridcell')

    expect(celdas).toHaveLength(30)
    expect(celdas[0]).toHaveAccessibleName('1 de noviembre de 2026')
    // 6 huecos antes del domingo 1.
    const grilla = celdas[0].parentElement as HTMLElement
    expect(grilla.children[6]).toBe(celdas[0])
  })

  it('los días fuera de min y max no se pueden elegir', async () => {
    const user = userEvent.setup()
    render(<Ejemplo min="2026-11-17" max="2026-11-25" />)
    await user.click(screen.getByLabelText('Fecha'))

    expect(await screen.findByRole('gridcell', { name: '16 de noviembre de 2026' })).toBeDisabled()
    expect(screen.getByRole('gridcell', { name: '17 de noviembre de 2026' })).toBeEnabled()
    expect(screen.getByRole('gridcell', { name: '25 de noviembre de 2026' })).toBeEnabled()
    expect(screen.getByRole('gridcell', { name: '26 de noviembre de 2026' })).toBeDisabled()

    await user.click(screen.getByRole('gridcell', { name: '16 de noviembre de 2026' }))
    expect(screen.getByTestId('valor')).toBeEmptyDOMElement()
  })

  it('no deja navegar a meses sin días elegibles', async () => {
    const user = userEvent.setup()
    render(<Ejemplo min="2026-11-17" max="2026-11-25" />)
    await user.click(screen.getByLabelText('Fecha'))

    expect(await screen.findByRole('button', { name: 'Mes anterior' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Mes siguiente' })).toBeDisabled()
  })

  it('navega entre meses con las flechas', async () => {
    const user = userEvent.setup()
    render(<Ejemplo />)
    await user.click(screen.getByLabelText('Fecha'))

    await user.click(await screen.findByRole('button', { name: 'Mes siguiente' }))
    expect(screen.getByRole('grid', { name: 'diciembre 2026' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Mes anterior' }))
    await user.click(screen.getByRole('button', { name: 'Mes anterior' }))
    expect(screen.getByRole('grid', { name: 'octubre 2026' })).toBeInTheDocument()
  })

  it('cambia de mes y de año desde la vista de meses', async () => {
    const user = userEvent.setup()
    render(<Ejemplo />)
    await user.click(screen.getByLabelText('Fecha'))

    await user.click(await screen.findByRole('button', { name: /noviembre 2026/ }))
    await user.click(screen.getByRole('button', { name: 'Año siguiente' }))
    await user.click(screen.getByRole('button', { name: 'marzo 2027' }))

    expect(screen.getByRole('grid', { name: 'marzo 2027' })).toBeInTheDocument()
  })

  it('«Hoy» elige el día de hoy y desaparece si hoy no es elegible', async () => {
    const user = userEvent.setup()
    const { unmount } = render(<Ejemplo />)
    await user.click(screen.getByLabelText('Fecha'))
    await user.click(await screen.findByRole('button', { name: 'Hoy' }))
    expect(screen.getByTestId('valor')).toHaveTextContent('2026-11-16')
    unmount()

    render(<Ejemplo min="2026-11-17" />)
    await user.click(screen.getByLabelText('Fecha'))
    await screen.findByRole('grid')
    expect(screen.queryByRole('button', { name: 'Hoy' })).not.toBeInTheDocument()
  })

  it('«Borrar» vacía el campo solo con clearable', async () => {
    const user = userEvent.setup()
    render(<Ejemplo inicial="2026-11-20" clearable />)
    await user.click(screen.getByLabelText('Fecha'))
    await user.click(await screen.findByRole('button', { name: 'Borrar' }))

    expect(screen.getByTestId('valor')).toBeEmptyDOMElement()
    expect(screen.getByLabelText('Fecha')).toHaveTextContent('dd/mm/aaaa')
  })

  it('se mueve con las flechas del teclado y elige con Enter', async () => {
    const user = userEvent.setup()
    render(<Ejemplo />)
    await user.click(screen.getByLabelText('Fecha'))
    const hoy = await screen.findByRole('gridcell', { name: '16 de noviembre de 2026' })
    hoy.focus()

    await user.keyboard('{ArrowRight}{ArrowDown}')
    expect(screen.getByRole('gridcell', { name: '24 de noviembre de 2026' })).toHaveFocus()
    await user.keyboard('{Enter}')

    expect(screen.getByTestId('valor')).toHaveTextContent('2026-11-24')
  })

  it('avisa con onBlur al cerrarse', async () => {
    const user = userEvent.setup()
    const alSalir = vi.fn()
    render(<Ejemplo onBlur={alSalir} />)
    await user.click(screen.getByLabelText('Fecha'))
    await screen.findByRole('grid')
    expect(alSalir).not.toHaveBeenCalled()

    await user.keyboard('{Escape}')
    expect(alSalir).toHaveBeenCalledTimes(1)
  })
})
