// Ayudas compartidas por los tests de pantallas y modales: montan la UI con un apiClient simulado y un QueryClient
// limpio, y arman las respuestas del backend.
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'

import type {
  Convocatoria,
  ConvocatoriaDetalle,
  EstadoConvocatoria,
  GestionAcademica,
  TipoDocumento,
} from '../types'

import { cliente } from './clienteSimulado'

export { cliente }

export const yo = {
  codigo: 'yo',
  nombres: 'Admin',
  apellidos: 'Principal',
  correo: 'admin@uagrm.edu.bo',
  rol: 'ADMINISTRADOR_DUBSS',
}

export const GESTION_2026_II: GestionAcademica = {
  codigo: 'g2',
  etiqueta: '2026-II',
  fechaInicio: '2026-07-01',
  fechaFin: '2026-12-20',
}

export const GESTION_2026_I: GestionAcademica = {
  codigo: 'g1',
  etiqueta: '2026-I',
  fechaInicio: '2026-01-15',
  fechaFin: '2026-06-30',
}

export const CARNET: TipoDocumento = { codigo: 't1', nombre: 'Carnet de identidad', descripcion: null }
export const NOTAS: TipoDocumento = { codigo: 't2', nombre: 'Certificado de notas', descripcion: null }

export function convocatoria(n: number, estado: EstadoConvocatoria = 'BORRADOR', extra: Partial<Convocatoria> = {}): Convocatoria {
  return {
    codigo: `c${n}`,
    nombre: `Beca Comedor ${n}`,
    gestion: { codigo: GESTION_2026_II.codigo, etiqueta: GESTION_2026_II.etiqueta },
    fechaApertura: '2026-08-01',
    fechaCierre: '2026-08-31',
    cupos: 100,
    puntajeMinimo: 40.5,
    cantidadFinalistas: 120,
    estado,
    ...extra,
  }
}

export function detalle(n: number, estado: EstadoConvocatoria = 'BORRADOR', extra: Partial<ConvocatoriaDetalle> = {}): ConvocatoriaDetalle {
  return {
    ...convocatoria(n, estado),
    tienePostulaciones: false,
    documentos: [{ tipoDocumento: CARNET, obligatorio: true }],
    ...extra,
  }
}

type Meta = Partial<{ page: number; pageSize: number; totalItems: number; totalPages: number }>

export function pagina<T>(data: T[], meta: Meta = {}) {
  return { data: { data, meta: { page: 1, pageSize: 20, totalItems: data.length, totalPages: 1, ...meta } } }
}

/** Responde cada `GET` según su ruta: lista, detalle, gestiones y tipos de documento. */
export function simularBackend(opciones: {
  convocatorias?: Convocatoria[]
  detalles?: ConvocatoriaDetalle[]
  gestiones?: GestionAcademica[]
  tipos?: TipoDocumento[]
  meta?: Meta
}) {
  const { convocatorias = [], detalles = [], gestiones = [GESTION_2026_II, GESTION_2026_I], tipos = [CARNET, NOTAS] } = opciones
  cliente.get.mockImplementation((ruta: string) => {
    if (ruta === '/convocatorias') return Promise.resolve(pagina(convocatorias, opciones.meta))
    if (ruta.startsWith('/convocatorias/')) {
      const codigo = ruta.split('/')[2]
      const encontrada = detalles.find((d) => d.codigo === codigo)
      return encontrada ? Promise.resolve({ data: encontrada }) : Promise.reject(new Error('sin detalle'))
    }
    if (ruta === '/gestiones-academicas') return Promise.resolve(pagina(gestiones, opciones.meta))
    if (ruta === '/tipos-documento') return Promise.resolve(pagina(tipos))
    return Promise.reject(new Error(`ruta no simulada: ${ruta}`))
  })
}

/** Parámetros de la última consulta a esa ruta. */
export function ultimaConsulta(ruta: string): Record<string, unknown> {
  const llamadas = cliente.get.mock.calls.filter((c) => c[0] === ruta)
  return llamadas[llamadas.length - 1][1].params
}

export function reiniciarCliente() {
  Object.values(cliente).forEach((fn) => fn.mockReset())
}

export function montar(ui: ReactNode) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const vista = render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>)
  return { ...vista, queryClient, user: userEvent.setup() }
}
