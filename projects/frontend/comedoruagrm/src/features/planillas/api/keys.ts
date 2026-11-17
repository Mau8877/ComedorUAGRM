import type { PlanillasParams } from '../types'

// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md.
export const planillasKeys = {
  all: ['planillas'] as const,
  lists: () => [...planillasKeys.all, 'list'] as const,
  list: (convocatoria: string, params: PlanillasParams) => [...planillasKeys.lists(), convocatoria, params] as const,
  resumen: (convocatoria: string) => [...planillasKeys.all, 'resumen', convocatoria] as const,
}
