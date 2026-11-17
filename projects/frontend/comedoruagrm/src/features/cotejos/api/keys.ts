import type { ListadoParams } from '../types'

// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md.
export const cotejosKeys = {
  all: ['cotejos'] as const,
  datos: (planilla: string) => [...cotejosKeys.all, 'datos', planilla] as const,
  registros: () => [...cotejosKeys.all, 'registros'] as const,
  registrosDe: (planilla: string, params: ListadoParams) => [...cotejosKeys.registros(), planilla, params] as const,
  lists: () => [...cotejosKeys.all, 'list'] as const,
  list: (planilla: string, params: ListadoParams) => [...cotejosKeys.lists(), planilla, params] as const,
}
