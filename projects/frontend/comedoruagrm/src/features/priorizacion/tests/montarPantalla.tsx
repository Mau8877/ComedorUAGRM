// Ayudas compartidas por los tests de la pantalla de la lista priorizada: montan la pantalla con un
// apiClient simulado y un QueryClient limpio.
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

import { ListaPriorizadaScreen } from '../screens/ListaPriorizadaScreen'
import type { ConvocatoriaPriorizable, PostulacionPriorizada } from '../types'

import { cliente } from './clienteSimulado'

export { cliente }

export const yo = { codigo: 'yo', nombres: 'Admin', apellidos: 'Principal', correo: 'admin@dubss.com', rol: 'ADMINISTRADOR_DUBSS' }

export const CARRERAS = [
  { codigo: 'car-a', nombre: 'Ingeniería de Sistemas', facultad: 'Tecnología' },
  { codigo: 'car-b', nombre: 'Derecho', facultad: 'Derecho' },
]

export function convocatoria(n: number, extra: Partial<ConvocatoriaPriorizable> = {}): ConvocatoriaPriorizable {
  return {
    codigo: `conv-${n}`,
    nombre: `Beca Comedor ${n}`,
    gestion: `2026-${n}`,
    estado: 'CERRADA',
    fechaApertura: '2026-07-01T00:00:00Z',
    fechaCierre: '2026-08-31T00:00:00Z',
    ...extra,
  }
}

export function fila(posicion: number, extra: Partial<PostulacionPriorizada> = {}): PostulacionPriorizada {
  return {
    codigo: `po-${posicion}`,
    posicion,
    nombres: `Nombre${posicion}`,
    apellidos: `Apellido${posicion}`,
    ci: `${9000 + posicion}`,
    carrera: { codigo: 'car-a', nombre: 'Ingeniería de Sistemas' },
    puntaje: 95 - posicion,
    fechaCalculo: '2026-09-01T10:00:00Z',
    estado: 'calificada',
    ...extra,
  }
}

type Meta = Partial<{ page: number; pageSize: number; totalItems: number; totalPages: number }>

/**
 * Responde cada ruta del backend con lo que se le pasa; lo que no se pasa responde vacío.
 * `lista` es la respuesta (con `meta`) de GET /lista-priorizada.
 */
export function responder(
  opciones: {
    convocatorias?: ConvocatoriaPriorizable[]
    resumen?: { totalCalificadas: number; totalPendientes: number }
    filas?: PostulacionPriorizada[]
    meta?: Meta
  } = {},
) {
  const filas = opciones.filas ?? [fila(1), fila(2)]
  cliente.get.mockImplementation((ruta: string) => {
    switch (ruta) {
      case '/lista-priorizada/convocatorias':
        return Promise.resolve({ data: opciones.convocatorias ?? [convocatoria(2), convocatoria(1)] })
      case '/lista-priorizada/resumen':
        return Promise.resolve({ data: opciones.resumen ?? { totalCalificadas: filas.length, totalPendientes: 3 } })
      case '/lista-priorizada':
        return Promise.resolve({
          data: { data: filas, meta: { page: 1, pageSize: 20, totalItems: filas.length, totalPages: 1, ...opciones.meta } },
        })
      case '/carreras':
        return Promise.resolve({ data: { data: CARRERAS, meta: { page: 1, pageSize: 200, totalItems: 2, totalPages: 1 } } })
      default:
        return Promise.reject(new Error(`ruta sin simular: ${ruta}`))
    }
  })
}

/** Parámetros de la última consulta de la lista (no del resumen ni de las convocatorias). */
export function ultimaConsulta(): Record<string, unknown> {
  const llamadas = cliente.get.mock.calls.filter((c) => c[0] === '/lista-priorizada')
  return llamadas[llamadas.length - 1][1].params
}

export function reiniciarCliente() {
  Object.values(cliente).forEach((fn) => fn.mockReset())
}

export function montar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const vista = render(
    <QueryClientProvider client={queryClient}>
      <ListaPriorizadaScreen titulo="Lista priorizada" activeHref="/lista-priorizada" usuarioActual={yo} />
    </QueryClientProvider>,
  )
  return { ...vista, queryClient, user: userEvent.setup() }
}
