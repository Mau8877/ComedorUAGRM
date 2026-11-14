// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md.

export const authKeys = {
  all: ['auth'] as const,
  usuarioActual: ['auth', 'usuario-actual'] as const,
}
