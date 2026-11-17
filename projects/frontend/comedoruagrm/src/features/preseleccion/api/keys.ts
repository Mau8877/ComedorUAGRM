import type { ParamsHistorial } from '../types'

// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md. El módulo va primero y el tipo segundo.
export const preseleccionKeys = {
  all: ['preseleccion'] as const,
  vigente: () => [...preseleccionKeys.all, 'vigente'] as const,
  lists: () => [...preseleccionKeys.all, 'list'] as const,
  list: (params: ParamsHistorial) => [...preseleccionKeys.lists(), params] as const,
  resumenes: () => [...preseleccionKeys.all, 'resumen'] as const,
  resumen: (convocatoria: string) => [...preseleccionKeys.resumenes(), convocatoria] as const,
  lotes: () => [...preseleccionKeys.all, 'lote'] as const,
  lote: (codigo: string) => [...preseleccionKeys.lotes(), codigo] as const,
  precalificaciones: () => [...preseleccionKeys.all, 'precalificacion'] as const,
  precalificacion: (postulacion: string) => [...preseleccionKeys.precalificaciones(), postulacion] as const,
}
