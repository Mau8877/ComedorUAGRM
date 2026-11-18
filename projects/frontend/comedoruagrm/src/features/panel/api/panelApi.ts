import { apiClient } from '@/store'

import type { PanelAdministrador, PanelComedor, PanelPostulante } from '../types'

export async function obtenerPanelAdministradorApi(): Promise<PanelAdministrador> {
  const respuesta = await apiClient.get<PanelAdministrador>('/panel/administrador')
  return respuesta.data
}

export async function obtenerPanelComedorApi(): Promise<PanelComedor> {
  const respuesta = await apiClient.get<PanelComedor>('/panel/comedor')
  return respuesta.data
}

export async function obtenerPanelPostulanteApi(): Promise<PanelPostulante> {
  const respuesta = await apiClient.get<PanelPostulante>('/panel/postulante')
  return respuesta.data
}
