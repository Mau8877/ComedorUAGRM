import { apiClient } from '@/store'

import type { AccesoActa, Acta, ActasParams, ConvocatoriaPendienteActa, Pagina, PendientesParams } from '../types'

// * Funciones sin hook: las usan los hooks de este mismo directorio. Todas van por el apiClient compartido,
// * que ya desempaqueta el sobre del backend (con `meta` devuelve { data, meta }).
// * Generar no lleva clave de idempotencia: una segunda petición la resuelve el backend (ERR_ACT_02) y los
// * botones se deshabilitan mientras se envía.

/** Los parámetros con valor: lo vacío o `undefined` no viaja en la URL. */
function consulta(params: Record<string, string | number | undefined>): Record<string, string | number> {
  const limpios: Record<string, string | number> = {}
  for (const [clave, valor] of Object.entries(params)) {
    if (valor !== undefined && valor !== '') limpios[clave] = valor
  }
  return limpios
}

export async function listarActasApi(params: ActasParams): Promise<Pagina<Acta>> {
  const respuesta = await apiClient.get<Pagina<Acta>>('/actas', {
    params: consulta({ page: params.page, pageSize: params.pageSize, search: params.search, sort: params.sort }),
  })
  return respuesta.data
}

export async function listarPendientesApi(params: PendientesParams): Promise<Pagina<ConvocatoriaPendienteActa>> {
  const respuesta = await apiClient.get<Pagina<ConvocatoriaPendienteActa>>('/actas/pendientes', {
    params: consulta({ page: params.page, pageSize: params.pageSize }),
  })
  return respuesta.data
}

export async function obtenerActaApi(convocatoria: string): Promise<Acta> {
  const respuesta = await apiClient.get<Acta>(`/convocatorias/${convocatoria}/acta`)
  return respuesta.data
}

/** Pide la dirección temporal del PDF y anota la descarga en la bitácora: no se guarda en caché. */
export async function obtenerAccesoActaApi(convocatoria: string): Promise<AccesoActa> {
  const respuesta = await apiClient.get<AccesoActa>(`/convocatorias/${convocatoria}/acta/descarga`)
  return respuesta.data
}

/** Genera el acta: es definitiva, no se puede regenerar ni eliminar. */
export async function generarActaApi(convocatoria: string): Promise<Acta> {
  const respuesta = await apiClient.post<Acta>(`/convocatorias/${convocatoria}/acta`)
  return respuesta.data
}

/** El PDF borrador como `Blob`; los errores del sobre llegan como `ApiError` (el apiClient los lee del blob). */
export async function obtenerVistaPreviaApi(convocatoria: string): Promise<Blob> {
  const respuesta = await apiClient.get<Blob>(`/convocatorias/${convocatoria}/acta/vista-previa`, {
    responseType: 'blob',
  })
  return respuesta.data
}
