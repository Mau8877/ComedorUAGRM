// Ayudas de los tests de la pantalla «Bitácora»: montan la pantalla con un apiClient simulado y un QueryClient
// limpio, y responden cada ruta del backend con lo que se les pasa.
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

import { BitacoraScreen } from '../screens/BitacoraScreen'
import type { RegistroBitacora } from '../types'

import { cliente } from './clienteSimulado'

export { cliente }

export const yo = {
  codigo: 'yo',
  nombres: 'Admin',
  apellidos: 'Principal',
  correo: 'admin@dubss.com',
  rol: 'ADMINISTRADOR_DUBSS',
}

export function registro(n: number, extra: Partial<RegistroBitacora> = {}): RegistroBitacora {
  return {
    codigo: `bit-${n}`,
    fecha: '2026-09-30T14:42:05Z',
    usuario: { codigo: 'u1', nombres: `Ana${n}`, apellidos: 'Pérez', correo: 'ana@uagrm.edu.bo' },
    accion: `ACCION_${n}`,
    entidad: 'adjudicacion',
    detalle: { convocatoria: 'abc' },
    ip: '10.0.3.21',
    ...extra,
  }
}

type Meta = Partial<{ page: number; pageSize: number; totalItems: number; totalPages: number }>

/** Responde cada ruta de GET con lo que se le pasa; lo que no se pasa responde vacío. */
export function responder(opciones: { filas?: RegistroBitacora[]; meta?: Meta; acciones?: string[] } = {}) {
  const filas = opciones.filas ?? [registro(1), registro(2)]
  const acciones = opciones.acciones ?? ['ADJUDICACION_BECA', 'INICIO_SESION']
  cliente.get.mockImplementation((ruta: string) => {
    if (ruta === '/bitacora') {
      return Promise.resolve({
        data: { data: filas, meta: { page: 1, pageSize: 20, totalItems: filas.length, totalPages: 1, ...opciones.meta } },
      })
    }
    if (ruta === '/bitacora/acciones') return Promise.resolve({ data: acciones })
    return Promise.reject(new Error(`ruta sin simular: ${ruta}`))
  })
}

/** Parámetros de la última consulta de la bitácora. */
export function ultimaConsulta(): Record<string, unknown> {
  const llamadas = cliente.get.mock.calls.filter((c) => c[0] === '/bitacora')
  return llamadas[llamadas.length - 1][1].params
}

export function consultas(): number {
  return cliente.get.mock.calls.filter((c) => c[0] === '/bitacora').length
}

export function reiniciarCliente() {
  Object.values(cliente).forEach((fn) => fn.mockReset())
}

export function montar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const vista = render(
    <QueryClientProvider client={queryClient}>
      <BitacoraScreen titulo="Bitácora" activeHref="/bitacora" usuarioActual={yo} />
    </QueryClientProvider>,
  )
  return { ...vista, queryClient, user: userEvent.setup() }
}
