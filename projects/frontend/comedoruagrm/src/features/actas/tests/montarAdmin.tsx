// Ayudas de los tests de la pantalla «Actas»: montan la pantalla con un apiClient simulado y un QueryClient
// limpio, y responden cada ruta del backend con lo que se les pasa.
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

import { ActasScreen } from '../screens/ActasScreen'
import type { Acta, ConvocatoriaPendienteActa } from '../types'

import { cliente } from './clienteSimulado'

export { cliente }

export const yo = {
  codigo: 'yo',
  nombres: 'Admin',
  apellidos: 'Principal',
  correo: 'admin@dubss.com',
  rol: 'ADMINISTRADOR_DUBSS',
}

export function acta(n: number, extra: Partial<Acta> = {}): Acta {
  return {
    codigo: `acta-${n}`,
    numero: `00${n}/2026`,
    convocatoria: { codigo: `conv-${n}`, nombre: `Beca ${n}`, ...extra.convocatoria },
    gestion: '2026-II',
    adjudicados: 3,
    generadaEn: '2026-11-13T15:30:00Z',
    generadaPor: 'Admin DUBSS',
    ...extra,
  }
}

export function pendiente(n: number, extra: Partial<ConvocatoriaPendienteActa> = {}): ConvocatoriaPendienteActa {
  return { codigo: `pend-${n}`, nombre: `Pendiente ${n}`, publicadoEn: '2026-11-12T12:00:00Z', adjudicados: 4, ...extra }
}

type Meta = Partial<{ page: number; pageSize: number; totalItems: number; totalPages: number }>

/** Responde cada ruta de GET con lo que se le pasa; lo que no se pasa responde vacío. */
export function responder(
  opciones: { actas?: Acta[]; meta?: Meta; pendientes?: ConvocatoriaPendienteActa[] } = {},
) {
  const actas = opciones.actas ?? [acta(1), acta(2)]
  const pendientes = opciones.pendientes ?? []
  cliente.get.mockImplementation((ruta: string) => {
    if (ruta === '/actas') {
      return Promise.resolve({
        data: { data: actas, meta: { page: 1, pageSize: 20, totalItems: actas.length, totalPages: 1, ...opciones.meta } },
      })
    }
    if (ruta === '/actas/pendientes') {
      return Promise.resolve({
        data: { data: pendientes, meta: { page: 1, pageSize: 20, totalItems: pendientes.length, totalPages: 1 } },
      })
    }
    return Promise.reject(new Error(`ruta sin simular: ${ruta}`))
  })
}

/** Parámetros de la última consulta de actas. */
export function ultimaConsulta(): Record<string, unknown> {
  const llamadas = cliente.get.mock.calls.filter((c) => c[0] === '/actas')
  return llamadas[llamadas.length - 1][1].params
}

export function reiniciarCliente() {
  Object.values(cliente).forEach((fn) => fn.mockReset())
}

export function montar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const vista = render(
    <QueryClientProvider client={queryClient}>
      <ActasScreen titulo="Actas de adjudicación" activeHref="/actas" usuarioActual={yo} />
    </QueryClientProvider>,
  )
  return { ...vista, queryClient, user: userEvent.setup() }
}
