import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './select'

function Ejemplo({ cantidad }: { cantidad: number }) {
  const opciones = Array.from({ length: cantidad }, (_, i) => ({
    valor: `v${i}`,
    nombre: i === 0 ? 'Ingeniería Informática' : `Carrera ${i}`,
  }))
  return (
    <Select>
      <SelectTrigger aria-label="Carrera">
        <SelectValue placeholder="Selecciona" />
      </SelectTrigger>
      <SelectContent>
        {opciones.map((o) => (
          <SelectItem key={o.valor} value={o.valor}>
            {o.nombre}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}

describe('Select', () => {
  it('no muestra buscador con pocas opciones', async () => {
    render(<Ejemplo cantidad={5} />)
    await userEvent.click(screen.getByRole('combobox', { name: 'Carrera' }))
    expect(await screen.findAllByRole('option')).toHaveLength(5)
    expect(screen.queryByLabelText('Buscar opciones')).not.toBeInTheDocument()
  })

  it('muestra buscador y filtra (sin distinguir tildes ni mayúsculas) con muchas opciones', async () => {
    render(<Ejemplo cantidad={12} />)
    await userEvent.click(screen.getByRole('combobox', { name: 'Carrera' }))
    const buscador = await screen.findByLabelText('Buscar opciones')
    await userEvent.type(buscador, 'INGENIERIA')
    const opciones = screen.getAllByRole('option')
    expect(opciones).toHaveLength(1)
    expect(opciones[0]).toHaveTextContent('Ingeniería Informática')
  })

  it('avisa cuando la búsqueda no tiene resultados', async () => {
    render(<Ejemplo cantidad={12} />)
    await userEvent.click(screen.getByRole('combobox', { name: 'Carrera' }))
    await userEvent.type(await screen.findByLabelText('Buscar opciones'), 'zzz')
    expect(screen.queryAllByRole('option')).toHaveLength(0)
    expect(screen.getByText('Sin resultados')).toBeInTheDocument()
  })
})
