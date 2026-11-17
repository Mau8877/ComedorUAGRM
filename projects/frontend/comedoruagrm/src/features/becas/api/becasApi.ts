import { apiClient } from '@/store'

import type {
  AltaPadron,
  Beca,
  CandidatoReemplazo,
  Pagina,
  ParamsPadron,
  Reemplazo,
  ResumenPadron,
} from '../types'

// * Funciones sin hook: las usan los hooks de este mismo directorio. Todas van por el apiClient compartido, que ya
// * desempaqueta el sobre del backend (con `meta` devuelve { data, meta }). Las rutas usan siempre el código público.

export async function obtenerResumenApi(convocatoria: string): Promise<ResumenPadron> {
  const respuesta = await apiClient.get<ResumenPadron>(`/convocatorias/${convocatoria}/padron/resumen`)
  return respuesta.data
}

export async function listarPadronApi(convocatoria: string, params: ParamsPadron): Promise<Pagina<Beca>> {
  const consulta: Record<string, string | number> = { page: params.page, pageSize: params.pageSize }
  if (params.search) consulta.search = params.search
  if (params.estado) consulta['filter[estado]'] = params.estado
  if (params.sort) consulta.sort = params.sort
  const respuesta = await apiClient.get<Pagina<Beca>>(`/convocatorias/${convocatoria}/padron`, { params: consulta })
  return respuesta.data
}

/** Crea una beca vigente por cada adjudicado que todavía no la tiene. */
export async function darDeAltaApi(convocatoria: string): Promise<AltaPadron> {
  const respuesta = await apiClient.post<AltaPadron>(`/convocatorias/${convocatoria}/padron/alta`)
  return respuesta.data
}

export async function darDeBajaApi(beca: string, motivo: string): Promise<Beca> {
  const respuesta = await apiClient.post<Beca>(`/becas/${beca}/dar-de-baja`, { motivo })
  return respuesta.data
}

export async function suspenderApi(beca: string, motivo: string): Promise<Beca> {
  const respuesta = await apiClient.post<Beca>(`/becas/${beca}/suspender`, { motivo })
  return respuesta.data
}

/** El siguiente postulante disponible para ocupar el cupo de una beca dada de baja. */
export async function obtenerCandidatoApi(beca: string): Promise<CandidatoReemplazo> {
  const respuesta = await apiClient.get<CandidatoReemplazo>(`/becas/${beca}/reemplazo`)
  return respuesta.data
}

export async function reemplazarApi(beca: string, postulanteCodigo: string): Promise<Reemplazo> {
  const respuesta = await apiClient.post<Reemplazo>(`/becas/${beca}/reemplazar`, { postulanteCodigo })
  return respuesta.data
}
