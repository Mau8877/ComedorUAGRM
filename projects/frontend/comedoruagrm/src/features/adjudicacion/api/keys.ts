// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md.
export const adjudicacionKeys = {
  all: ['adjudicacion'] as const,
  panels: () => [...adjudicacionKeys.all, 'panel'] as const,
  panel: (convocatoriaCodigo: string) => [...adjudicacionKeys.panels(), convocatoriaCodigo] as const,
}
