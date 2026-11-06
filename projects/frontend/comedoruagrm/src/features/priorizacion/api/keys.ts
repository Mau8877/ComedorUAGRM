// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md.
import type { ListaPriorizadaParams } from '../types'

export const priorizacionKeys = {
  all: ['priorizacion'] as const,
  convocatorias: () => [...priorizacionKeys.all, 'convocatorias'] as const,
  resumen: (convocatoriaId: string) => [...priorizacionKeys.all, 'resumen', convocatoriaId] as const,
  lists: () => [...priorizacionKeys.all, 'list'] as const,
  list: (params: ListaPriorizadaParams) => [...priorizacionKeys.lists(), params] as const,
}
