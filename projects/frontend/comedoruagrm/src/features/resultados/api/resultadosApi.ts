import { apiClient } from '@/store'

import type {
  AdjudicadoPublico,
  AdjudicadosParams,
  ConvocatoriaPublicada,
  DecisionesParams,
  DecisionResultado,
  MiResultado,
  Pagina,
  PublicacionResultado,
  PublicadosParams,
  ResumenPublicacion,
} from '../types'

// * Funciones sin hook: las usan los hooks de este mismo directorio. Todas van por el apiClient
// * compartido, que ya desempaqueta el sobre del backend (con `meta` devuelve { data, meta }).
// * Publicar no lleva clave de idempotencia: una segunda petición la resuelve el backend (ERR_PUB_02)
// * y el botón se deshabilita mientras se envía.

/** Los parámetros con valor: lo vacío o `undefined` no viaja en la URL. */
function consulta(params: Record<string, string | number | undefined>): Record<string, string | number> {
  const limpios: Record<string, string | number> = {}
  for (const [clave, valor] of Object.entries(params)) {
    if (valor !== undefined && valor !== '') limpios[clave] = valor
  }
  return limpios
}

export async function obtenerResumenApi(convocatoria: string): Promise<ResumenPublicacion> {
  const respuesta = await apiClient.get<ResumenPublicacion>(`/convocatorias/${convocatoria}/resultados/resumen`)
  return respuesta.data
}

export async function listarDecisionesApi(
  convocatoria: string,
  params: DecisionesParams,
): Promise<Pagina<DecisionResultado>> {
  const respuesta = await apiClient.get<Pagina<DecisionResultado>>(
    `/convocatorias/${convocatoria}/resultados/decisiones`,
    {
      params: consulta({
        page: params.page,
        pageSize: params.pageSize,
        'filter[resultado]': params.resultado,
        search: params.search,
        sort: params.sort,
      }),
    },
  )
  return respuesta.data
}

/** Publica los resultados de la convocatoria: no se puede deshacer. */
export async function publicarResultadosApi(convocatoria: string): Promise<PublicacionResultado> {
  const respuesta = await apiClient.post<PublicacionResultado>(`/convocatorias/${convocatoria}/resultados/publicar`)
  return respuesta.data
}

export async function listarConvocatoriasPublicadasApi(
  params: PublicadosParams,
): Promise<Pagina<ConvocatoriaPublicada>> {
  const respuesta = await apiClient.get<Pagina<ConvocatoriaPublicada>>('/resultados/convocatorias', {
    params: consulta({ page: params.page, pageSize: params.pageSize }),
  })
  return respuesta.data
}

export async function listarAdjudicadosApi(
  convocatoria: string,
  params: AdjudicadosParams,
): Promise<Pagina<AdjudicadoPublico>> {
  const respuesta = await apiClient.get<Pagina<AdjudicadoPublico>>(`/convocatorias/${convocatoria}/resultados`, {
    params: consulta({ page: params.page, pageSize: params.pageSize, search: params.search, sort: params.sort }),
  })
  return respuesta.data
}

export async function obtenerMiResultadoApi(convocatoria: string): Promise<MiResultado> {
  const respuesta = await apiClient.get<MiResultado>(`/convocatorias/${convocatoria}/resultados/mio`)
  return respuesta.data
}
