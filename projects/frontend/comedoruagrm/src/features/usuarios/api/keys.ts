// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md.
import type { UsuariosListParams } from '../types'

export const usuariosKeys = {
  all: ['usuarios'] as const,
  lists: () => [...usuariosKeys.all, 'list'] as const,
  list: (params: UsuariosListParams) => [...usuariosKeys.lists(), params] as const,
}
