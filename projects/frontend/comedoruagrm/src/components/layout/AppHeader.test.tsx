import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

vi.mock('./Breadcrumbs', () => ({ Breadcrumbs: () => null }))
vi.mock('./NotificationsMenu', () => ({ NotificationsMenu: () => null }))

import { AppHeader } from './AppHeader'

const user = { nombre: 'Ana Perez', rol: 'Postulante' }

describe('AppHeader', () => {
  it('muestra el nombre y el rol del usuario', () => {
    render(<AppHeader appName="ComedorU" user={user} />)

    expect(screen.getByText('Ana Perez')).toBeInTheDocument()
    expect(screen.getByText('Postulante')).toBeInTheDocument()
  })

  it('el ítem «Cerrar sesión» llama a onCerrarSesion', async () => {
    const onCerrarSesion = vi.fn()
    const usuario = userEvent.setup()
    render(<AppHeader appName="ComedorU" user={user} onCerrarSesion={onCerrarSesion} />)

    await usuario.click(screen.getByText('Ana Perez'))
    await usuario.click(await screen.findByText('Cerrar sesión'))

    expect(onCerrarSesion).toHaveBeenCalledTimes(1)
  })
})
