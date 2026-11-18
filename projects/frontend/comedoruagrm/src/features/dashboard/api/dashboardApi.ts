import { apiClient } from '@/store'

import type { Bi, FiltrosDashboard, Indicadores, SeleccionDashboard } from '../types'

// * Funciones sin hook: las usan los hooks de este mismo directorio. Todas van por el apiClient compartido.

/** Los filtros solo viajan si tienen valor: sin ellos el backend usa la gestión vigente y todas las convocatorias. */
function consulta(seleccion: SeleccionDashboard): Record<string, string> {
  const query: Record<string, string> = {}
  if (seleccion.gestion) query['filter[gestion]'] = seleccion.gestion
  if (seleccion.convocatoria) query['filter[convocatoria]'] = seleccion.convocatoria
  return query
}

export async function obtenerFiltrosApi(): Promise<FiltrosDashboard> {
  const respuesta = await apiClient.get<FiltrosDashboard>('/dashboard/filtros')
  return respuesta.data
}

export async function obtenerIndicadoresApi(seleccion: SeleccionDashboard): Promise<Indicadores> {
  const respuesta = await apiClient.get<Indicadores>('/dashboard/indicadores', {
    params: consulta(seleccion),
  })
  return respuesta.data
}

export interface PdfDashboard {
  archivo: Blob
  nombre: string
}

/** El nombre que manda el servidor en `Content-Disposition`; si no viene, uno genérico. */
export function nombreDeArchivo(contentDisposition: unknown): string {
  const coincidencia = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(
    String(contentDisposition ?? '')
  )
  return coincidencia ? decodeURIComponent(coincidencia[1]) : 'indicadores.pdf'
}

/** Descarga el PDF con exactamente la selección aplicada en pantalla. */
export async function descargarDashboardApi(seleccion: SeleccionDashboard): Promise<PdfDashboard> {
  const respuesta = await apiClient.get<Blob>('/dashboard/indicadores/pdf', {
    params: consulta(seleccion),
    responseType: 'blob',
  })
  return {
    archivo: respuesta.data,
    nombre: nombreDeArchivo(respuesta.headers['content-disposition']),
  }
}

export async function obtenerBiApi(seleccion: SeleccionDashboard): Promise<Bi> {
  const respuesta = await apiClient.get<Bi>('/dashboard/bi', { params: consulta(seleccion) })
  return respuesta.data
}

export interface ExcelDashboard {
  archivo: Blob
  nombre: string
}

/** Descarga el análisis BI en Excel con exactamente la selección aplicada en pantalla. */
export async function descargarBiExcelApi(seleccion: SeleccionDashboard): Promise<ExcelDashboard> {
  const respuesta = await apiClient.get<Blob>('/dashboard/bi/excel', {
    params: consulta(seleccion),
    responseType: 'blob',
  })
  const nombre = nombreDeArchivo(respuesta.headers['content-disposition']).replace(
    /\.pdf$/i,
    '.xlsx'
  )
  return { archivo: respuesta.data, nombre }
}
