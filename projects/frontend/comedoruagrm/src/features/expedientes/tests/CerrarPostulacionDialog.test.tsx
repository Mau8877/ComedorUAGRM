import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/store', async (original) => ({
  ...(await original<object>()),
  apiClient: (await import('../../finalistas/tests/clienteSimulado')).cliente,
}))
vi.mock('@/features/auth', () => ({
  AlertaError: ({ children }: { children: ReactNode }) => <div role="alert">{children}</div>,
}))

import { ApiError } from '@/store'

import { cliente } from '../../finalistas/tests/clienteSimulado'
import { CerrarPostulacionDialog } from '../components'
import type { AccionCierre } from '../types'

const CASOS: Array<{ accion: AccionCierre; boton: string; titulo: string; ruta: string; estado: string }> = [
  { accion: 'archivar', boton: 'Archivar', titulo: 'Archivar postulación', ruta: '/finalistas/f-1/archivar', estado: 'archivada' },
  { accion: 'rechazar', boton: 'Rechazar', titulo: 'Rechazar postulación', ruta: '/finalistas/f-1/rechazar', estado: 'no_adjudicada' },
]

function resultado(estado: string) {
  return { postulacion: 'p-1', estado, motivo: 'No entregó', fecha: 't', decididaPor: 'Ana' }
}

function montar(accion: AccionCierre, props: Partial<React.ComponentProps<typeof CerrarPostulacionDialog>> = {}) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const onOpenChange = vi.fn()
  render(
    <QueryClientProvider client={queryClient}>
      <CerrarPostulacionDialog
        open
        onOpenChange={onOpenChange}
        accion={accion}
        finalista="f-1"
        postulante="Luis Pérez"
        convocatoria="Beca Comedor 2026"
        {...props}
      />
    </QueryClientProvider>,
  )
  return { onOpenChange }
}

describe.each(CASOS)('CerrarPostulacionDialog (%s)', ({ accion, boton, titulo, ruta, estado }) => {
  beforeEach(() => {
    cliente.post.mockReset()
  })

  it('muestra el título de la acción, a quién afecta, el aviso de que es definitivo y el contador de 500', () => {
    montar(accion)

    expect(screen.getByText(titulo)).toBeInTheDocument()
    expect(screen.getByText('Luis Pérez')).toBeInTheDocument()
    expect(screen.getByText('Beca Comedor 2026')).toBeInTheDocument()
    expect(screen.getByText(/definitiva para esta gestión/)).toBeInTheDocument()
    expect(screen.getByText('0/500')).toBeInTheDocument()
    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument()
  })

  it('cuenta los caracteres a medida que se escribe', async () => {
    const usuario = userEvent.setup()
    montar(accion)

    await usuario.type(screen.getByLabelText('Motivo'), 'Hola')

    expect(screen.getByText('4/500')).toBeInTheDocument()
  })

  it('sin motivo no envía y señala el campo', async () => {
    const usuario = userEvent.setup()
    montar(accion)

    await usuario.click(screen.getByRole('button', { name: boton }))

    expect(await screen.findByText('El motivo es obligatorio')).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('con solo espacios no envía', async () => {
    const usuario = userEvent.setup()
    montar(accion)

    await usuario.type(screen.getByLabelText('Motivo'), '   ')
    await usuario.click(screen.getByRole('button', { name: boton }))

    expect(await screen.findByText('El motivo es obligatorio')).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('con más de 500 caracteres no envía', async () => {
    const usuario = userEvent.setup()
    montar(accion)

    await usuario.click(screen.getByLabelText('Motivo'))
    await usuario.paste('x'.repeat(501))
    await usuario.click(screen.getByRole('button', { name: boton }))

    expect(await screen.findByText('El motivo no puede pasar de 500 caracteres')).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('envía el motivo, avisa a la pantalla y cierra el diálogo', async () => {
    cliente.post.mockResolvedValue({ data: resultado(estado) })
    const usuario = userEvent.setup()
    const onCerrada = vi.fn()
    const { onOpenChange } = montar(accion, { onCerrada })

    await usuario.type(screen.getByLabelText('Motivo'), '  No entregó ')
    await usuario.click(screen.getByRole('button', { name: boton }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalledWith(ruta, { motivo: 'No entregó' }))
    await waitFor(() => expect(onCerrada).toHaveBeenCalledWith(resultado(estado)))
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('deshabilita el botón mientras se envía y no manda dos peticiones', async () => {
    let resolver: (valor: unknown) => void = () => undefined
    cliente.post.mockReturnValue(new Promise((r) => (resolver = r)))
    const usuario = userEvent.setup()
    montar(accion)

    await usuario.type(screen.getByLabelText('Motivo'), 'No entregó')
    await usuario.click(screen.getByRole('button', { name: boton }))
    await waitFor(() => expect(screen.getByRole('button', { name: new RegExp(boton) })).toBeDisabled())
    await usuario.click(screen.getByRole('button', { name: new RegExp(boton) }))

    expect(cliente.post).toHaveBeenCalledTimes(1)
    resolver({ data: resultado(estado) })
  })

  it('cancelar cierra sin llamar a la API', async () => {
    const usuario = userEvent.setup()
    const { onOpenChange } = montar(accion)

    await usuario.type(screen.getByLabelText('Motivo'), 'No entregó')
    await usuario.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(onOpenChange).toHaveBeenCalledWith(false)
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('ante ERR_ARC_01 muestra el mensaje, pide refrescar el expediente y deja abierto el diálogo con lo escrito', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_ARC_01', 'x'))
    const usuario = userEvent.setup()
    const onDesactualizado = vi.fn()
    const { onOpenChange } = montar(accion, { onDesactualizado })

    await usuario.type(screen.getByLabelText('Motivo'), 'No entregó')
    await usuario.click(screen.getByRole('button', { name: boton }))

    expect(await screen.findByRole('alert')).toHaveTextContent('ya no está calificada')
    expect(onDesactualizado).toHaveBeenCalledTimes(1)
    expect(screen.getByLabelText('Motivo')).toHaveValue('No entregó')
    expect(onOpenChange).not.toHaveBeenCalledWith(false)
  })

  it('si el finalista ya no existe avisa a la pantalla y cierra', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_FIN_01', 'x'))
    const usuario = userEvent.setup()
    const onFinalistaInexistente = vi.fn()
    const { onOpenChange } = montar(accion, { onFinalistaInexistente })

    await usuario.type(screen.getByLabelText('Motivo'), 'No entregó')
    await usuario.click(screen.getByRole('button', { name: boton }))

    await waitFor(() => expect(onFinalistaInexistente).toHaveBeenCalledTimes(1))
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('ante otro error lo muestra dentro de la ventana', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_AUTH_07', 'x'))
    const usuario = userEvent.setup()
    montar(accion)

    await usuario.type(screen.getByLabelText('Motivo'), 'No entregó')
    await usuario.click(screen.getByRole('button', { name: boton }))

    expect(await screen.findByRole('alert')).toHaveTextContent('No tienes permiso')
  })
})
