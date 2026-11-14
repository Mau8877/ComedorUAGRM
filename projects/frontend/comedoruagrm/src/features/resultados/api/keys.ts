import type { AdjudicadosParams, DecisionesParams, PublicadosParams } from '../types'

// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md.
export const resultadosKeys = {
  all: ['resultados'] as const,
  resumen: (convocatoriaCodigo: string) => [...resultadosKeys.all, 'resumen', convocatoriaCodigo] as const,
  decisiones: (convocatoriaCodigo: string, params: DecisionesParams) =>
    [...resultadosKeys.all, 'decisiones', convocatoriaCodigo, params] as const,
  publicados: (params: PublicadosParams) => [...resultadosKeys.all, 'publicados', params] as const,
  adjudicados: (convocatoriaCodigo: string, params: AdjudicadosParams) =>
    [...resultadosKeys.all, 'adjudicados', convocatoriaCodigo, params] as const,
  mio: (convocatoriaCodigo: string) => [...resultadosKeys.all, 'mio', convocatoriaCodigo] as const,
}
