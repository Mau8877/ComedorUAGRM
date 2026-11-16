import { render, screen } from '@testing-library/react'
import { UsersIcon } from 'lucide-react'
import { describe, expect, it } from 'vitest'

import { EncabezadoPagina } from './EncabezadoPagina'

describe('EncabezadoPagina', () => {
  it('muestra el título como h1', () => {
    render(<EncabezadoPagina titulo="Directivos" />)
    expect(screen.getByRole('heading', { level: 1, name: 'Directivos' })).toBeInTheDocument()
  })

  it('renderiza las migas y marca la última como página actual', () => {
    render(
      <EncabezadoPagina
        icono={UsersIcon}
        titulo="Directivos"
        migas={[{ label: 'Admin' }, { label: 'Usuarios' }, { label: 'Directivos' }]}
      />,
    )
    const nav = screen.getByRole('navigation', { name: 'Ruta de la página' })
    expect(nav).toHaveTextContent('Admin')
    expect(nav).toHaveTextContent('Usuarios')
    expect(nav.querySelector('[aria-current="page"]')).toHaveTextContent('Directivos')
  })

  it('no dibuja el breadcrumb si no hay migas', () => {
    render(<EncabezadoPagina titulo="Actas" />)
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument()
  })

  it('usa enlace solo en los tramos con href', () => {
    render(<EncabezadoPagina titulo="X" migas={[{ label: 'Inicio', href: '/panel' }, { label: 'X' }]} />)
    expect(screen.getByRole('link', { name: 'Inicio' })).toHaveAttribute('href', '/panel')
  })
})

describe('EncabezadoPagina alineación', () => {
  it('con ícono, la casa ocupa la misma columna que el ícono grande', () => {
    const { container } = render(<EncabezadoPagina icono={UsersIcon} titulo="Actas" migas={[{ label: 'Actas' }]} />)
    const casa = container.querySelector('ol li')
    expect(casa?.className).toContain('w-7')
    expect(casa?.className).toContain('justify-center')
  })
})
