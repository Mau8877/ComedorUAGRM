import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/store', async (original) => ({
  ...(await original<object>()),
  apiClient: (await import('../../finalistas/tests/clienteSimulado')).cliente,
}))

import { ApiError } from '@/store'

import { cliente } from '../../finalistas/tests/clienteSimulado'
import { AuditoriaForm, motivoBloqueoAuditoria } from '../components'
import { auditoria, expediente } from './fixtures'

function montar(props: Partial<React.ComponentProps<typeof AuditoriaForm>> = {}) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuditoriaForm finalista="f-1" {...props} />
    </QueryClientProvider>,
  )
}

describe('AuditoriaForm', () => {
  beforeEach(() => {
    cliente.post.mockReset()
  })

  it('«Validado» pide puntaje y, tras confirmar, manda el POST sin observaciones vacías', async () => {
    cliente.post.mockResolvedValue({ data: { auditoria: auditoria(), creada: true } })
    const usuario = userEvent.setup()
    montar()

    expect(screen.queryByLabelText(/Puntaje académico/)).not.toBeInTheDocument()
    await usuario.click(screen.getByRole('radio', { name: 'Validado' }))
    await usuario.type(screen.getByLabelText(/Puntaje académico/), '22,5')
    await usuario.click(screen.getByRole('button', { name: 'Registrar auditoría' }))
    await usuario.click(await screen.findByRole('button', { name: 'Confirmar' }))

    await waitFor(() =>
      expect(cliente.post).toHaveBeenCalledWith(
        '/finalistas/f-1/auditorias',
        { resultado: 'VALIDADO', puntajeAcademico: 22.5 },
        { headers: { 'Idempotency-Key': expect.stringMatching(/^[0-9a-f-]{36}$/) } },
      ),
    )
  })

  it('reintentar lo mismo tras un fallo reutiliza la clave; tras un éxito se genera otra', async () => {
    cliente.post.mockRejectedValueOnce(new ApiError('ERR_SYS_00', 'x'))
    cliente.post.mockResolvedValue({ data: { auditoria: auditoria(), creada: true } })
    const usuario = userEvent.setup()
    montar()
    const claves = () => cliente.post.mock.calls.map((llamada) => llamada[2].headers['Idempotency-Key'])
    const enviar = async () => {
      await usuario.click(screen.getByRole('button', { name: 'Registrar auditoría' }))
      await usuario.click(await screen.findByRole('button', { name: 'Confirmar' }))
    }

    await usuario.click(screen.getByRole('radio', { name: 'Observado' }))
    await usuario.type(screen.getByLabelText('Observaciones'), 'Falta el carnet')
    await enviar()
    await screen.findByRole('alert')
    await enviar()
    await waitFor(() => expect(cliente.post).toHaveBeenCalledTimes(2))
    expect(claves()[1]).toBe(claves()[0])

    await usuario.click(screen.getByRole('radio', { name: 'Observado' }))
    await usuario.type(screen.getByLabelText('Observaciones'), 'Falta el carnet')
    await enviar()
    await waitFor(() => expect(cliente.post).toHaveBeenCalledTimes(3))
    expect(claves()[2]).not.toBe(claves()[0])
  })

  it('con otro contenido tras un fallo usa una clave nueva', async () => {
    cliente.post.mockRejectedValueOnce(new ApiError('ERR_SYS_00', 'x'))
    cliente.post.mockResolvedValue({ data: { auditoria: auditoria(), creada: true } })
    const usuario = userEvent.setup()
    montar()
    const claves = () => cliente.post.mock.calls.map((llamada) => llamada[2].headers['Idempotency-Key'])

    await usuario.click(screen.getByRole('radio', { name: 'Observado' }))
    await usuario.type(screen.getByLabelText('Observaciones'), 'Falta el carnet')
    await usuario.click(screen.getByRole('button', { name: 'Registrar auditoría' }))
    await usuario.click(await screen.findByRole('button', { name: 'Confirmar' }))
    await screen.findByRole('alert')

    await usuario.type(screen.getByLabelText('Observaciones'), ' y el certificado')
    await usuario.click(screen.getByRole('button', { name: 'Registrar auditoría' }))
    await usuario.click(await screen.findByRole('button', { name: 'Confirmar' }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalledTimes(2))
    expect(claves()[1]).not.toBe(claves()[0])
  })

  it('si el finalista ya no existe avisa al llamador en vez de mostrar un error', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_FIN_01', 'x'))
    const onFinalistaInexistente = vi.fn()
    const usuario = userEvent.setup()
    montar({ onFinalistaInexistente })

    await usuario.click(screen.getByRole('radio', { name: 'Observado' }))
    await usuario.type(screen.getByLabelText('Observaciones'), 'Falta el carnet')
    await usuario.click(screen.getByRole('button', { name: 'Registrar auditoría' }))
    await usuario.click(await screen.findByRole('button', { name: 'Confirmar' }))

    await waitFor(() => expect(onFinalistaInexistente).toHaveBeenCalledTimes(1))
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('«Observado» exige observaciones y no manda nada sin ellas', async () => {
    const usuario = userEvent.setup()
    montar()

    await usuario.click(screen.getByRole('radio', { name: 'Observado' }))
    await usuario.click(screen.getByRole('button', { name: 'Registrar auditoría' }))

    expect(await screen.findByText(/Las observaciones son obligatorias/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Confirmar' })).not.toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('muestra el contador de caracteres de las observaciones', async () => {
    const usuario = userEvent.setup()
    montar()

    await usuario.type(screen.getByLabelText('Observaciones'), 'hola')

    expect(screen.getByText('4/500')).toBeInTheDocument()
  })

  it('con un bloqueo deshabilita el formulario y muestra el motivo', () => {
    montar({ bloqueo: 'Primero registra la entrega de la documentación física.' })

    expect(screen.getByRole('note')).toHaveTextContent('Primero registra la entrega')
    expect(screen.getByRole('button', { name: 'Registrar auditoría' })).toBeDisabled()
    expect(screen.getByRole('radio', { name: 'Validado' })).toHaveAttribute('aria-disabled', 'true')
  })

  it('si el backend responde ERR_EXP_01 deja el formulario bloqueado con el motivo', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_EXP_01', 'x'))
    const usuario = userEvent.setup()
    montar()

    await usuario.click(screen.getByRole('radio', { name: 'Observado' }))
    await usuario.type(screen.getByLabelText('Observaciones'), 'Falta el carnet')
    await usuario.click(screen.getByRole('button', { name: 'Registrar auditoría' }))
    await usuario.click(await screen.findByRole('button', { name: 'Confirmar' }))

    expect(await screen.findByRole('note')).toHaveTextContent('Primero registra la entrega')
    expect(screen.getByRole('button', { name: 'Registrar auditoría' })).toBeDisabled()
  })
})

describe('motivoBloqueoAuditoria', () => {
  it('no bloquea una postulación calificada con la entrega recibida', () => {
    expect(motivoBloqueoAuditoria(expediente())).toBeNull()
  })

  it('bloquea si la postulación ya no está calificada', () => {
    expect(motivoBloqueoAuditoria(expediente({ estadoPostulacion: 'devuelta' }))).toBe(
      'La postulación ya no está calificada.',
    )
  })

  it('bloquea si la entrega física sigue pendiente', () => {
    const e = expediente()
    e.finalista.entrega = { estado: 'PENDIENTE', fecha: null }
    expect(motivoBloqueoAuditoria(e)).toBe('Primero registra la entrega de la documentación física.')
  })
})
