import { apiClient } from '@/store'
import type { Pagina } from '@/features/planillas'

import type { Cotejo, DatosCotejo, ListadoParams, RegistrarCotejoRequest, RegistroDigital } from '../types'

// * Funciones sin hook: las usan los hooks de este mismo directorio. Todas van por el apiClient compartido,
// * que ya desempaqueta el sobre del backend (con `meta` devuelve { data, meta }).
// * Registrar no lleva clave de idempotencia: un cotejo es un registro de lo constatado y se pueden registrar
// * varios de la misma planilla; el botón se deshabilita mientras se envía.

const params = (p: ListadoParams) => ({ page: p.page, pageSize: p.pageSize, ...(p.sort ? { sort: p.sort } : {}) })

export async function obtenerDatosCotejoApi(planilla: string): Promise<DatosCotejo> {
  const respuesta = await apiClient.get<DatosCotejo>(`/planillas/${planilla}/cotejos/datos`)
  return respuesta.data
}

/** Registros digitales del periodo de la planilla; `sort` solo por `fecha` o `apellidos`. */
export async function listarRegistrosApi(planilla: string, p: ListadoParams): Promise<Pagina<RegistroDigital>> {
  const respuesta = await apiClient.get<Pagina<RegistroDigital>>(`/planillas/${planilla}/cotejos/registros`, {
    params: params(p),
  })
  return respuesta.data
}

/** Cotejos ya registrados de la planilla; `sort` solo por `creadoEn` o `fechaCotejo`. */
export async function listarCotejosApi(planilla: string, p: ListadoParams): Promise<Pagina<Cotejo>> {
  const respuesta = await apiClient.get<Pagina<Cotejo>>(`/planillas/${planilla}/cotejos`, { params: params(p) })
  return respuesta.data
}

export async function registrarCotejoApi(planilla: string, cotejo: RegistrarCotejoRequest): Promise<Cotejo> {
  const respuesta = await apiClient.post<Cotejo>(`/planillas/${planilla}/cotejos`, cotejo)
  return respuesta.data
}
