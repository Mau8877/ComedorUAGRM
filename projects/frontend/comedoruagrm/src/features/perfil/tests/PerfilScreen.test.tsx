import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError } from 'axios'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const navigate = vi.hoisted(() => vi.fn())
vi.mock('@tanstack/react-router', () => ({
  useNavigate: () => navigate,
  useLocation: () => '',
  Link: ({ to, children }: { to: string; children: ReactNode }) => <a href={to}>{children}</a>,
}))
vi.mock('../api/usePerfil', () => ({ usePerfil: vi.fn() }))
vi.mock('../api/useActualizarPerfil', () => ({ useActualizarPerfil: () => ({ mutate: vi.fn(), isPending: false }) }))
vi.mock('../api/useCambiarContrasena', () => ({ useCambiarContrasena: () => ({ mutate: vi.fn(), isPending: false }) }))
vi.mock('@/features/auth', async (original) => ({
  ...(await original<object>()),
  useCerrarSesion: () => ({ mutate: vi.fn() }),
}))
// Los layouts reales traen sidebar, reloj y notificaciones: acá solo importa a quién se le da cada acción.
vi.mock('@/layouts', () => ({
  AdminLayout: ({ title, onPerfil, children }: { title: string; onPerfil?: () => void; children: ReactNode }) => (
    <div data-testid="admin">
      <h1>{title}</h1>
      <button onClick={onPerfil}>ir-perfil</button>
      {children}
    </div>
  ),
  EstudianteLayout: ({
    title,
    activeHref,
    onCambiarContrasena,
    children,
  }: {
    title: string
    activeHref?: string
    onCambiarContrasena?: () => void
    children: ReactNode
  }) => (
    <div data-testid="estudiante" data-active={activeHref}>
      <h1>{title}</h1>
      <button onClick={onCambiarContrasena}>ir-contrasena</button>
      {children}
    </div>
  ),
}))

import { usePerfil } from '../api/usePerfil'
import { PerfilScreen } from '../screens/PerfilScreen'
import { tienePerfilWeb } from '../useAccionesDeCuenta'
import type { Perfil } from '../types'

const postulante: Perfil = {
  codigo: 'u1',
  nombres: 'Ana',
  apellidos: 'Perez',
  ci: '1234567',
  correo: 'ana@uagrm.edu.bo',
  rol: 'POSTULANTE',
  activo: true,
  postulante: {
    registroUniversitario: '219012345',
    carrera: { codigo: 'c1', nombre: 'Ingeniería Informática', facultad: 'Tecnología' },
    telefono: '70000001',
  },
}
const admin: Perfil = { ...postulante, codigo: 'u2', nombres: 'Luis', rol: 'ADMINISTRADOR_DUBSS', postulante: null }

function montar(perfil: Perfil, estado: Record<string, unknown> = {}) {
  vi.mocked(usePerfil).mockReturnValue({ isPending: false, isError: false, data: perfil, ...estado } as never)
  const usuario = { codigo: perfil.codigo, nombres: perfil.nombres, apellidos: perfil.apellidos, correo: perfil.correo, rol: perfil.rol }
  render(<PerfilScreen usuario={usuario} />)
  return userEvent.setup()
}

describe('PerfilScreen', () => {
  beforeEach(() => {
    navigate.mockReset()
    vi.mocked(usePerfil).mockReset()
  })

  it('el Postulante ve sus datos con registro universitario y carrera, dentro de su layout', () => {
    montar(postulante)

    expect(screen.getByTestId('estudiante')).toHaveAttribute('data-active', '/perfil')
    expect(screen.getByLabelText('Nombres')).toHaveValue('Ana')
    expect(screen.getByLabelText('Registro universitario')).toHaveValue('219012345')
    expect(screen.getByLabelText('Carrera')).toHaveValue('Ingeniería Informática (Tecnología)')
    expect(screen.getByLabelText('Rol')).toHaveValue('Postulante')
    expect(screen.getByLabelText('Estado de la cuenta')).toHaveValue('Activa')
  })

  it('el Administrador no ve registro universitario ni carrera y usa el layout de administrador', () => {
    montar(admin)

    expect(screen.getByTestId('admin')).toBeInTheDocument()
    expect(screen.getByLabelText('Rol')).toHaveValue('Administrador DUBSS')
    expect(screen.queryByLabelText('Registro universitario')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Carrera')).not.toBeInTheDocument()
  })

  it('el CI se muestra bloqueado, nunca como campo editable (A4)', () => {
    montar(postulante)

    const ci = screen.getByLabelText('CI')
    expect(ci).toHaveValue('1234567')
    expect(ci).toBeDisabled()
    expect(screen.getByText('El CI no se puede modificar.')).toBeInTheDocument()
  })

  it('«Mi perfil» y «Cambiar contraseña» del menú navegan a /perfil (la segunda, a su sección)', async () => {
    const usuarioSimulado = montar(admin)
    await usuarioSimulado.click(screen.getByRole('button', { name: 'ir-perfil' }))
    expect(navigate).toHaveBeenCalledWith({ to: '/perfil' })
    navigate.mockReset()

    document.body.innerHTML = ''
    const otro = montar(postulante)
    await otro.click(screen.getByRole('button', { name: 'ir-contrasena' }))
    expect(navigate).toHaveBeenCalledWith({ to: '/perfil', hash: 'cambiar-contrasena' })
  })

  it('mientras carga muestra un estado de carga y no los datos', () => {
    montar(postulante, { isPending: true, data: undefined })

    expect(screen.queryByLabelText('Nombres')).not.toBeInTheDocument()
    expect(document.querySelector('[aria-busy="true"]')).not.toBeNull()
  })

  it('un error de red muestra el aviso con «Reintentar» que vuelve a pedir el perfil (A10)', async () => {
    const refetch = vi.fn()
    const usuarioSimulado = montar(postulante, {
      isError: true,
      data: undefined,
      error: new AxiosError('Network Error', 'ERR_NETWORK'),
      refetch,
    })

    expect(screen.getByRole('alert')).toHaveTextContent('No se pudo conectar con el servidor')
    await usuarioSimulado.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(refetch).toHaveBeenCalledTimes(1)
  })
})

describe('tienePerfilWeb', () => {
  it('solo el Administrador DUBSS y el Postulante tienen perfil en la web (los demás no tienen layout todavía)', () => {
    expect(tienePerfilWeb('ADMINISTRADOR_DUBSS')).toBe(true)
    expect(tienePerfilWeb('POSTULANTE')).toBe(true)
    expect(tienePerfilWeb('DIRECTIVO')).toBe(false)
    expect(tienePerfilWeb('PERSONAL_COMEDOR')).toBe(false)
  })
})
