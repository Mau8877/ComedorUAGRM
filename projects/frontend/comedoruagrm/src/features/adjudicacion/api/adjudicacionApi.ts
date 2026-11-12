import { apiClient } from '@/store'

import type { PanelAdjudicacion, SeleccionarFinalistasRequest, SeleccionFinalistasResultado } from '../types'

// * Funciones sin hook: las usan los hooks de este mismo directorio. Todas van por el apiClient
// * compartido, que ya desempaqueta el sobre del backend. Sin clave de idempotencia: una segunda
// * petición la resuelve el backend (ERR_ADJ_01) y el botón se deshabilita mientras se envía.

export async function obtenerPanelApi(convocatoria: string): Promise<PanelAdjudicacion> {
  const respuesta = await apiClient.get<PanelAdjudicacion>(`/convocatorias/${convocatoria}/adjudicacion`)
  return respuesta.data
}

/** Selecciona a los mejores calificados que aún no son finalistas. Sin `cantidad` usa la configurada. */
export async function seleccionarFinalistasApi(
  convocatoria: string,
  pedido: SeleccionarFinalistasRequest,
): Promise<SeleccionFinalistasResultado> {
  const respuesta = await apiClient.post<SeleccionFinalistasResultado>(
    `/convocatorias/${convocatoria}/finalistas`,
    pedido,
  )
  return respuesta.data
}
