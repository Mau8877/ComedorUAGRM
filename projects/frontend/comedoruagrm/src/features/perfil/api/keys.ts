// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md.

export const perfilKeys = {
  all: ['perfil'] as const,
  detail: () => [...perfilKeys.all, 'detail'] as const,
}
