import { render, screen } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'

const montajes = vi.hoisted(() => ({ marco: 0 }))

vi.mock('@tanstack/react-router', async (original) => ({
  ...(await original<object>()),
  useNavigate: () => vi.fn(),
}))
vi.mock('@/features/notificaciones', () => ({ useNotificaciones: () => ({}) }))
// El AppShell real arrastra el router y el menú lateral: acá solo importa si el marco se remonta o no.
vi.mock('@/components/layout', async (original) => {
  const { useEffect } = await import('react')
  return {
    ...(await original<object>()),
    AppShell: ({
      appName,
      title,
      activeHref,
      children,
    }: {
      appName: string
      title: string
      activeHref?: string
      children: ReactNode
    }) => {
      useEffect(() => {
        montajes.marco += 1
      }, [])
      return (
        <div data-testid="marco" data-app={appName} data-active={activeHref}>
          <h1>{title}</h1>
          {children}
        </div>
      )
    },
  }
})

import { AdminLayout } from '../admin/AdminLayout'
import { EstudianteLayout } from '../estudiante/EstudianteLayout'
import { ShellDeRol } from './ShellDeRol'

const user = { nombre: 'Ana Perez', rol: 'Administrador' }

function shell(rol: string, pantalla: ReactNode) {
  return (
    <ShellDeRol rol={rol} user={user} onCerrarSesion={vi.fn()} onPerfil={vi.fn()} onCambiarContrasena={vi.fn()}>
      {pantalla}
    </ShellDeRol>
  )
}

describe('ShellDeRol', () => {
  it('mantiene montado el marco al cambiar de pantalla y solo cambia título, ítem activo y contenido', () => {
    montajes.marco = 0
    const { rerender } = render(
      shell(
        'ADMINISTRADOR_DUBSS',
        <AdminLayout title="Actas" activeHref="/actas" user={user}>
          <p>contenido actas</p>
        </AdminLayout>,
      ),
    )
    expect(screen.getByRole('heading', { name: 'Actas' })).toBeInTheDocument()
    expect(screen.getByTestId('marco')).toHaveAttribute('data-active', '/actas')

    rerender(
      shell(
        'ADMINISTRADOR_DUBSS',
        <AdminLayout title="Finalistas" activeHref="/finalistas" user={user}>
          <p>contenido finalistas</p>
        </AdminLayout>,
      ),
    )
    expect(screen.getByRole('heading', { name: 'Finalistas' })).toBeInTheDocument()
    expect(screen.getByText('contenido finalistas')).toBeInTheDocument()
    expect(screen.queryByText('contenido actas')).not.toBeInTheDocument()
    expect(screen.getAllByTestId('marco')).toHaveLength(1)
    expect(montajes.marco).toBe(1)
  })

  it('usa el layout del rol del usuario y dibuja el marco una sola vez aunque la pantalla traiga su layout', () => {
    render(
      shell(
        'POSTULANTE',
        <EstudianteLayout title="Resultados" activeHref="/resultados" user={user}>
          <p>mis resultados</p>
        </EstudianteLayout>,
      ),
    )
    expect(screen.getAllByTestId('marco')).toHaveLength(1)
    expect(screen.getByTestId('marco')).toHaveAttribute('data-app', 'ComedorU')
    expect(screen.getByText('mis resultados')).toBeInTheDocument()
  })

  it('el Personal del Comedor recibe su marco', () => {
    render(shell('PERSONAL_COMEDOR', <p>mi panel</p>))
    expect(screen.getByTestId('marco')).toHaveAttribute('data-app', 'ComedorU Personal')
    expect(screen.getByText('mi panel')).toBeInTheDocument()
  })

  it('un rol sin layout web no recibe marco', () => {
    render(shell('ROL_DESCONOCIDO', <p>aviso</p>))
    expect(screen.queryByTestId('marco')).not.toBeInTheDocument()
    expect(screen.getByText('aviso')).toBeInTheDocument()
  })
})
