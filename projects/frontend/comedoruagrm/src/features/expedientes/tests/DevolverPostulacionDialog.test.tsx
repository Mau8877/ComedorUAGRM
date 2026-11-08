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
import { DevolverPostulacionDialog } from '../components'

const RESULTADO = { postulacion: 'p-1', estado: 'devuelta', motivo: 'Falta el carnet', fecha: 't', devueltaPor: 'Ana' }

function montar(props: Partial<React.ComponentProps<typeof DevolverPostulacionDialog>> = {}) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const onOpenChange = vi.fn()
  render(
    <QueryClientProvider client={queryClient}>
      <DevolverPostulacionDialog open onOpenChange={onOpenChange} finalista="f-1" {...props} />
    </QueryClientProvider>,
  )
  return { onOpenChange }
}

describe('DevolverPostulacionDialog', () => {
  beforeEach(() => {
    cliente.post.mockReset()
  })

  it('no ofrece la opción de no avisar al postulante y muestra el contador de 500', () => {
    montar()

    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument()
    expect(screen.getByText('0/500')).toBeInTheDocument()
    expect(screen.getByText(/bandeja, el push y el correo/)).toBeInTheDocument()
  })

  it('cuenta los caracteres a medida que se escribe', async () => {
    const usuario = userEvent.setup()
    montar()

    await usuario.type(screen.getByLabelText('Motivo'), 'Hola')

    expect(screen.getByText('4/500')).toBeInTheDocument()
  })

  it('sin motivo no envía y señala el campo', async () => {
    const usuario = userEvent.setup()
    montar()

    await usuario.click(screen.getByRole('button', { name: 'Devolver' }))

    expect(await screen.findByText('El motivo es obligatorio')).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('con más de 500 caracteres no envía', async () => {
    const usuario = userEvent.setup()
    montar()

    await usuario.click(screen.getByLabelText('Motivo'))
    await usuario.paste('x'.repeat(501))
    await usuario.click(screen.getByRole('button', { name: 'Devolver' }))

    expect(await screen.findByText('El motivo no puede pasar de 500 caracteres')).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('envía el motivo, avisa a la pantalla y cierra el diálogo', async () => {
    cliente.post.mockResolvedValue({ data: RESULTADO })
    const usuario = userEvent.setup()
    const onDevuelta = vi.fn()
    const { onOpenChange } = montar({ onDevuelta })

    await usuario.type(screen.getByLabelText('Motivo'), '  Falta el carnet ')
    await usuario.click(screen.getByRole('button', { name: 'Devolver' }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalledWith('/finalistas/f-1/devolver', { motivo: 'Falta el carnet' }))
    await waitFor(() => expect(onDevuelta).toHaveBeenCalledWith(RESULTADO))
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('deshabilita el botón mientras se envía y no manda dos peticiones', async () => {
    let resolver: (valor: unknown) => void = () => undefined
    cliente.post.mockReturnValue(new Promise((r) => (resolver = r)))
    const usuario = userEvent.setup()
    montar()

    await usuario.type(screen.getByLabelText('Motivo'), 'Falta el carnet')
    await usuario.click(screen.getByRole('button', { name: 'Devolver' }))
    await waitFor(() => expect(screen.getByRole('button', { name: /Devolver/ })).toBeDisabled())
    await usuario.click(screen.getByRole('button', { name: /Devolver/ }))

    expect(cliente.post).toHaveBeenCalledTimes(1)
    resolver({ data: RESULTADO })
  })

  it('ante un conflicto muestra el mensaje y deja abierto el diálogo con lo escrito', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_DEV_01', 'x'))
    const usuario = userEvent.setup()
    const { onOpenChange } = montar()

    await usuario.type(screen.getByLabelText('Motivo'), 'Falta el carnet')
    await usuario.click(screen.getByRole('button', { name: 'Devolver' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('ya no está calificada')
    expect(screen.getByLabelText('Motivo')).toHaveValue('Falta el carnet')
    expect(onOpenChange).not.toHaveBeenCalledWith(false)
  })

  it('si el finalista ya no existe avisa a la pantalla y cierra', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_FIN_01', 'x'))
    const usuario = userEvent.setup()
    const onFinalistaInexistente = vi.fn()
    const { onOpenChange } = montar({ onFinalistaInexistente })

    await usuario.type(screen.getByLabelText('Motivo'), 'Falta el carnet')
    await usuario.click(screen.getByRole('button', { name: 'Devolver' }))

    await waitFor(() => expect(onFinalistaInexistente).toHaveBeenCalledTimes(1))
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })
})
