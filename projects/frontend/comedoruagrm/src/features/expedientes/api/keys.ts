// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md.
export const expedientesKeys = {
  all: ['expedientes'] as const,
  details: () => [...expedientesKeys.all, 'detail'] as const,
  detail: (codigo: string) => [...expedientesKeys.details(), codigo] as const,
}
