import type { ActasParams, PendientesParams } from '../types'

// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md.
export const actasKeys = {
  all: ['actas'] as const,
  lists: () => [...actasKeys.all, 'list'] as const,
  list: (params: ActasParams) => [...actasKeys.lists(), params] as const,
  pendientes: (params: PendientesParams) => [...actasKeys.all, 'pendientes', params] as const,
  detail: (convocatoriaCodigo: string) => [...actasKeys.all, 'detail', convocatoriaCodigo] as const,
}
