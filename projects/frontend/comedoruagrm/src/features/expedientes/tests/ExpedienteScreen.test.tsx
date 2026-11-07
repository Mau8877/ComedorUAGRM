import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/store', async (original) => ({
  ...(await original<object>()),
  apiClient: (await import('../../finalistas/tests/clienteSimulado')).cliente,
}))
vi.mock('@/features/auth', async () => {
  const sim = await import('../../finalistas/tests/clienteSimulado')
  return {
    etiquetaRol: sim.etiquetaRolSimulada,
    useCerrarSesion: () => ({ mutate: sim.cerrarSesion }),
    AlertaError: ({ children }: { children: ReactNode }) => <div role="alert">{children}</div>,
  }
})
vi.mock('@/features/perfil', () => ({ useAccionesDeCuenta: () => ({}) }))
vi.mock('@/layouts', () => ({
  AdminLayout: ({ title, children }: { title: string; children: ReactNode }) => (
    <div>
      <h1>{title}</h1>
      {children}
    </div>
  ),
}))
vi.mock('@/components/ui/toast', async (original) => ({
  ...(await original<object>()),
  toastError: vi.fn(),
}))
// El enlace «Volver» usa el router; acá basta con un ancla.
vi.mock('@tanstack/react-router', () => ({
  Link: ({ to, children, ...resto }: { to: string; children: ReactNode }) => (
    <a href={to} {...resto}>
      {children}
    </a>
  ),
}))

import { toastError } from '@/components/ui/toast'
import { ApiError } from '@/store'

import { cliente } from '../../finalistas/tests/clienteSimulado'
import { ExpedienteScreen } from '../screens/ExpedienteScreen'
import { auditoria, expediente } from './fixtures'

const yo = { codigo: 'yo', nombres: 'Admin', apellidos: 'Principal', correo: 'admin@dubss.com', rol: 'ADMINISTRADOR_DUBSS' }

function montar(onFinalistaInexistente?: () => void) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  render(
    <QueryClientProvider client={queryClient}>
      <ExpedienteScreen
        titulo="Auditar expediente"
        activeHref="/finalistas"
        usuarioActual={yo}
        codigo="f-1"
        onFinalistaInexistente={onFinalistaInexistente}
      />
    </QueryClientProvider>,
  )
}

describe('ExpedienteScreen', () => {
  beforeEach(() => {
    cliente.get.mockReset()
    cliente.post.mockReset()
    vi.mocked(toastError).mockClear()
  })

  it('arma la vista con resumen, documentos, puntaje, formulario e historial (CA01)', async () => {
    cliente.get.mockResolvedValue({ data: expediente() })
    montar()

    expect(await screen.findByText('Luis Pérez')).toBeInTheDocument()
    expect(cliente.get).toHaveBeenCalledWith('/finalistas/f-1/expediente')
    expect(screen.getByText('Documentos digitales')).toBeInTheDocument()
    expect(screen.getByText('80.50')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Registrar auditoría' })).toBeEnabled()
    expect(screen.getByText('Historial de auditorías')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Volver a finalistas/ })).toHaveAttribute('href', '/finalistas')
  })

  it('con la entrega pendiente deshabilita la auditoría y explica por qué', async () => {
    const e = expediente({ auditorias: [] })
    e.finalista.entrega = { estado: 'PENDIENTE', fecha: null }
    cliente.get.mockResolvedValue({ data: e })
    montar()

    await screen.findByText('Luis Pérez')
    expect(screen.getByRole('button', { name: 'Registrar auditoría' })).toBeDisabled()
    expect(screen.getByText('Sin auditorías todavía.')).toBeInTheDocument()
    expect(screen.getAllByText(/Primero registra la entrega/).length).toBeGreaterThan(0)
  })

  it('si el finalista ya no existe avisa con un toast, vuelve a la lista y no ofrece reintentar (A11)', async () => {
    cliente.get.mockRejectedValue(new ApiError('ERR_FIN_01', 'x'))
    const volver = vi.fn()
    montar(volver)

    expect(await screen.findByRole('alert')).toHaveTextContent('El finalista ya no existe.')
    await waitFor(() => expect(volver).toHaveBeenCalledTimes(1))
    expect(toastError).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument()
  })

  it('ante un error de red ofrece reintentar', async () => {
    cliente.get.mockRejectedValueOnce(new ApiError('ERR_SYS_00', 'x'))
    cliente.get.mockResolvedValueOnce({ data: expediente({ auditorias: [auditoria()] }) })
    const usuario = userEvent.setup()
    montar()

    await usuario.click(await screen.findByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByText('Luis Pérez')).toBeInTheDocument()
  })
})
