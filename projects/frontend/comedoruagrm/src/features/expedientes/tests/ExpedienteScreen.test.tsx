import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
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

    expect((await screen.findAllByText('Luis Pérez')).length).toBeGreaterThan(0)
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

    await screen.findAllByText('Luis Pérez')
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

    expect((await screen.findAllByText('Luis Pérez')).length).toBeGreaterThan(0)
  })

  it('con la postulación calificada y una auditoría el botón «Devolver para corrección» está habilitado (CU19)', async () => {
    cliente.get.mockResolvedValue({ data: expediente() })
    montar()

    expect(await screen.findByRole('button', { name: 'Devolver para corrección' })).toBeEnabled()
  })

  it('sin auditorías deshabilita la devolución y explica por qué (CU19)', async () => {
    cliente.get.mockResolvedValue({ data: expediente({ auditorias: [] }) })
    montar()

    expect(await screen.findByRole('button', { name: 'Devolver para corrección' })).toBeDisabled()
    expect(screen.getAllByText('Primero registra la auditoría del expediente.').length).toBeGreaterThan(0)
  })

  it('con la postulación devuelta deshabilita la devolución y muestra la insignia «Devuelta» (CU19)', async () => {
    cliente.get.mockResolvedValue({ data: expediente({ estadoPostulacion: 'devuelta' }) })
    montar()

    expect(await screen.findByRole('button', { name: 'Devolver para corrección' })).toBeDisabled()
    expect(screen.getByText('La postulación ya está devuelta.')).toBeInTheDocument()
    expect(screen.getByText('Devuelta')).toBeInTheDocument()
  })

  it('devolver abre el diálogo, manda el motivo y refresca el expediente (CU19)', async () => {
    cliente.get.mockResolvedValue({ data: expediente() })
    cliente.post.mockResolvedValue({
      data: { postulacion: 'p-1', estado: 'devuelta', motivo: 'Falta la luz', fecha: 't', devueltaPor: 'Admin' },
    })
    const usuario = userEvent.setup()
    montar()

    await usuario.click(await screen.findByRole('button', { name: 'Devolver para corrección' }))
    await usuario.type(await screen.findByLabelText('Motivo'), 'Falta la luz')
    await usuario.click(screen.getByRole('button', { name: 'Devolver' }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalledWith('/finalistas/f-1/devolver', { motivo: 'Falta la luz' }))
    await waitFor(() => expect(cliente.get.mock.calls.length).toBeGreaterThan(1))
  })

  it('con la postulación calificada habilita «Archivar» y «Rechazar», aun sin auditorías ni entrega (CU20)', async () => {
    cliente.get.mockResolvedValue({
      data: expediente({
        auditorias: [],
        finalista: { codigo: 'f-1', posicion: 1, plazoEntrega: null, entrega: { estado: 'PENDIENTE', fecha: null } },
      }),
    })
    montar()

    expect(await screen.findByRole('button', { name: 'Archivar' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Rechazar' })).toBeEnabled()
  })

  it.each([
    ['archivada', 'La postulación ya está archivada.', 'Archivada'],
    ['no_adjudicada', 'La postulación ya fue marcada como no adjudicada.', 'No adjudicada'],
  ])('con la postulación %s deshabilita ambas acciones, explica por qué y muestra la insignia (CU20)', async (estado, nota, insignia) => {
    cliente.get.mockResolvedValue({ data: expediente({ estadoPostulacion: estado }) })
    montar()

    expect(await screen.findByRole('button', { name: 'Archivar' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Rechazar' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Devolver para corrección' })).toBeDisabled()
    expect(screen.getByText(nota)).toBeInTheDocument()
    expect(screen.getAllByText(insignia).length).toBeGreaterThan(0)
    // La auditoría queda bloqueada: ya no está calificada.
    expect(screen.getByRole('button', { name: 'Registrar auditoría' })).toBeDisabled()
  })

  it('con la postulación devuelta también deshabilita «Archivar» y «Rechazar» (CU20)', async () => {
    cliente.get.mockResolvedValue({ data: expediente({ estadoPostulacion: 'devuelta' }) })
    montar()

    expect(await screen.findByRole('button', { name: 'Archivar' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Rechazar' })).toBeDisabled()
  })

  it.each([
    ['Archivar', 'archivar', 'archivada'],
    ['Rechazar', 'rechazar', 'no_adjudicada'],
  ])('«%s» abre la ventana con el postulante, manda el motivo y refresca el expediente (CU20)', async (nombre, ruta, estado) => {
    cliente.get.mockResolvedValue({ data: expediente() })
    cliente.post.mockResolvedValue({
      data: { postulacion: 'p-1', estado, motivo: 'No entregó', fecha: 't', decididaPor: 'Admin' },
    })
    const usuario = userEvent.setup()
    montar()

    await usuario.click(await screen.findByRole('button', { name: nombre }))
    expect(await screen.findByText(/definitiva para esta gestión/)).toBeInTheDocument()
    await usuario.type(screen.getByLabelText('Motivo'), 'No entregó')
    await usuario.click(within(screen.getByRole('dialog')).getByRole('button', { name: nombre }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalledWith(`/finalistas/f-1/${ruta}`, { motivo: 'No entregó' }))
    await waitFor(() => expect(cliente.get.mock.calls.length).toBeGreaterThan(1))
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })

  it('cancelar la ventana no cambia nada (CU20)', async () => {
    cliente.get.mockResolvedValue({ data: expediente() })
    const usuario = userEvent.setup()
    montar()

    await usuario.click(await screen.findByRole('button', { name: 'Archivar' }))
    await usuario.type(await screen.findByLabelText('Motivo'), 'No entregó')
    await usuario.click(screen.getByRole('button', { name: 'Cancelar' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('si al cerrar el backend dice que la postulación ya no está calificada, refresca el expediente (CU20)', async () => {
    cliente.get.mockResolvedValue({ data: expediente() })
    cliente.post.mockRejectedValue(new ApiError('ERR_ARC_01', 'x'))
    const usuario = userEvent.setup()
    montar()

    await usuario.click(await screen.findByRole('button', { name: 'Rechazar' }))
    await usuario.type(await screen.findByLabelText('Motivo'), 'No entregó')
    await usuario.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Rechazar' }))

    await waitFor(() => expect(cliente.get.mock.calls.length).toBeGreaterThan(1))
    expect(await screen.findByRole('dialog')).toHaveTextContent('ya no está calificada')
  })
})
