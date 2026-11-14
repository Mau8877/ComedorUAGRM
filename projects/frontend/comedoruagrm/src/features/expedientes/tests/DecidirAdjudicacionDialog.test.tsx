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

import type { PanelAdjudicacion } from '@/features/adjudicacion'
import { ApiError } from '@/store'

import { cliente } from '../../finalistas/tests/clienteSimulado'
import { DecidirAdjudicacionDialog } from '../components'
import type { AccionDecision } from '../types'
import { expediente } from './fixtures'

const CASOS: Array<{ accion: AccionDecision; boton: string; titulo: string; ruta: string; resultado: string }> = [
  { accion: 'adjudicar', boton: 'Adjudicar', titulo: 'Adjudicar beca', ruta: '/finalistas/f-1/adjudicar', resultado: 'ADJUDICADA' },
  { accion: 'no-adjudicar', boton: 'No adjudicar', titulo: 'No adjudicar', ruta: '/finalistas/f-1/no-adjudicar', resultado: 'NO_ADJUDICADA' },
]

function panel(extra: Partial<PanelAdjudicacion> = {}): PanelAdjudicacion {
  return {
    convocatoria: { codigo: 'c-1', nombre: 'Beca Comedor 2026', estado: 'CERRADA' },
    cupos: 10,
    puntajeMinimo: 60,
    adjudicados: 6,
    noAdjudicados: 1,
    cuposDisponibles: 4,
    cantidadFinalistas: 15,
    totalFinalistas: 12,
    calificadasSinSeleccionar: 30,
    resultadosPublicados: false,
    decisionesHabilitadas: true,
    ...extra,
  }
}

function decision(resultado: string) {
  return {
    finalista: 'f-1',
    postulacion: 'p-1',
    resultado,
    estado: resultado === 'ADJUDICADA' ? 'adjudicada' : 'no_adjudicada',
    puntajeFinal: 80.5,
    fechaAdjudicacion: 't',
    decididaPor: 'Ana',
    cuposDisponibles: 3,
  }
}

function montar(accion: AccionDecision, props: Partial<React.ComponentProps<typeof DecidirAdjudicacionDialog>> = {}) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const onOpenChange = vi.fn()
  render(
    <QueryClientProvider client={queryClient}>
      <DecidirAdjudicacionDialog
        open
        onOpenChange={onOpenChange}
        accion={accion}
        expediente={expediente()}
        panel={panel()}
        {...props}
      />
    </QueryClientProvider>,
  )
  return { onOpenChange }
}

async function confirmar(accion: AccionDecision, boton: string) {
  const usuario = userEvent.setup()
  if (accion === 'no-adjudicar') await usuario.type(screen.getByLabelText('Motivo'), 'No cumple')
  await usuario.click(screen.getByRole('button', { name: boton }))
}

describe.each(CASOS)('DecidirAdjudicacionDialog (%s)', ({ accion, boton, titulo, ruta, resultado }) => {
  beforeEach(() => {
    cliente.post.mockReset()
  })

  it('muestra el título, el postulante, el puntaje total, los cupos disponibles y el aviso de que es definitiva', () => {
    montar(accion)

    expect(screen.getAllByText(titulo).length).toBeGreaterThan(0)
    expect(screen.getByText('Luis Pérez')).toBeInTheDocument()
    expect(screen.getByText('Beca Comedor 2026')).toBeInTheDocument()
    expect(screen.getByText('80.50')).toBeInTheDocument()
    expect(screen.getByText('4')).toBeInTheDocument()
    expect(screen.getByText('60.00')).toBeInTheDocument()
    expect(screen.getByText(/La decisión es definitiva/)).toBeInTheDocument()
    expect(screen.getByText(/no se le avisa ahora/)).toBeInTheDocument()
    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument()
  })

  it('con cupos, mínimo o puntaje sin definir muestra «Sin definir» y «No disponible», sin inventar números', () => {
    const e = expediente({ puntajeTotal: null })
    montar(accion, { expediente: e, panel: panel({ cuposDisponibles: null, puntajeMinimo: null }) })

    expect(screen.getAllByText('Sin definir')).toHaveLength(2)
    expect(screen.getByText('No disponible')).toBeInTheDocument()
  })

  it('mientras el panel no cargó muestra los cupos sin definir', () => {
    montar(accion, { panel: undefined })

    expect(screen.getAllByText('Sin definir')).toHaveLength(2)
  })

  it('cancelar cierra sin llamar a la API', async () => {
    const usuario = userEvent.setup()
    const { onOpenChange } = montar(accion)

    await usuario.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(onOpenChange).toHaveBeenCalledWith(false)
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('envía la decisión, avisa a la pantalla y cierra la ventana', async () => {
    cliente.post.mockResolvedValue({ data: decision(resultado) })
    const onDecidida = vi.fn()
    const { onOpenChange } = montar(accion, { onDecidida })

    await confirmar(accion, boton)

    const esperado = accion === 'adjudicar' ? [ruta] : [ruta, { motivo: 'No cumple' }]
    await waitFor(() => expect(cliente.post).toHaveBeenCalledWith(...esperado))
    await waitFor(() => expect(onDecidida).toHaveBeenCalledWith(decision(resultado)))
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('deshabilita el botón mientras se envía y no manda dos peticiones', async () => {
    let resolver: (valor: unknown) => void = () => undefined
    cliente.post.mockReturnValue(new Promise((r) => (resolver = r)))
    const usuario = userEvent.setup()
    montar(accion)

    await confirmar(accion, boton)
    await waitFor(() => expect(screen.getByRole('button', { name: new RegExp(boton) })).toBeDisabled())
    await usuario.click(screen.getByRole('button', { name: new RegExp(boton) }))

    expect(cliente.post).toHaveBeenCalledTimes(1)
    resolver({ data: decision(resultado) })
  })

  it.each([
    ['ERR_ADJ_01', 'ya no está calificada', true],
    ['ERR_ADJ_04', 'Ya no quedan cupos', true],
    ['ERR_ADJ_02', 'auditado', false],
    ['ERR_ADJ_03', 'no alcanza el puntaje mínimo', false],
    ['ERR_ADJ_05', 'debe estar cerrada', false],
    ['ERR_AUTH_07', 'No tienes permiso', false],
  ])('ante %s muestra el mensaje, deja abierta la ventana y %s refresca', async (codigo, texto, refresca) => {
    cliente.post.mockRejectedValue(new ApiError(codigo, 'x'))
    const onDesactualizado = vi.fn()
    const { onOpenChange } = montar(accion, { onDesactualizado })

    await confirmar(accion, boton)

    expect(await screen.findByRole('alert')).toHaveTextContent(texto)
    expect(onDesactualizado).toHaveBeenCalledTimes(refresca ? 1 : 0)
    expect(onOpenChange).not.toHaveBeenCalledWith(false)
  })

  it('si el finalista ya no existe avisa a la pantalla y cierra', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_FIN_01', 'x'))
    const onFinalistaInexistente = vi.fn()
    const { onOpenChange } = montar(accion, { onFinalistaInexistente })

    await confirmar(accion, boton)

    await waitFor(() => expect(onFinalistaInexistente).toHaveBeenCalledTimes(1))
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })
})

describe('DecidirAdjudicacionDialog: motivo solo al no adjudicar', () => {
  beforeEach(() => {
    cliente.post.mockReset()
  })

  it('adjudicar no tiene campo de motivo ni contador', () => {
    montar('adjudicar')

    expect(screen.queryByLabelText('Motivo')).not.toBeInTheDocument()
    expect(screen.queryByText('0/500')).not.toBeInTheDocument()
  })

  it('no adjudicar muestra el motivo con su contador de 500 y lo cuenta mientras se escribe', async () => {
    const usuario = userEvent.setup()
    montar('no-adjudicar')

    expect(screen.getByText('0/500')).toBeInTheDocument()
    await usuario.type(screen.getByLabelText('Motivo'), 'Hola')

    expect(screen.getByText('4/500')).toBeInTheDocument()
  })

  it('sin motivo o con solo espacios no envía y señala el campo', async () => {
    const usuario = userEvent.setup()
    montar('no-adjudicar')

    await usuario.click(screen.getByRole('button', { name: 'No adjudicar' }))
    expect(await screen.findByText('El motivo es obligatorio')).toBeInTheDocument()
    await usuario.type(screen.getByLabelText('Motivo'), '   ')
    await usuario.click(screen.getByRole('button', { name: 'No adjudicar' }))

    expect(await screen.findByText('El motivo es obligatorio')).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('con más de 500 caracteres no envía', async () => {
    const usuario = userEvent.setup()
    montar('no-adjudicar')

    await usuario.click(screen.getByLabelText('Motivo'))
    await usuario.paste('x'.repeat(501))
    await usuario.click(screen.getByRole('button', { name: 'No adjudicar' }))

    expect(await screen.findByText('El motivo no puede pasar de 500 caracteres')).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('manda el motivo sin espacios sobrantes', async () => {
    cliente.post.mockResolvedValue({ data: decision('NO_ADJUDICADA') })
    const usuario = userEvent.setup()
    montar('no-adjudicar')

    await usuario.type(screen.getByLabelText('Motivo'), '  No cumple ')
    await usuario.click(screen.getByRole('button', { name: 'No adjudicar' }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalledWith('/finalistas/f-1/no-adjudicar', { motivo: 'No cumple' }))
  })
})
