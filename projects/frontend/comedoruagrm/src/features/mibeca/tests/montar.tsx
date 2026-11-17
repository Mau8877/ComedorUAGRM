// Ayudas de los tests de la pantalla «Mi beca»: montan la pantalla con un apiClient simulado y un QueryClient
// limpio, y responden cada ruta del backend con lo que se les pasa.
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

import { ApiError } from '@/store'

import { MiBecaScreen } from '../screens/MiBecaScreen'
import type { AsistenciaBecado, MiBeca } from '../types'

import { cliente } from './clienteSimulado'

export { cliente }

export const yo = {
  codigo: 'yo',
  nombres: 'Ana',
  apellidos: 'Rojas',
  correo: 'ana@uagrm.edu.bo',
  rol: 'POSTULANTE',
}

export function beca(extra: Partial<MiBeca> = {}): MiBeca {
  return {
    codigo: 'beca-1',
    estado: 'VIGENTE',
    convocatoria: 'Convocatoria 2026-II',
    gestion: '2026-II',
    fechaAdjudicacion: '2026-08-28T14:00:00Z',
    fechaInicio: '2026-09-01',
    fechaFin: null,
    motivo: null,
    totalAsistencias: 13,
    ...extra,
  }
}

export function asistencia(dia: number): AsistenciaBecado {
  const dd = String(dia).padStart(2, '0')
  return { fecha: `2026-09-${dd}`, horaRegistro: `2026-09-${dd}T16:30:00Z` }
}

type Meta = Partial<{ page: number; pageSize: number; totalItems: number; totalPages: number }>

/** Responde cada ruta de GET con lo que se le pasa; `beca: null` simula el 404 ERR_MBE_01. */
export function responder(opciones: { beca?: MiBeca | null; filas?: AsistenciaBecado[]; meta?: Meta } = {}) {
  const filas = opciones.filas ?? [asistencia(18), asistencia(17)]
  const miBeca = opciones.beca === undefined ? beca() : opciones.beca
  cliente.get.mockImplementation((ruta: string) => {
    if (ruta === '/mi-beca') {
      return miBeca
        ? Promise.resolve({ data: miBeca })
        : Promise.reject(new ApiError('ERR_MBE_01', 'No cuentas con una beca'))
    }
    if (ruta === '/mi-beca/asistencias') {
      return Promise.resolve({
        data: { data: filas, meta: { page: 1, pageSize: 20, totalItems: filas.length, totalPages: 1, ...opciones.meta } },
      })
    }
    return Promise.reject(new Error(`ruta sin simular: ${ruta}`))
  })
}

/** Parámetros de la última consulta del historial. */
export function ultimaConsulta(): Record<string, unknown> {
  const llamadas = cliente.get.mock.calls.filter((c) => c[0] === '/mi-beca/asistencias')
  return llamadas[llamadas.length - 1][1].params
}

export function consultasHistorial(): number {
  return cliente.get.mock.calls.filter((c) => c[0] === '/mi-beca/asistencias').length
}

export function reiniciarCliente() {
  Object.values(cliente).forEach((fn) => fn.mockReset())
}

export function montar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const vista = render(
    <QueryClientProvider client={queryClient}>
      <MiBecaScreen titulo="Mi beca" activeHref="/mi-beca" usuarioActual={yo} />
    </QueryClientProvider>,
  )
  return { ...vista, queryClient, user: userEvent.setup() }
}
