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
// El selector de convocatorias es el de CU06: acá se simula con lo que devuelve su endpoint.
vi.mock('@/features/priorizacion', async () => {
  const { useQuery } = await import('@tanstack/react-query')
  const { cliente } = await import('../../finalistas/tests/clienteSimulado')
  return {
    useConvocatoriasPriorizables: () =>
      useQuery({
        queryKey: ['priorizacion', 'convocatorias'],
        queryFn: async () => (await cliente.get('/lista-priorizada/convocatorias')).data,
      }),
  }
})
// Los accesos a la lista priorizada y a finalistas usan el router; acá basta con anclas.
vi.mock('@tanstack/react-router', () => ({
  Link: ({ to, children, ...resto }: { to: string; children?: ReactNode }) => (
    <a href={to} {...resto}>
      {children}
    </a>
  ),
}))

import { ApiError } from '@/store'

import { cliente } from '../../finalistas/tests/clienteSimulado'
import { AdjudicacionScreen } from '../screens/AdjudicacionScreen'
import type { PanelAdjudicacion } from '../types'

const yo = { codigo: 'yo', nombres: 'Admin', apellidos: 'Principal', correo: 'admin@dubss.com', rol: 'ADMINISTRADOR_DUBSS' }

const CONVOCATORIAS = [
  { codigo: 'conv-2', nombre: 'Beca 2027-I', gestion: '2027-I', estado: 'ABIERTA', fechaApertura: '', fechaCierre: '' },
  { codigo: 'conv-1', nombre: 'Beca 2026-II', gestion: '2026-II', estado: 'CERRADA', fechaApertura: '', fechaCierre: '' },
]

function panel(extra: Partial<PanelAdjudicacion> = {}): PanelAdjudicacion {
  return {
    convocatoria: { codigo: 'conv-2', nombre: 'Beca 2027-I', estado: 'CERRADA' },
    cupos: 10,
    puntajeMinimo: 55.5,
    adjudicados: 3,
    noAdjudicados: 1,
    cuposDisponibles: 7,
    cantidadFinalistas: 15,
    totalFinalistas: 12,
    calificadasSinSeleccionar: 30,
    resultadosPublicados: false,
    decisionesHabilitadas: true,
    ...extra,
  }
}

function responder(paneles: Record<string, PanelAdjudicacion | ApiError>, convocatorias = CONVOCATORIAS) {
  cliente.get.mockImplementation((ruta: string) => {
    if (ruta === '/lista-priorizada/convocatorias') return Promise.resolve({ data: convocatorias })
    const coincide = /^\/convocatorias\/(.+)\/adjudicacion$/.exec(ruta)
    if (coincide) {
      const respuesta = paneles[coincide[1]]
      if (respuesta instanceof ApiError) return Promise.reject(respuesta)
      return respuesta ? Promise.resolve({ data: respuesta }) : Promise.reject(new Error(`sin panel: ${ruta}`))
    }
    return Promise.reject(new Error(`ruta sin simular: ${ruta}`))
  })
}

function montar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  render(
    <QueryClientProvider client={queryClient}>
      <AdjudicacionScreen titulo="Auditoría y adjudicación" activeHref="/adjudicacion" usuarioActual={yo} />
    </QueryClientProvider>,
  )
}

describe('AdjudicacionScreen', () => {
  beforeEach(() => {
    cliente.get.mockReset()
    cliente.post.mockReset()
  })

  it('preselecciona la convocatoria más reciente y muestra su panel de cupos', async () => {
    responder({ 'conv-2': panel() })
    montar()

    expect(await screen.findByText('Cupos disponibles')).toBeInTheDocument()
    expect(cliente.get).toHaveBeenCalledWith('/convocatorias/conv-2/adjudicacion')
    expect(screen.getByText('Auditoría y adjudicación')).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: 'Convocatoria' })).toHaveTextContent('Beca 2027-I (2027-I)')
    const tarjetas = screen.getByRole('region', { name: 'Panel de adjudicación' })
    expect(tarjetas).toHaveTextContent('Cupos10')
    expect(tarjetas).toHaveTextContent('Adjudicados3')
    expect(tarjetas).toHaveTextContent('No adjudicados1')
    expect(tarjetas).toHaveTextContent('Cupos disponibles7')
    expect(tarjetas).toHaveTextContent('Puntaje mínimo55,5')
    expect(tarjetas).toHaveTextContent('Cantidad de finalistas15')
    expect(tarjetas).toHaveTextContent('Finalistas12')
    expect(tarjetas).toHaveTextContent('Sin seleccionar30')
    expect(screen.getByText('Ver lista priorizada').closest('a')).toHaveAttribute('href', '/lista-priorizada')
    expect(screen.getByText('Ver finalistas').closest('a')).toHaveAttribute('href', '/finalistas')
  })

  it('al cambiar de convocatoria pide el panel de la nueva', async () => {
    responder({ 'conv-2': panel(), 'conv-1': panel({ cupos: 6, cuposDisponibles: 4 }) })
    const usuario = userEvent.setup()
    montar()

    await screen.findByText('Cupos disponibles')
    await usuario.click(screen.getByRole('combobox', { name: 'Convocatoria' }))
    await usuario.click(await screen.findByRole('option', { name: 'Beca 2026-II (2026-II)' }))

    await waitFor(() => expect(cliente.get).toHaveBeenCalledWith('/convocatorias/conv-1/adjudicacion'))
    await waitFor(() =>
      expect(screen.getByRole('region', { name: 'Panel de adjudicación' })).toHaveTextContent('Cupos6'),
    )
  })

  it('los valores sin definir se muestran «Sin definir», sin inventar números', async () => {
    responder({
      'conv-2': panel({
        cupos: null,
        puntajeMinimo: null,
        cuposDisponibles: null,
        cantidadFinalistas: null,
        decisionesHabilitadas: false,
      }),
    })
    montar()

    const tarjetas = await screen.findByRole('region', { name: 'Panel de adjudicación' })
    expect(tarjetas).toHaveTextContent('CuposSin definir')
    expect(tarjetas).toHaveTextContent('Cupos disponiblesSin definir')
    expect(tarjetas).toHaveTextContent('Puntaje mínimoSin definir')
    expect(tarjetas).toHaveTextContent('Cantidad de finalistasSin definir')
  })

  it.each([
    [{ decisionesHabilitadas: false, resultadosPublicados: true }, 'Los resultados ya se publicaron.'],
    [
      { decisionesHabilitadas: false, convocatoria: { codigo: 'conv-2', nombre: 'Beca 2027-I', estado: 'ABIERTA' } },
      'La convocatoria debe estar cerrada',
    ],
    [{ decisionesHabilitadas: false, cupos: null }, 'Faltan cupos o puntaje mínimo por definir con la DUBSS.'],
  ])('avisa por qué no se puede decidir (%#)', async (extra, aviso) => {
    responder({ 'conv-2': panel(extra) })
    montar()

    expect((await screen.findByRole('status')).textContent).toContain(aviso)
  })

  it('con decisiones habilitadas no muestra avisos', async () => {
    responder({ 'conv-2': panel() })
    montar()

    await screen.findByText('Cupos disponibles')
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('sin postulaciones dice «No hay postulaciones para auditar» y deshabilita seleccionar', async () => {
    responder({ 'conv-2': panel({ totalFinalistas: 0, calificadasSinSeleccionar: 0 }) })
    montar()

    expect(await screen.findByText('No hay postulaciones para auditar')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Seleccionar finalistas/ })).toBeDisabled()
  })

  it('«Seleccionar finalistas» está habilitado con decisiones habilitadas y postulaciones sin seleccionar', async () => {
    responder({ 'conv-2': panel() })
    montar()

    await screen.findByText('Cupos disponibles')
    expect(screen.getByRole('button', { name: /Seleccionar finalistas/ })).toBeEnabled()
  })

  it('«Seleccionar finalistas» está deshabilitado con su motivo visible cuando no se puede decidir', async () => {
    responder({ 'conv-2': panel({ decisionesHabilitadas: false, resultadosPublicados: true }) })
    montar()

    await screen.findByText('Cupos disponibles')
    expect(screen.getByRole('button', { name: /Seleccionar finalistas/ })).toBeDisabled()
    expect(screen.getByRole('note')).toHaveTextContent('Los resultados ya se publicaron.')
  })

  it('seleccionar abre la ventana con la cantidad propuesta, envía y refresca el panel', async () => {
    responder({ 'conv-2': panel() })
    cliente.post.mockResolvedValue({ data: { convocatoria: 'conv-2', seleccionados: 15, finalistas: [] } })
    const usuario = userEvent.setup()
    montar()

    await screen.findByText('Cupos disponibles')
    await usuario.click(screen.getByRole('button', { name: /Seleccionar finalistas/ }))
    expect(await screen.findByLabelText('Cantidad')).toHaveValue('15')
    await usuario.click(screen.getByRole('button', { name: 'Seleccionar' }))

    await waitFor(() =>
      expect(cliente.post).toHaveBeenCalledWith('/convocatorias/conv-2/finalistas', { cantidad: 15 }),
    )
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    // El panel se vuelve a pedir tras seleccionar.
    await waitFor(() =>
      expect(cliente.get.mock.calls.filter(([ruta]) => ruta === '/convocatorias/conv-2/adjudicacion').length).toBeGreaterThan(1),
    )
  })

  it('cancelar la ventana no cambia nada', async () => {
    responder({ 'conv-2': panel() })
    const usuario = userEvent.setup()
    montar()

    await screen.findByText('Cupos disponibles')
    await usuario.click(screen.getByRole('button', { name: /Seleccionar finalistas/ }))
    await usuario.click(await screen.findByRole('button', { name: 'Cancelar' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('si el panel responde 404 (la convocatoria ya no existe) vuelve al selector con el aviso', async () => {
    responder({ 'conv-2': new ApiError('ERR_PRI_01', 'x'), 'conv-1': panel({ cupos: 5 }) })
    montar()

    expect(await screen.findByText('La convocatoria ya no existe. Elige otra.')).toBeInTheDocument()
    await waitFor(() => expect(cliente.get).toHaveBeenCalledWith('/convocatorias/conv-1/adjudicacion'))
  })

  it('ante un error del panel lo muestra y ofrece reintentar', async () => {
    cliente.get.mockImplementation((ruta: string) =>
      ruta === '/lista-priorizada/convocatorias'
        ? Promise.resolve({ data: CONVOCATORIAS })
        : Promise.reject(new ApiError('ERR_SYS_00', 'x')),
    )
    montar()

    expect(await screen.findByRole('alert')).toHaveTextContent('Ocurrió un error inesperado')
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
  })

  it('mientras carga muestra el estado de carga y sin convocatorias lo dice', async () => {
    cliente.get.mockImplementation((ruta: string) =>
      ruta === '/lista-priorizada/convocatorias' ? Promise.resolve({ data: [] }) : Promise.reject(new Error('x')),
    )
    montar()

    expect(screen.getByText('Cargando convocatorias…')).toBeInTheDocument()
    expect(await screen.findByText('Todavía no hay convocatorias registradas.')).toBeInTheDocument()
  })
})
