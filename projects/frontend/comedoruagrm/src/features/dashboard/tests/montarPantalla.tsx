// Ayudas compartidas por los tests de la pantalla del dashboard: montan la pantalla con un apiClient simulado
// y un QueryClient limpio.
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

import { DashboardScreen } from '../screens/DashboardScreen'
import type { Bi, FiltrosDashboard, Indicadores, SeleccionDashboard } from '../types'

import { cliente } from './simulados'

export { cliente }

export const yo = {
  codigo: 'yo',
  nombres: 'Dora',
  apellidos: 'Directiva',
  correo: 'dir@dubss.com',
  rol: 'DIRECTIVO',
}

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
    {
      codigo: 'g-1',
      etiqueta: '2026-I',
      vigente: false,
      convocatorias: [{ codigo: 'c-z', nombre: 'Beca Z' }],
    },
  ],
}

export function indicadores(extra: Partial<Indicadores> = {}): Indicadores {
  return {
    gestion: { codigo: 'g-2', etiqueta: '2026-II' },
    convocatoria: null,
    totales: {
      postulacionesRecibidas: 5230,
      finalistas: 40,
      becasAdjudicadas: 12,
      asistenciaPromedio: null,
    },
    porMes: [
      { mes: '2026-09', postulaciones: 3000, finalistas: 0, adjudicadas: 0, asistencia: null },
      { mes: '2026-10', postulaciones: 2230, finalistas: 40, adjudicadas: 12, asistencia: null },
    ],
    actualizadoEn: '2026-11-13T06:00:00Z',
    sinDatos: false,
    ...extra,
  }
}

/** El análisis BI sin datos: la sección no se muestra (así los tests de los totales no ven textos repetidos). */
export function biVacio(): Bi {
  return {
    ...biConDatos(),
    facultades: [],
    convocatorias: [],
    puntaje: [],
    tendencia: [],
    sinDatos: true,
  }
}

export function biConDatos(extra: Partial<Bi> = {}): Bi {
  return {
    gestion: { codigo: 'g-2', etiqueta: '2026-II' },
    convocatoria: null,
    embudo: {
      postulaciones: 5230,
      precalificadas: 5000,
      finalistas: 40,
      adjudicadas: 12,
      becasVigentes: 10,
    },
    facultades: [
      {
        nombre: 'Facultad de Ingeniería',
        postulaciones: 3000,
        adjudicadas: 8,
        porcentajePostulaciones: 57.4,
        tasaAdjudicacion: 0.3,
        carreras: [{ nombre: 'Informática', postulaciones: 2000, adjudicadas: 5 }],
      },
      {
        nombre: 'Facultad de Ciencias',
        postulaciones: 2230,
        adjudicadas: 4,
        porcentajePostulaciones: 42.6,
        tasaAdjudicacion: 0.2,
        carreras: [],
      },
    ],
    convocatorias: [
      {
        codigo: 'c-a',
        nombre: 'Beca A',
        cupos: 15,
        postulaciones: 3000,
        finalistas: 20,
        adjudicadas: 8,
        tasaAdjudicacion: 0.3,
        puntajePromedio: 41.2,
        variacionPostulaciones: null,
        seleccionada: false,
      },
      {
        codigo: 'c-b',
        nombre: 'Beca B',
        cupos: 15,
        postulaciones: 2230,
        finalistas: 20,
        adjudicadas: 4,
        tasaAdjudicacion: 0.2,
        puntajePromedio: 39,
        variacionPostulaciones: -25.7,
        seleccionada: false,
      },
    ],
    puntaje: [0, 10, 20, 30, 40, 50, 60].map((d) => ({
      desde: d,
      hasta: d + 10,
      cantidad: d === 30 ? 100 : 0,
    })),
    puntajePromedio: 41.2,
    cobertura: {
      cupos: 30,
      becasVigentes: 10,
      becasSuspendidas: 1,
      becasBaja: 1,
      reemplazos: 0,
      porcentaje: 33.3,
    },
    asistencia: {
      tasaPromedio: 80,
      diasConRegistro: 12,
      becasEnRiesgo: 2,
      porDiaSemana: [1, 2, 3, 4, 5, 6, 7].map((dia) => ({ dia, tasa: dia < 6 ? 80 : null })),
    },
    tendencia: [
      {
        codigo: 'g-1',
        etiqueta: '2026-I',
        postulaciones: 4000,
        adjudicadas: 10,
        variacionPostulaciones: null,
        seleccionada: false,
      },
      {
        codigo: 'g-2',
        etiqueta: '2026-II',
        postulaciones: 5230,
        adjudicadas: 12,
        variacionPostulaciones: 30.8,
        seleccionada: true,
      },
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
    bi?: Bi
    porSeleccion?: (seleccion: Record<string, string>) => Indicadores | Error
  } = {}
) {
  cliente.get.mockImplementation((ruta: string, config?: { params?: Record<string, string> }) => {
    switch (ruta) {
      case '/dashboard/filtros':
        return Promise.resolve({ data: opciones.filtros ?? FILTROS })
      case '/dashboard/indicadores': {
        const resultado =
          opciones.porSeleccion?.(config?.params ?? {}) ?? opciones.indicadores ?? indicadores()
        return resultado instanceof Error
          ? Promise.reject(resultado)
          : Promise.resolve({ data: resultado })
      }
      case '/dashboard/bi':
        return Promise.resolve({ data: opciones.bi ?? biVacio() })
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
      <DashboardScreen
        titulo="Dashboard de indicadores"
        activeHref="/dashboard"
        usuarioActual={yo}
      />
    </QueryClientProvider>
  )
  return { ...vista, queryClient, user: userEvent.setup() }
}
