// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md. Es solo lectura: no hay mutaciones que invaliden.
import type { SeleccionDashboard } from '../types'

export const dashboardKeys = {
  all: ['dashboard'] as const,
  filtros: () => [...dashboardKeys.all, 'filtros'] as const,
  indicadores: (params: SeleccionDashboard) => [...dashboardKeys.all, 'indicadores', params] as const,
}
