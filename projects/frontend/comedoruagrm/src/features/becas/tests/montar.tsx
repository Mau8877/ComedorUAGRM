// Ayudas de los tests de la pantalla «Padrón de becados»: montan la pantalla con un apiClient simulado y un
// QueryClient limpio, y responden cada ruta del backend con lo que se les pasa.
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

import { PadronScreen } from '../screens/PadronScreen'
import type { Beca, CandidatoReemplazo, EstadoBeca, ResumenPadron } from '../types'

import { cliente, convocatoriasPublicadas } from './clienteSimulado'

export { cliente }

export const yo = {
  codigo: 'yo',
  nombres: 'Admin',
  apellidos: 'Principal',
  correo: 'admin@dubss.com',
  rol: 'ADMINISTRADOR_DUBSS',
}

export const CONVOCATORIA = 'conv-1'

export function beca(n: number, estado: EstadoBeca = 'VIGENTE', extra: Partial<Beca> = {}): Beca {
  return {
    codigo: `beca-${n}`,
    becado: { codigo: `post-${n}`, nombres: `Ana${n}`, apellidos: 'Rojas', ci: `10${n}`, carrera: 'Ing. Sistemas' },
    estado,
    fechaInicio: '2026-11-10',
    fechaFin: null,
    ...extra,
  }
}

export function resumen(extra: Partial<ResumenPadron> = {}): ResumenPadron {
  return {
    convocatoria: { codigo: CONVOCATORIA, nombre: 'Beca 2026-II' },
    resultadosPublicados: true,
    adjudicados: 2,
    pendientesDeAlta: 0,
    vigentes: 1,
    suspendidas: 0,
    bajas: 1,
    ...extra,
  }
}

export function candidato(extra: Partial<CandidatoReemplazo> = {}): CandidatoReemplazo {
  return {
    postulanteCodigo: 'post-9',
    nombres: 'Carla',
    apellidos: 'Peña',
    ci: '109',
    carrera: 'Medicina',
    posicion: 3,
    puntajeTotal: 75,
    ...extra,
  }
}

type Meta = Partial<{ page: number; pageSize: number; totalItems: number; totalPages: number }>

/** Responde cada ruta de GET con lo que se le pasa; lo que no se pasa responde con datos por defecto. */
export function responder(
  opciones: { becas?: Beca[]; meta?: Meta; resumen?: ResumenPadron; candidato?: CandidatoReemplazo } = {},
) {
  const becas = opciones.becas ?? [beca(1, 'VIGENTE'), beca(2, 'BAJA')]
  convocatoriasPublicadas.data = [{ codigo: CONVOCATORIA, nombre: 'Beca 2026-II' }]
  cliente.get.mockImplementation((ruta: string) => {
    if (ruta === `/convocatorias/${CONVOCATORIA}/padron`) {
      return Promise.resolve({
        data: { data: becas, meta: { page: 1, pageSize: 20, totalItems: becas.length, totalPages: 1, ...opciones.meta } },
      })
    }
    if (ruta === `/convocatorias/${CONVOCATORIA}/padron/resumen`) {
      return Promise.resolve({ data: opciones.resumen ?? resumen() })
    }
    if (/^\/becas\/[^/]+\/reemplazo$/.test(ruta)) return Promise.resolve({ data: opciones.candidato ?? candidato() })
    return Promise.reject(new Error(`ruta sin simular: ${ruta}`))
  })
}

/** Parámetros de la última consulta del listado del padrón. */
export function ultimaConsulta(): Record<string, unknown> {
  const llamadas = cliente.get.mock.calls.filter((c) => c[0] === `/convocatorias/${CONVOCATORIA}/padron`)
  return llamadas[llamadas.length - 1][1].params
}

export function consultas(): number {
  return cliente.get.mock.calls.filter((c) => c[0] === `/convocatorias/${CONVOCATORIA}/padron`).length
}

export function reiniciarCliente() {
  Object.values(cliente).forEach((fn) => fn.mockReset())
  convocatoriasPublicadas.data = []
}

export function montar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const vista = render(
    <QueryClientProvider client={queryClient}>
      <PadronScreen titulo="Padrón de becados" activeHref="/padron-becados" usuarioActual={yo} />
    </QueryClientProvider>,
  )
  return { ...vista, queryClient, user: userEvent.setup() }
}
