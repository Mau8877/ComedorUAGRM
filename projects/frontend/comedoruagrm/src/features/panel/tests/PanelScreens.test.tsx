import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const cliente = vi.hoisted(() => ({ get: vi.fn() }))
vi.mock('@/store', () => ({ apiClient: cliente, ApiError: class ApiError extends Error { code: string; constructor(code: string, message: string) { super(message); this.code = code } } }))
// Los atajos son enlaces del router: acá solo importa a dónde apuntan.
vi.mock('@tanstack/react-router', async (original) => ({
  ...(await original<object>()),
  Link: ({ to, params, children, className }: { to: string; params?: Record<string, string>; children: ReactNode; className?: string }) => (
    <a href={Object.entries(params ?? {}).reduce((ruta, [clave, valor]) => ruta.replace('$' + clave, valor), to)} className={className}>
      {children}
    </a>
  ),
}))
// Recharts mide el DOM: en jsdom el gráfico no se dibuja, pero sí su contenedor.
vi.mock('@/components/ui/chart', () => ({
  ChartContainer: ({ children }: { children: ReactNode }) => <div data-testid="grafico">{children}</div>,
  ChartTooltip: () => null,
  ChartTooltipContent: () => null,
}))
vi.mock('recharts', () => {
  const Nada = () => null
  return { Bar: Nada, BarChart: ({ children }: { children: ReactNode }) => <>{children}</>, CartesianGrid: Nada, XAxis: Nada, YAxis: Nada }
})

import { ApiError } from '@/store'

import { PanelAdministradorScreen, PanelComedorScreen, PanelPostulanteScreen } from '../index'
import type { PanelAdministrador, PanelComedor, PanelPostulante } from '../types'

function montar(pantalla: ReactNode) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={queryClient}>{pantalla}</QueryClientProvider>)
}

const admin: PanelAdministrador = {
  convocatoriasAbiertas: 1,
  postulacionesPendientes: 1200,
  postulacionesCalificadas: 30,
  becasVigentes: 80,
  becasSuspendidas: 3,
  asistenciasHoy: 55,
  convocatoriaAbierta: { codigo: 'c-1', nombre: 'Beca 2026-II', fechaCierre: '2099-12-01T04:00:00Z', cupos: 100, postulaciones: 42 },
}

const comedor: PanelComedor = {
  hoy: '2026-11-18',
  becasVigentes: 80,
  asistenciasHoy: 40,
  registradasPorMi: 25,
  ultimoRegistro: '2026-11-18T14:30:00Z',
  ultimosDias: ['12', '13', '14', '15', '16', '17', '18'].map((d) => ({ fecha: `2026-11-${d}`, asistencias: d === '18' ? 40 : 5 })),
}

describe('PanelAdministradorScreen', () => {
  beforeEach(() => {
    cliente.get.mockReset()
  })

  it('muestra los conteos, la convocatoria abierta y los accesos directos', async () => {
    cliente.get.mockResolvedValue({ data: admin })
    montar(<PanelAdministradorScreen nombres="Ana" />)

    expect(await screen.findByText('Postulaciones pendientes')).toBeInTheDocument()
    expect(cliente.get).toHaveBeenCalledWith('/panel/administrador')
    expect(screen.getByText('1.200')).toBeInTheDocument()
    expect(screen.getByText('Beca 2026-II')).toBeInTheDocument()
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '42')
    expect(screen.getByRole('link', { name: /Lista priorizada/ })).toHaveAttribute('href', '/lista-priorizada')
    expect(screen.getByRole('link', { name: /Padrón de becados/ })).toHaveAttribute('href', '/padron-becados')
  })

  it('sin convocatoria abierta lo dice', async () => {
    cliente.get.mockResolvedValue({ data: { ...admin, convocatoriasAbiertas: 0, convocatoriaAbierta: null } })
    montar(<PanelAdministradorScreen nombres="Ana" />)

    expect(await screen.findByText(/No hay ninguna convocatoria abierta/)).toBeInTheDocument()
  })

  it('si el resumen falla avisa pero deja los accesos directos', async () => {
    cliente.get.mockRejectedValue(new Error('caído'))
    montar(<PanelAdministradorScreen nombres="Ana" />)

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar el resumen')
    expect(screen.getByRole('link', { name: /Convocatorias/ })).toHaveAttribute('href', '/convocatorias')
  })
})

describe('PanelComedorScreen', () => {
  beforeEach(() => {
    cliente.get.mockReset()
  })

  it('muestra el resumen del día con la hora de La Paz y el gráfico', async () => {
    cliente.get.mockResolvedValue({ data: comedor })
    montar(<PanelComedorScreen nombres="Luis" />)

    expect(await screen.findByText('Asistencias de hoy')).toBeInTheDocument()
    expect(cliente.get).toHaveBeenCalledWith('/panel/comedor')
    expect(screen.getByText('Resumen de hoy, 18/11/2026.')).toBeInTheDocument()
    expect(screen.getByText('10:30')).toBeInTheDocument()
    expect(screen.getByText('50 % de los becados')).toBeInTheDocument()
    expect(screen.getByTestId('grafico')).toBeInTheDocument()
  })

  it('sin registros hoy muestra un guion y sin gráfico si no hubo ninguno en la semana', async () => {
    cliente.get.mockResolvedValue({
      data: { ...comedor, asistenciasHoy: 0, registradasPorMi: 0, ultimoRegistro: null, ultimosDias: comedor.ultimosDias.map((d) => ({ ...d, asistencias: 0 })) },
    })
    montar(<PanelComedorScreen nombres="Luis" />)

    expect(await screen.findByText('Hoy aún no se registra ninguna asistencia')).toBeInTheDocument()
    expect(screen.getByText('Sin registros en los últimos 7 días.')).toBeInTheDocument()
  })
})

describe('PanelPostulanteScreen', () => {
  const abierta = { codigo: 'c-1', nombre: 'Beca 2026-II', fechaCierre: '2099-12-01T04:00:00Z' }

  function responder(panel: PanelPostulante, opciones: { postulaciones?: unknown[]; beca?: 'ninguna' | 'vigente' } = {}) {
    cliente.get.mockImplementation((ruta: string) => {
      switch (ruta) {
        case '/panel/postulante':
          return Promise.resolve({ data: panel })
        case '/postulaciones/mias':
          return Promise.resolve({ data: { data: opciones.postulaciones ?? [], meta: { page: 1, pageSize: 1, totalItems: 0, totalPages: 0 } } })
        case '/mi-beca':
          return opciones.beca === 'vigente'
            ? Promise.resolve({ data: { codigo: 'b', estado: 'VIGENTE', convocatoria: 'Beca 2026-I', gestion: '2026-I', fechaAdjudicacion: null, fechaInicio: '2026-03-01', fechaFin: null, motivo: null, totalAsistencias: 12 } })
            : Promise.reject(new ApiError('ERR_MBE_01', 'sin beca'))
        case '/notificaciones/me/resumen':
          return Promise.resolve({ data: { noLeidas: 3 } })
        default:
          return Promise.reject(new Error(`ruta sin simular: ${ruta}`))
      }
    })
  }

  beforeEach(() => {
    cliente.get.mockReset()
  })

  it('con la convocatoria abierta y sin postular ofrece «Postular ahora»', async () => {
    responder({ convocatoriaAbierta: abierta, tieneBorrador: false, yaPostulo: false })
    montar(<PanelPostulanteScreen nombres="Eva" />)

    expect(await screen.findByText('Beca 2026-II')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Postular ahora' })).toHaveAttribute('href', '/postular')
    expect(await screen.findByText('Aún no enviaste ninguna postulación.')).toBeInTheDocument()
    expect(await screen.findByText(/Todavía no tienes una beca/)).toBeInTheDocument()
    expect(await screen.findByText('3')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Resultados/ })).toHaveAttribute('href', '/resultados')
  })

  it('con un borrador ofrece continuarlo', async () => {
    responder({ convocatoriaAbierta: abierta, tieneBorrador: true, yaPostulo: false })
    montar(<PanelPostulanteScreen nombres="Eva" />)

    expect(await screen.findByRole('link', { name: 'Continuar mi borrador' })).toBeInTheDocument()
  })

  it('si ya postuló no ofrece postular otra vez y muestra su postulación y su beca', async () => {
    responder(
      { convocatoriaAbierta: abierta, tieneBorrador: false, yaPostulo: true },
      {
        beca: 'vigente',
        postulaciones: [{ codigo: 'p-1', convocatoria: { codigo: 'c-1', nombre: 'Beca 2026-II' }, gestionAcademica: { codigo: 'g', nombre: '2026-II' }, fechaEnvio: '2026-11-10T14:00:00Z', estado: 'pendiente', numeroEnvios: 1 }],
      }
    )
    montar(<PanelPostulanteScreen nombres="Eva" />)

    expect(await screen.findByText(/Ya enviaste tu postulación/)).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Postular ahora' })).not.toBeInTheDocument()
    expect(await screen.findByText('Pendiente')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver el detalle' })).toHaveAttribute('href', '/mis-postulaciones/p-1')
    expect(await screen.findByText('Vigente')).toBeInTheDocument()
    expect(screen.getByText('12 asistencias al comedor')).toBeInTheDocument()
  })

  it('sin convocatoria abierta lo dice', async () => {
    responder({ convocatoriaAbierta: null, tieneBorrador: false, yaPostulo: false })
    montar(<PanelPostulanteScreen nombres="Eva" />)

    expect(await screen.findByText(/no hay ninguna convocatoria abierta/)).toBeInTheDocument()
  })
})
