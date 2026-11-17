import type { ParamsPadron } from '../types'

// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md. El módulo va primero y el tipo segundo.
export const becasKeys = {
  all: ['becas'] as const,
  lists: () => [...becasKeys.all, 'list'] as const,
  list: (convocatoria: string, params: ParamsPadron) => [...becasKeys.lists(), convocatoria, params] as const,
  resumenes: () => [...becasKeys.all, 'resumen'] as const,
  resumen: (convocatoria: string) => [...becasKeys.resumenes(), convocatoria] as const,
  candidatos: () => [...becasKeys.all, 'candidato'] as const,
  candidato: (beca: string) => [...becasKeys.candidatos(), beca] as const,
}
