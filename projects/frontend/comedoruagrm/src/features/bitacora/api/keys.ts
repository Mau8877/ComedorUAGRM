import type { BitacoraParams } from '../types'

// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md.
export const bitacoraKeys = {
  all: ['bitacora'] as const,
  lists: () => [...bitacoraKeys.all, 'list'] as const,
  list: (params: BitacoraParams) => [...bitacoraKeys.lists(), params] as const,
  acciones: () => [...bitacoraKeys.all, 'acciones'] as const,
}
