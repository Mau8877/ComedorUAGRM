import { useQuery } from '@tanstack/react-query'

import { panelKeys } from './keys'
import { obtenerPanelAdministradorApi, obtenerPanelComedorApi, obtenerPanelPostulanteApi } from './panelApi'

/** Resumen de la pantalla de inicio del Administrador DUBSS. */
export function usePanelAdministrador() {
  return useQuery({ queryKey: panelKeys.administrador(), queryFn: obtenerPanelAdministradorApi })
}

/** Resumen de la pantalla de inicio del Personal del Comedor. */
export function usePanelComedor() {
  return useQuery({ queryKey: panelKeys.comedor(), queryFn: obtenerPanelComedorApi })
}

/** Resumen de la pantalla de inicio del Postulante (convocatoria abierta y su avance en ella). */
export function usePanelPostulante() {
  return useQuery({ queryKey: panelKeys.postulante(), queryFn: obtenerPanelPostulanteApi })
}
