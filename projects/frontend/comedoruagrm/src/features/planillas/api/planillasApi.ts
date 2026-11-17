import { apiClient } from '@/store'

import type { AccesoPlanilla, Pagina, PeriodoPlanilla, Planilla, PlanillasParams, ResumenPlanilla } from '../types'

// * Funciones sin hook: las usan los hooks de este mismo directorio. Todas van por el apiClient compartido,
// * que ya desempaqueta el sobre del backend (con `meta` devuelve { data, meta }).
// * Generar no lleva clave de idempotencia: una planilla es un registro de lo impreso y se puede generar más de
// * una; los botones se deshabilitan mientras se envía.

export async function obtenerResumenPlanillaApi(convocatoria: string): Promise<ResumenPlanilla> {
  const respuesta = await apiClient.get<ResumenPlanilla>(`/convocatorias/${convocatoria}/planillas/resumen`)
  return respuesta.data
}

export async function listarPlanillasApi(convocatoria: string, params: PlanillasParams): Promise<Pagina<Planilla>> {
  const respuesta = await apiClient.get<Pagina<Planilla>>(`/convocatorias/${convocatoria}/planillas`, {
    params: { page: params.page, pageSize: params.pageSize, ...(params.sort ? { sort: params.sort } : {}) },
  })
  return respuesta.data
}

/** Genera la planilla del periodo con el padrón vigente al momento de generarla. */
export async function generarPlanillaApi(convocatoria: string, periodo: PeriodoPlanilla): Promise<Planilla> {
  const respuesta = await apiClient.post<Planilla>(`/convocatorias/${convocatoria}/planillas`, periodo)
  return respuesta.data
}

/** Pide la dirección temporal del PDF y anota la descarga en la bitácora: no se guarda en caché. */
export async function obtenerAccesoPlanillaApi(planilla: string): Promise<AccesoPlanilla> {
  const respuesta = await apiClient.get<AccesoPlanilla>(`/planillas/${planilla}/descarga`)
  return respuesta.data
}

/** El PDF borrador como `Blob`; los errores del sobre llegan como `ApiError` (el apiClient los lee del blob). */
export async function obtenerVistaPreviaPlanillaApi(convocatoria: string, periodo: PeriodoPlanilla): Promise<Blob> {
  const respuesta = await apiClient.get<Blob>(`/convocatorias/${convocatoria}/planillas/vista-previa`, {
    params: { desde: periodo.fechaDesde, hasta: periodo.fechaHasta },
    responseType: 'blob',
  })
  return respuesta.data
}
