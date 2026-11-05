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

  it('el ítem «Mi perfil» llama a onPerfil', async () => {
    const onPerfil = vi.fn()
    const usuario = userEvent.setup()
    render(<AppHeader appName="ComedorU" user={user} onPerfil={onPerfil} />)

    await usuario.click(screen.getByText('Ana Perez'))
    await usuario.click(await screen.findByText('Mi perfil'))

    expect(onPerfil).toHaveBeenCalledTimes(1)
  })

  it('el ítem «Cambiar contraseña» llama a onCambiarContrasena', async () => {
    const onCambiarContrasena = vi.fn()
    const usuario = userEvent.setup()
    render(<AppHeader appName="ComedorU" user={user} onCambiarContrasena={onCambiarContrasena} />)

    await usuario.click(screen.getByText('Ana Perez'))
    await usuario.click(await screen.findByText('Cambiar contraseña'))

    expect(onCambiarContrasena).toHaveBeenCalledTimes(1)
  })
})
