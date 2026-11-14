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
import { SeleccionarFinalistasDialog } from '../components'
import type { PanelAdjudicacion } from '../types'

function panel(extra: Partial<PanelAdjudicacion> = {}): PanelAdjudicacion {
  return {
    convocatoria: { codigo: 'c-1', nombre: 'Beca Comedor 2026', estado: 'CERRADA' },
    cupos: 10,
    puntajeMinimo: 60,
    adjudicados: 2,
    noAdjudicados: 0,
    cuposDisponibles: 8,
    cantidadFinalistas: 15,
    totalFinalistas: 0,
    calificadasSinSeleccionar: 40,
    resultadosPublicados: false,
    decisionesHabilitadas: true,
    ...extra,
  }
}

function resultado(n: number) {
  return {
    convocatoria: 'c-1',
    seleccionados: n,
    finalistas: Array.from({ length: n }, (_, i) => ({ codigo: `f-${i}`, postulacion: `p-${i}`, posicion: i + 1 })),
  }
}

function montar(p: PanelAdjudicacion, props: Partial<React.ComponentProps<typeof SeleccionarFinalistasDialog>> = {}) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const onOpenChange = vi.fn()
  render(
    <QueryClientProvider client={queryClient}>
      <SeleccionarFinalistasDialog open onOpenChange={onOpenChange} panel={p} {...props} />
    </QueryClientProvider>,
  )
  return { onOpenChange }
}

describe('SeleccionarFinalistasDialog', () => {
  beforeEach(() => {
    cliente.post.mockReset()
  })

  it('propone la cantidad configurada y explica los cupos, los suplentes y que no cambia la configurada', () => {
    montar(panel())

    expect(screen.getByLabelText('Cantidad')).toHaveValue('15')
    expect(screen.getByText('Beca Comedor 2026')).toBeInTheDocument()
    expect(screen.getByText('40')).toBeInTheDocument()
    expect(screen.getByText('8')).toBeInTheDocument()
    expect(screen.getByText(/más finalistas que cupos/)).toBeInTheDocument()
    expect(screen.getByText(/no cambia la configurada/)).toBeInTheDocument()
  })

  it('sin cantidad configurada deja el campo vacío y lo avisa', () => {
    montar(panel({ cantidadFinalistas: null }))

    expect(screen.getByLabelText('Cantidad')).toHaveValue('')
    expect(screen.getByText(/no tiene una configurada/)).toBeInTheDocument()
  })

  it('no propone un número mayor que las postulaciones sin seleccionar', () => {
    montar(panel({ cantidadFinalistas: 50, calificadasSinSeleccionar: 12 }))

    expect(screen.getByLabelText('Cantidad')).toHaveValue('12')
  })

  it('sin cantidad configurada la cantidad es obligatoria y no envía', async () => {
    const usuario = userEvent.setup()
    montar(panel({ cantidadFinalistas: null }))

    await usuario.click(screen.getByRole('button', { name: 'Seleccionar' }))

    expect(await screen.findByText('Indica cuántos finalistas seleccionar')).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it.each(['0', '-2', '2.5', 'diez'])('con «%s» no envía y pide un entero positivo', async (texto) => {
    const usuario = userEvent.setup()
    montar(panel())

    await usuario.clear(screen.getByLabelText('Cantidad'))
    await usuario.type(screen.getByLabelText('Cantidad'), texto)
    await usuario.click(screen.getByRole('button', { name: 'Seleccionar' }))

    expect(await screen.findByText('La cantidad debe ser un número entero positivo')).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('envía la cantidad ajustada (aunque supere los cupos), avisa a la pantalla y cierra', async () => {
    cliente.post.mockResolvedValue({ data: resultado(12) })
    const usuario = userEvent.setup()
    const onSeleccionados = vi.fn()
    const { onOpenChange } = montar(panel(), { onSeleccionados })

    await usuario.clear(screen.getByLabelText('Cantidad'))
    await usuario.type(screen.getByLabelText('Cantidad'), '12')
    await usuario.click(screen.getByRole('button', { name: 'Seleccionar' }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalledWith('/convocatorias/c-1/finalistas', { cantidad: 12 }))
    await waitFor(() => expect(onSeleccionados).toHaveBeenCalledWith(resultado(12)))
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('confirmar sin tocar nada manda la cantidad propuesta', async () => {
    cliente.post.mockResolvedValue({ data: resultado(15) })
    const usuario = userEvent.setup()
    montar(panel())

    await usuario.click(screen.getByRole('button', { name: 'Seleccionar' }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalledWith('/convocatorias/c-1/finalistas', { cantidad: 15 }))
  })

  it('vaciar el campo con una cantidad configurada manda un pedido sin cantidad (usa la configurada)', async () => {
    cliente.post.mockResolvedValue({ data: resultado(15) })
    const usuario = userEvent.setup()
    montar(panel())

    await usuario.clear(screen.getByLabelText('Cantidad'))
    await usuario.click(screen.getByRole('button', { name: 'Seleccionar' }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalledWith('/convocatorias/c-1/finalistas', {}))
  })

  it('deshabilita el botón mientras se envía y no manda dos peticiones', async () => {
    let resolver: (valor: unknown) => void = () => undefined
    cliente.post.mockReturnValue(new Promise((r) => (resolver = r)))
    const usuario = userEvent.setup()
    montar(panel())

    await usuario.click(screen.getByRole('button', { name: 'Seleccionar' }))
    await waitFor(() => expect(screen.getByRole('button', { name: /Seleccionar/ })).toBeDisabled())
    await usuario.click(screen.getByRole('button', { name: /Seleccionar/ }))

    expect(cliente.post).toHaveBeenCalledTimes(1)
    resolver({ data: resultado(15) })
  })

  it('cancelar cierra sin llamar a la API', async () => {
    const usuario = userEvent.setup()
    const { onOpenChange } = montar(panel())

    await usuario.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(onOpenChange).toHaveBeenCalledWith(false)
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it.each([
    ['ERR_ADJ_01', 'ya se publicaron', true],
    ['ERR_ADJ_05', 'debe estar cerrada', true],
    ['ERR_AUTH_07', 'No tienes permiso', false],
  ])('ante %s muestra el mensaje, deja la ventana abierta y %s pide refrescar el panel', async (codigo, texto, refresca) => {
    cliente.post.mockRejectedValue(new ApiError(codigo, 'x'))
    const usuario = userEvent.setup()
    const onDesactualizado = vi.fn()
    const { onOpenChange } = montar(panel(), { onDesactualizado })

    await usuario.click(screen.getByRole('button', { name: 'Seleccionar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(texto)
    expect(onDesactualizado).toHaveBeenCalledTimes(refresca ? 1 : 0)
    expect(onOpenChange).not.toHaveBeenCalledWith(false)
  })

  it('ante ERR_SYS_01 (cantidad mayor que las disponibles) muestra el mensaje del backend', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_SYS_01', 'La cantidad (50) supera las disponibles (40)'))
    const usuario = userEvent.setup()
    montar(panel())

    await usuario.click(screen.getByRole('button', { name: 'Seleccionar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('La cantidad (50) supera las disponibles (40)')
  })

  it('si la convocatoria ya no existe avisa a la pantalla y cierra', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_PRI_01', 'x'))
    const usuario = userEvent.setup()
    const onConvocatoriaInexistente = vi.fn()
    const { onOpenChange } = montar(panel(), { onConvocatoriaInexistente })

    await usuario.click(screen.getByRole('button', { name: 'Seleccionar' }))

    await waitFor(() => expect(onConvocatoriaInexistente).toHaveBeenCalledTimes(1))
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })
})
