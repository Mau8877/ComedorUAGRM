// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md. Solo lectura: no hay mutaciones que invaliden.
export const panelKeys = {
  all: ['panel'] as const,
  administrador: () => [...panelKeys.all, 'administrador'] as const,
  comedor: () => [...panelKeys.all, 'comedor'] as const,
  postulante: () => [...panelKeys.all, 'postulante'] as const,
}
