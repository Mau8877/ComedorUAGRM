// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md.

export const authKeys = {
  all: ['auth'] as const,
  usuarioActual: ['auth', 'usuario-actual'] as const,
}

export const carrerasKeys = {
  all: ['carreras'] as const,
  lists: () => [...carrerasKeys.all, 'list'] as const,
  list: (params: { pageSize: number }) => [...carrerasKeys.lists(), params] as const,
}
