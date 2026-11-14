import { apiClient } from '@/store'

import type { MiPostulacionDetalle, MiPostulacionResumen, MisPostulacionesParams, Pagina } from '../types'

// * Funciones sin hook: las usan los hooks de este mismo directorio. Todas van por el apiClient compartido, que ya
// * desempaqueta el sobre del backend (con `meta` devuelve { data, meta }). Son solo lecturas: no hay mutaciones.

/** Los parámetros con valor: lo vacío o `undefined` no viaja en la URL. */
function consulta(params: Record<string, string | number | undefined>): Record<string, string | number> {
  const limpios: Record<string, string | number> = {}
  for (const [clave, valor] of Object.entries(params)) {
    if (valor !== undefined && valor !== '') limpios[clave] = valor
  }
  return limpios
}

export async function listarMisPostulacionesApi(params: MisPostulacionesParams): Promise<Pagina<MiPostulacionResumen>> {
  const respuesta = await apiClient.get<Pagina<MiPostulacionResumen>>('/postulaciones/mias', {
    params: consulta({
      page: params.page,
      pageSize: params.pageSize,
      'filter[gestionAcademica]': params.gestionAcademica,
      sort: params.sort,
    }),
  })
  return respuesta.data
}

export async function obtenerPostulacionApi(codigo: string): Promise<MiPostulacionDetalle> {
  const respuesta = await apiClient.get<MiPostulacionDetalle>(`/postulaciones/${codigo}`)
  return respuesta.data
}
