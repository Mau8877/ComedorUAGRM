import type { MisPostulacionesParams } from '../types'

// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md. Solo lecturas: nada se invalida.
export const postulacionesKeys = {
  all: ['postulaciones'] as const,
  mias: (params: MisPostulacionesParams) => [...postulacionesKeys.all, 'mias', params] as const,
  detalle: (codigo: string) => [...postulacionesKeys.all, 'detalle', codigo] as const,
}
