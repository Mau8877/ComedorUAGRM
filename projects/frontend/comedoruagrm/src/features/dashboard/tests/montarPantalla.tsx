// Ayudas compartidas por los tests de la pantalla del dashboard: montan la pantalla con un apiClient simulado
// y un QueryClient limpio.
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

import { DashboardScreen } from '../screens/DashboardScreen'
import type { FiltrosDashboard, Indicadores, SeleccionDashboard } from '../types'

import { cliente } from './simulados'

export { cliente }

export const yo = { codigo: 'yo', nombres: 'Dora', apellidos: 'Directiva', correo: 'dir@dubss.com', rol: 'DIRECTIVO' }

export const FILTROS: FiltrosDashboard = {
  gestiones: [
    {
      codigo: 'g-2',
      etiqueta: '2026-II',
      vigente: true,
      convocatorias: [
        { codigo: 'c-a', nombre: 'Beca A' },
        { codigo: 'c-b', nombre: 'Beca B' },
      ],
    },
    { codigo: 'g-1', etiqueta: '2026-I', vigente: false, convocatorias: [{ codigo: 'c-z', nombre: 'Beca Z' }] },
  ],
}

export function indicadores(extra: Partial<Indicadores> = {}): Indicadores {
  return {
    gestion: { codigo: 'g-2', etiqueta: '2026-II' },
    convocatoria: null,
    totales: { postulacionesRecibidas: 5230, finalistas: 40, becasAdjudicadas: 12, asistenciaPromedio: null },
    porMes: [
      { mes: '2026-09', postulaciones: 3000, finalistas: 0, adjudicadas: 0, asistencia: null },
      { mes: '2026-10', postulaciones: 2230, finalistas: 40, adjudicadas: 12, asistencia: null },
    ],
    actualizadoEn: '2026-11-13T06:00:00Z',
    sinDatos: false,
    ...extra,
  }
}

/** Responde filtros e indicadores; `porSeleccion` permite devolver otros indicadores según la selección pedida. */
export function responder(
  opciones: {
    filtros?: FiltrosDashboard
    indicadores?: Indicadores
    porSeleccion?: (seleccion: Record<string, string>) => Indicadores | Error
  } = {},
) {
  cliente.get.mockImplementation((ruta: string, config?: { params?: Record<string, string> }) => {
    switch (ruta) {
      case '/dashboard/filtros':
        return Promise.resolve({ data: opciones.filtros ?? FILTROS })
      case '/dashboard/indicadores': {
        const resultado = opciones.porSeleccion?.(config?.params ?? {}) ?? opciones.indicadores ?? indicadores()
        return resultado instanceof Error ? Promise.reject(resultado) : Promise.resolve({ data: resultado })
      }
      default:
        return Promise.reject(new Error(`ruta sin simular: ${ruta}`))
    }
  })
}

/** Parámetros de la última consulta de indicadores. */
export function ultimaConsulta(): SeleccionDashboard & Record<string, string> {
  const llamadas = cliente.get.mock.calls.filter((c) => c[0] === '/dashboard/indicadores')
  return llamadas[llamadas.length - 1][1].params
}

export function consultasDeIndicadores(): number {
  return cliente.get.mock.calls.filter((c) => c[0] === '/dashboard/indicadores').length
}

export function reiniciarCliente() {
  Object.values(cliente).forEach((fn) => fn.mockReset())
}

export function montar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const vista = render(
    <QueryClientProvider client={queryClient}>
      <DashboardScreen titulo="Dashboard de indicadores" activeHref="/dashboard" usuarioActual={yo} />
    </QueryClientProvider>,
  )
  return { ...vista, queryClient, user: userEvent.setup() }
}
