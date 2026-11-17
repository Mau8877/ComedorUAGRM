// Ayudas de los tests de la pantalla «Parámetros del modelo»: montan la pantalla con un apiClient simulado y un
// QueryClient limpio, y responden cada ruta del backend con lo que se les pasa.
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

import { ParametrosModeloScreen } from '../screens/ParametrosModeloScreen'
import type { LoteReprocesamiento, ModeloPreseleccion, ModeloResumen, ResumenReprocesamiento } from '../types'

import { cliente, convocatoriasDisponibles } from './clienteSimulado'

export { cliente }

export const yo = {
  codigo: 'yo',
  nombres: 'Admin',
  apellidos: 'Principal',
  correo: 'admin@dubss.com',
  rol: 'ADMINISTRADOR_DUBSS',
}

export const CONVOCATORIA = 'conv-1'

/** La versión v1.3 del mockup: 30, 20, 15, 15, 10, 10 = 100, con las variables del formulario oficial en 0. */
export function vigente(extra: Partial<ModeloPreseleccion> = {}): ModeloPreseleccion {
  return {
    codigo: 'mod-13',
    version: 'v1.3',
    descripcion: null,
    vigente: true,
    creadoEn: '2026-07-01T15:00:00Z',
    parametros: [
      { clave: 'ingresos_familiares', nombre: 'Ingresos familiares', ponderacion: 30 },
      { clave: 'dependientes', nombre: 'Dependientes', ponderacion: 20 },
      { clave: 'procedencia', nombre: 'Procedencia', ponderacion: 15 },
      { clave: 'patrimonio', nombre: 'Patrimonio', ponderacion: 15 },
      { clave: 'estado_vulnerabilidad', nombre: 'Estado de vulnerabilidad', ponderacion: 10 },
      { clave: 'promedio_academico', nombre: 'Promedio académico', ponderacion: 10 },
      { clave: 'dependencia_economica', nombre: 'Dependencia económica', ponderacion: 0 },
      { clave: 'ocupacion_sosten', nombre: 'Ocupación de quien sostiene', ponderacion: 0 },
      { clave: 'discapacidad', nombre: 'Discapacidad', ponderacion: 0 },
      { clave: 'hijos', nombre: 'Hijos', ponderacion: 0 },
    ],
    ...extra,
  }
}

export function lote(extra: Partial<LoteReprocesamiento> = {}): LoteReprocesamiento {
  return {
    codigo: 'lote-1',
    convocatoria: CONVOCATORIA,
    modelo: 'v1.3',
    estado: 'EN_PROCESO',
    total: 10,
    procesadas: 4,
    fallidas: 0,
    iniciadoEn: '2026-11-16T15:00:00Z',
    terminadoEn: null,
    ...extra,
  }
}

export function resumen(extra: Partial<ResumenReprocesamiento> = {}): ResumenReprocesamiento {
  return {
    convocatoria: CONVOCATORIA,
    nombre: 'Convocatoria 2026-II',
    gestion: '2026-II',
    modelo: 'v1.3',
    calificadas: 120,
    porReprocesar: 118,
    resultadosPublicados: false,
    loteEnCurso: null,
    ...extra,
  }
}

export function version(n: number, extra: Partial<ModeloResumen> = {}): ModeloResumen {
  return {
    codigo: `mod-1${n}`,
    version: `v1.${n}`,
    descripcion: null,
    vigente: false,
    creadoEn: '2026-02-10T15:00:00Z',
    ...extra,
  }
}

interface Opciones {
  /** `null` = todavía no hay versión vigente. */
  vigente?: ModeloPreseleccion | null
  historial?: ModeloResumen[]
  resumen?: ResumenReprocesamiento
  lote?: LoteReprocesamiento
}

/** Responde cada ruta de GET con lo que se le pasa; lo que no se pasa responde con datos por defecto. */
export function responder(opciones: Opciones = {}) {
  const historial = opciones.historial ?? [version(3, { vigente: true, creadoEn: '2026-07-01T15:00:00Z' }), version(2)]
  convocatoriasDisponibles.data = [{ codigo: CONVOCATORIA, nombre: 'Convocatoria 2026-II' }]
  cliente.get.mockImplementation((ruta: string) => {
    if (ruta === '/modelos-preseleccion/vigente') {
      return Promise.resolve({ data: opciones.vigente === undefined ? vigente() : opciones.vigente })
    }
    if (ruta === '/modelos-preseleccion') {
      return Promise.resolve({
        data: { data: historial, meta: { page: 1, pageSize: 10, totalItems: historial.length, totalPages: 1 } },
      })
    }
    if (ruta === `/convocatorias/${CONVOCATORIA}/resumen-reprocesamiento`) {
      return Promise.resolve({ data: opciones.resumen ?? resumen() })
    }
    if (/^\/lotes-reprocesamiento\/[^/]+$/.test(ruta)) return Promise.resolve({ data: opciones.lote ?? lote() })
    return Promise.reject(new Error(`ruta sin simular: ${ruta}`))
  })
}

export function reiniciarCliente() {
  Object.values(cliente).forEach((fn) => fn.mockReset())
  convocatoriasDisponibles.data = []
}

export function montar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const vista = render(
    <QueryClientProvider client={queryClient}>
      <ParametrosModeloScreen titulo="Parámetros del modelo" activeHref="/parametros-modelo" usuarioActual={yo} />
    </QueryClientProvider>,
  )
  return { ...vista, queryClient, user: userEvent.setup() }
}
