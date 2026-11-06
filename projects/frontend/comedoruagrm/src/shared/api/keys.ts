// Fábrica de queryKey de los endpoints usados por más de una feature -- ver TANSTACK_QUERY_FRONTEND.md.
export const sharedKeys = {
  all: ['shared'] as const,
  carreras: () => [...sharedKeys.all, 'carreras'] as const,
}
