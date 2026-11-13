import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/store', async (original) => ({
  ...(await original<object>()),
  apiClient: (await import('./clienteSimulado')).cliente,
}))

import { ComunicadoForm } from '@/features/comunicados'

import { NotificarFinalistasDialog } from '../components/NotificarFinalistasDialog'
import { RegistrarEntregaPanel } from '../components/RegistrarEntregaPanel'
import { hoyIso, mananaIso } from '../schemas'
import type { Finalista } from '../types'
import { cliente } from './clienteSimulado'

const FINALISTA: Finalista = {
  codigo: 'fin-1',
  posicion: 1,
  nombres: 'Ana',
  apellidos: 'Pérez',
  ci: '9001',
  puntaje: 94,
  estadoNotificacion: 'ENVIADA',
  estadoPostulacion: 'calificada',
  resultadoAuditoria: 'SIN_AUDITAR',
  puntajeTotal: null,
  plazoEntrega: null,
  notificadoEn: null,
  documentacionRecibidaEn: null,
}

function conCliente(hijo: ReactNode) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(<QueryClientProvider client={queryClient}>{hijo}</QueryClientProvider>)
}

beforeEach(() => {
  cliente.get.mockReset()
  cliente.post.mockReset()
  cliente.patch.mockReset()
})

describe('RegistrarEntregaPanel', () => {
  it('sin finalista elegido pide elegir uno', () => {
    conCliente(<RegistrarEntregaPanel finalista={undefined} />)

    expect(screen.getByText(/Elige un finalista de la tabla/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Marcar como recibida' })).not.toBeInTheDocument()
  })

  it('propone la fecha de hoy y manda el PATCH con lo escrito', async () => {
    cliente.patch.mockResolvedValue({ data: { ...FINALISTA, documentacionRecibidaEn: hoyIso() } })
    const alRegistrar = vi.fn()
    const user = userEvent.setup()
    conCliente(<RegistrarEntregaPanel finalista={FINALISTA} onRegistrada={alRegistrar} />)

    expect(screen.getByLabelText('Fecha de recepción')).toHaveValue(hoyIso())
    await user.type(screen.getByLabelText('Observaciones (opcional)'), 'Completo')
    await user.click(screen.getByRole('button', { name: 'Marcar como recibida' }))

    await waitFor(() =>
      expect(cliente.patch).toHaveBeenCalledWith('/finalistas/fin-1/entrega', {
        fechaRecepcion: hoyIso(),
        observaciones: 'Completo',
      }),
    )
    await waitFor(() => expect(alRegistrar).toHaveBeenCalled())
  })

  it('con una fecha ya registrada la muestra y avisa que corregirla deja la anterior en la bitácora (A5)', () => {
    conCliente(<RegistrarEntregaPanel finalista={{ ...FINALISTA, documentacionRecibidaEn: '2026-11-05' }} />)

    expect(screen.getByText(/la anterior queda en la bitácora/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Corregir la fecha' })).toBeInTheDocument()
  })

  it('no deja guardar una fecha futura', async () => {
    const user = userEvent.setup()
    conCliente(<RegistrarEntregaPanel finalista={FINALISTA} />)

    fireEvent.change(screen.getByLabelText('Fecha de recepción'), { target: { value: mananaIso() } })
    await user.click(screen.getByRole('button', { name: 'Marcar como recibida' }))

    expect(await screen.findByText('La fecha de recepción no puede ser futura')).toBeInTheDocument()
    expect(cliente.patch).not.toHaveBeenCalled()
  })
})

describe('NotificarFinalistasDialog', () => {
  it('resume a cuántos se enviará y no manda nada con los campos vacíos', async () => {
    const user = userEvent.setup()
    conCliente(
      <NotificarFinalistasDialog open onOpenChange={vi.fn()} finalistas={['fin-1', 'fin-2']} onNotificados={vi.fn()} />,
    )
    const dialogo = await screen.findByRole('dialog')

    expect(within(dialogo).getByText('Se enviará a 2 finalistas')).toBeInTheDocument()
    await user.click(within(dialogo).getByRole('button', { name: 'Enviar aviso' }))

    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('con fallidos explica cómo reintentar solo a esos (A2)', async () => {
    cliente.post.mockResolvedValue({ data: { solicitados: 2, enviados: 1, fallidos: 1 } })
    const alNotificar = vi.fn()
    const user = userEvent.setup()
    conCliente(
      <NotificarFinalistasDialog open onOpenChange={vi.fn()} finalistas={['fin-1', 'fin-2']} onNotificados={alNotificar} />,
    )
    const dialogo = await screen.findByRole('dialog')
    fireEvent.change(within(dialogo).getByLabelText('Plazo de entrega'), { target: { value: mananaIso() } })
    await user.type(within(dialogo).getByLabelText('Lugar y horario de entrega'), 'DUBSS')
    await user.type(within(dialogo).getByLabelText('Documentos físicos a presentar'), 'CI')
    await user.click(within(dialogo).getByRole('button', { name: 'Enviar aviso' }))
    await user.click(await screen.findByRole('button', { name: 'Confirmar envío' }))

    expect(await screen.findByText(/Se enviaron/)).toHaveTextContent('Se enviaron 1 avisos y fallaron 1.')
    expect(screen.getByText(/Seleccionar fallidos/)).toBeInTheDocument()
    expect(alNotificar).toHaveBeenCalled()
  })
})

describe('ComunicadoForm', () => {
  it('pide confirmación y publica mandando la convocatoria elegida', async () => {
    cliente.post.mockResolvedValue({ data: { codigo: 'c-1', totalDestinatarios: 7 } })
    const user = userEvent.setup()
    conCliente(<ComunicadoForm convocatoriaId="conv-2" />)

    await user.type(screen.getByLabelText('Título'), 'Aviso')
    await user.type(screen.getByLabelText('Contenido'), 'Texto del aviso')
    await user.click(screen.getByRole('button', { name: 'Revisar y publicar' }))
    expect(cliente.post).not.toHaveBeenCalled()
    await user.click(await screen.findByRole('button', { name: 'Confirmar publicación' }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalledTimes(1))
    expect(cliente.post.mock.calls[0][0]).toBe('/comunicados')
    expect(cliente.post.mock.calls[0][1]).toMatchObject({
      titulo: 'Aviso',
      contenido: 'Texto del aviso',
      convocatoriaId: 'conv-2',
    })
    expect(await screen.findByText('Se envió a 7 destinatarios.')).toBeInTheDocument()
  })

  async function elegirDestinatarios(user: ReturnType<typeof userEvent.setup>, opcion: string) {
    await user.click(screen.getByRole('combobox', { name: 'Dirigido a' }))
    await user.click(await screen.findByRole('option', { name: opcion }))
  }

  it('el selector ofrece «Adjudicados» junto a «Todos» y «Finalistas»', async () => {
    const user = userEvent.setup()
    conCliente(<ComunicadoForm convocatoriaId="conv-2" />)

    await user.click(screen.getByRole('combobox', { name: 'Dirigido a' }))

    expect(await screen.findByRole('option', { name: 'Todos los postulantes' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Finalistas' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Adjudicados' })).toBeInTheDocument()
  })

  it('«Adjudicados» sin convocatoria marca el error y no deja publicar', async () => {
    const user = userEvent.setup()
    conCliente(<ComunicadoForm />)
    await user.type(screen.getByLabelText('Título'), 'Aviso')
    await user.type(screen.getByLabelText('Contenido'), 'Texto del aviso')

    await elegirDestinatarios(user, 'Adjudicados')

    expect(screen.getByText('Elige una convocatoria para avisar a los adjudicados.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Revisar y publicar' }))
    expect(screen.queryByRole('button', { name: 'Confirmar publicación' })).not.toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('«Adjudicados» con convocatoria manda destinatarios ADJUDICADOS y la convocatoria', async () => {
    cliente.post.mockResolvedValue({ data: { codigo: 'c-2', totalDestinatarios: 3 } })
    const user = userEvent.setup()
    conCliente(<ComunicadoForm convocatoriaId="conv-2" />)
    await user.type(screen.getByLabelText('Título'), 'Aviso')
    await user.type(screen.getByLabelText('Contenido'), 'Texto del aviso')
    await elegirDestinatarios(user, 'Adjudicados')

    await user.click(screen.getByRole('button', { name: 'Revisar y publicar' }))
    await user.click(await screen.findByRole('button', { name: 'Confirmar publicación' }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalledTimes(1))
    expect(cliente.post.mock.calls[0][1]).toMatchObject({
      titulo: 'Aviso',
      destinatarios: 'ADJUDICADOS',
      convocatoriaId: 'conv-2',
    })
  })
})
