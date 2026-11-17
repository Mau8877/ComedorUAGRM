import type { AsistenciasParams } from '../types'

// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md. Todo es lectura: no hay mutaciones que invalidar.
export const mibecaKeys = {
  all: ['mibeca'] as const,
  detail: () => [...mibecaKeys.all, 'detail'] as const,
  lists: () => [...mibecaKeys.all, 'list'] as const,
  list: (params: AsistenciasParams) => [...mibecaKeys.lists(), params] as const,
}
