import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query'

import type { SeleccionDashboard } from '../types'
import { descargarDashboardApi, obtenerFiltrosApi, obtenerIndicadoresApi } from './dashboardApi'
import { dashboardKeys } from './keys'

/** Gestiones con sus convocatorias, para los selectores. */
export function useFiltrosDashboard() {
  return useQuery({ queryKey: dashboardKeys.filtros(), queryFn: obtenerFiltrosApi })
}

/** Los indicadores de la selección; conserva los anteriores mientras llegan los nuevos (sin parpadeo). */
export function useIndicadores(seleccion: SeleccionDashboard, habilitado = true) {
  return useQuery({
    queryKey: dashboardKeys.indicadores(seleccion),
    queryFn: () => obtenerIndicadoresApi(seleccion),
    enabled: habilitado,
    placeholderData: keepPreviousData,
  })
}

/** El PDF no se guarda en la caché: es una mutación que devuelve el archivo a quien la llama. */
export function useDescargarDashboard() {
  return useMutation({ mutationFn: (seleccion: SeleccionDashboard) => descargarDashboardApi(seleccion) })
}
