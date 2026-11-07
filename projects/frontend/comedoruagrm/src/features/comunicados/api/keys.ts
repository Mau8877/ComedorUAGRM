// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md.
export const comunicadosKeys = {
  all: ['comunicados'] as const,
  lists: () => [...comunicadosKeys.all, 'list'] as const,
  list: (params: { page: number; pageSize: number }) => [...comunicadosKeys.lists(), params] as const,
}
