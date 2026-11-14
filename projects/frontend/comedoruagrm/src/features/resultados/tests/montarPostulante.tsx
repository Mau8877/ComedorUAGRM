// Ayudas de los tests de la pantalla «Resultados» del postulante: montan la pantalla con un apiClient simulado y un
// QueryClient limpio, y responden cada ruta del backend con lo que se les pasa.
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

import { ResultadosScreen } from '../screens/ResultadosScreen'
import type { AdjudicadoPublico, ConvocatoriaPublicada, MiResultado } from '../types'

import { cliente } from './clienteSimulado'

export { cliente }

export const yo = { codigo: 'yo', nombres: 'Ana', apellidos: 'Postulante', correo: 'ana@uagrm.edu.bo', rol: 'POSTULANTE' }

export function publicada(n: number, extra: Partial<ConvocatoriaPublicada> = {}): ConvocatoriaPublicada {
  return { codigo: `conv-${n}`, nombre: `Beca Comedor ${n}`, publicadoEn: '2026-11-12T15:00:00Z', ...extra }
}

export function adjudicado(n: number, extra: Partial<AdjudicadoPublico> = {}): AdjudicadoPublico {
  return {
    nombres: `Nombre${n}`,
    apellidos: `Apellido${n}`,
    nota: 90 - n,
    carrera: 'Ingeniería de Sistemas',
    facultad: 'Facultad de Ciencias y Tecnología',
    ...extra,
  }
}

export function mio(extra: Partial<MiResultado> = {}): MiResultado {
  return { resultado: 'ADJUDICADA', nota: 88.5, publicadoEn: '2026-11-12T15:00:00Z', ...extra }
}

type Meta = Partial<{ page: number; pageSize: number; totalItems: number; totalPages: number }>

/** Responde cada ruta de GET con lo que se le pasa; lo que no se pasa responde vacío. */
export function responder(
  opciones: {
    publicadas?: ConvocatoriaPublicada[]
    mio?: MiResultado
    filas?: AdjudicadoPublico[]
    meta?: Meta
  } = {},
) {
  const filas = opciones.filas ?? [adjudicado(1), adjudicado(2)]
  const publicadas = opciones.publicadas ?? [publicada(2), publicada(1)]
  cliente.get.mockImplementation((ruta: string) => {
    if (ruta === '/resultados/convocatorias') {
      return Promise.resolve({
        data: { data: publicadas, meta: { page: 1, pageSize: 50, totalItems: publicadas.length, totalPages: 1 } },
      })
    }
    if (/^\/convocatorias\/(.+)\/resultados\/mio$/.test(ruta)) return Promise.resolve({ data: opciones.mio ?? mio() })
    if (/^\/convocatorias\/(.+)\/resultados$/.test(ruta)) {
      return Promise.resolve({
        data: { data: filas, meta: { page: 1, pageSize: 20, totalItems: filas.length, totalPages: 1, ...opciones.meta } },
      })
    }
    return Promise.reject(new Error(`ruta sin simular: ${ruta}`))
  })
}

/** Parámetros de la última consulta de la lista de adjudicados. */
export function ultimaConsulta(): Record<string, unknown> {
  const llamadas = cliente.get.mock.calls.filter((c) => /^\/convocatorias\/[^/]+\/resultados$/.test(String(c[0])))
  return llamadas[llamadas.length - 1][1].params
}

export function reiniciarCliente() {
  Object.values(cliente).forEach((fn) => fn.mockReset())
}

export function montar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const vista = render(
    <QueryClientProvider client={queryClient}>
      <ResultadosScreen titulo="Resultados" activeHref="/resultados" usuarioActual={yo} />
    </QueryClientProvider>,
  )
  return { ...vista, queryClient, user: userEvent.setup() }
}
