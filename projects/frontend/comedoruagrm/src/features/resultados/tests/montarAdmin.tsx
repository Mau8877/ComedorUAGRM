// Ayudas de los tests de la pantalla «Publicar resultados»: montan la pantalla con un apiClient simulado y un
// QueryClient limpio, y responden cada ruta del backend con lo que se les pasa.
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

import { PublicarResultadosScreen } from '../screens/PublicarResultadosScreen'
import type { DecisionResultado, ResumenPublicacion } from '../types'

import { cliente } from './clienteSimulado'

export { cliente }

export const yo = { codigo: 'yo', nombres: 'Admin', apellidos: 'Principal', correo: 'admin@dubss.com', rol: 'ADMINISTRADOR_DUBSS' }

export const CONVOCATORIAS = [
  { codigo: 'conv-2', nombre: 'Beca 2027-I', gestion: '2027-I', estado: 'CERRADA', fechaApertura: '', fechaCierre: '' },
  { codigo: 'conv-1', nombre: 'Beca 2026-II', gestion: '2026-II', estado: 'CERRADA', fechaApertura: '', fechaCierre: '' },
]

export function resumen(extra: Partial<ResumenPublicacion> = {}): ResumenPublicacion {
  return {
    convocatoria: { codigo: 'conv-2', nombre: 'Beca 2027-I', estado: 'CERRADA' },
    adjudicados: 3,
    noAdjudicados: 2,
    calificadasPendientes: 0,
    totalFinalistas: 8,
    aAvisar: 5,
    yaPublicada: false,
    publicadoEn: null,
    publicadoPor: null,
    puedePublicar: true,
    ...extra,
  }
}

export function decision(n: number, extra: Partial<DecisionResultado> = {}): DecisionResultado {
  return {
    finalista: `fin-${n}`,
    nombres: `Nombre${n}`,
    apellidos: `Apellido${n}`,
    resultado: 'ADJUDICADA',
    puntajeFinal: 90 - n,
    fechaAdjudicacion: '2026-11-10T10:00:00Z',
    ...extra,
  }
}

type Meta = Partial<{ page: number; pageSize: number; totalItems: number; totalPages: number }>

/** Responde cada ruta de GET con lo que se le pasa; lo que no se pasa responde vacío o falla. */
export function responder(
  opciones: {
    convocatorias?: typeof CONVOCATORIAS
    resumenes?: Record<string, ResumenPublicacion>
    filas?: DecisionResultado[]
    meta?: Meta
  } = {},
) {
  const filas = opciones.filas ?? [decision(1), decision(2)]
  const resumenes = opciones.resumenes ?? { 'conv-2': resumen(), 'conv-1': resumen({ adjudicados: 1 }) }
  cliente.get.mockImplementation((ruta: string) => {
    if (ruta === '/lista-priorizada/convocatorias') {
      return Promise.resolve({ data: opciones.convocatorias ?? CONVOCATORIAS })
    }
    const resumenRuta = /^\/convocatorias\/(.+)\/resultados\/resumen$/.exec(ruta)
    if (resumenRuta) {
      const respuesta = resumenes[resumenRuta[1]]
      return respuesta ? Promise.resolve({ data: respuesta }) : Promise.reject(new Error(`sin resumen: ${ruta}`))
    }
    if (/^\/convocatorias\/(.+)\/resultados\/decisiones$/.test(ruta)) {
      return Promise.resolve({
        data: { data: filas, meta: { page: 1, pageSize: 20, totalItems: filas.length, totalPages: 1, ...opciones.meta } },
      })
    }
    return Promise.reject(new Error(`ruta sin simular: ${ruta}`))
  })
}

/** Parámetros de la última consulta de decisiones. */
export function ultimaConsulta(): Record<string, unknown> {
  const llamadas = cliente.get.mock.calls.filter((c) => String(c[0]).endsWith('/resultados/decisiones'))
  return llamadas[llamadas.length - 1][1].params
}

export function reiniciarCliente() {
  Object.values(cliente).forEach((fn) => fn.mockReset())
}

export function montar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const vista = render(
    <QueryClientProvider client={queryClient}>
      <PublicarResultadosScreen titulo="Publicar resultados" activeHref="/publicar-resultados" usuarioActual={yo} />
    </QueryClientProvider>,
  )
  return { ...vista, queryClient, user: userEvent.setup() }
}
