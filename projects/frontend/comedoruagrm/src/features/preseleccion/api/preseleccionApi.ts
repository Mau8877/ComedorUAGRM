import { apiClient } from '@/store'

import type {
  LoteReprocesamiento,
  ModeloPreseleccion,
  ModeloResumen,
  Pagina,
  ParamsHistorial,
  ParametroModelo,
  Precalificacion,
  ResumenReprocesamiento,
} from '../types'

// * Funciones sin hook: las usan los hooks de este mismo directorio. Todas van por el apiClient compartido, que ya
// * desempaqueta el sobre del backend (con `meta` devuelve { data, meta }). Las rutas usan siempre el código público.

/** La versión vigente con sus parámetros; `null` si todavía no se guardó ninguna. */
export async function obtenerVigenteApi(): Promise<ModeloPreseleccion | null> {
  const respuesta = await apiClient.get<ModeloPreseleccion | null>('/modelos-preseleccion/vigente')
  return respuesta.data ?? null
}

export async function listarVersionesApi(params: ParamsHistorial): Promise<Pagina<ModeloResumen>> {
  const respuesta = await apiClient.get<Pagina<ModeloResumen>>('/modelos-preseleccion', {
    params: { page: params.page, pageSize: params.pageSize },
  })
  return respuesta.data
}

export interface GuardarModeloPayload {
  descripcion?: string
  parametros: Pick<ParametroModelo, 'clave' | 'ponderacion'>[]
}

/** Guarda una versión nueva (pasa a ser la vigente y archiva la anterior). */
export async function guardarModeloApi(payload: GuardarModeloPayload): Promise<ModeloPreseleccion> {
  const respuesta = await apiClient.post<ModeloPreseleccion>('/modelos-preseleccion', payload)
  return respuesta.data
}

export async function obtenerResumenReprocesamientoApi(convocatoria: string): Promise<ResumenReprocesamiento> {
  const respuesta = await apiClient.get<ResumenReprocesamiento>(
    `/convocatorias/${convocatoria}/resumen-reprocesamiento`,
  )
  return respuesta.data
}

/** Crea el lote y arranca el reprocesamiento en segundo plano (el backend responde 202). */
export async function reprocesarApi(convocatoria: string): Promise<LoteReprocesamiento> {
  const respuesta = await apiClient.post<LoteReprocesamiento>(`/convocatorias/${convocatoria}/reprocesar`)
  return respuesta.data
}

export async function obtenerLoteApi(codigo: string): Promise<LoteReprocesamiento> {
  const respuesta = await apiClient.get<LoteReprocesamiento>(`/lotes-reprocesamiento/${codigo}`)
  return respuesta.data
}

/** La precalificación con IA de una postulación (CU03): puntaje, aporte de cada variable y traza. */
export async function obtenerPrecalificacionApi(postulacion: string): Promise<Precalificacion> {
  const respuesta = await apiClient.get<Precalificacion>(`/postulaciones/${postulacion}/precalificacion`)
  return respuesta.data
}
