// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md.
import type { FinalistasParams } from '../types'

export const finalistasKeys = {
  all: ['finalistas'] as const,
  lists: () => [...finalistasKeys.all, 'list'] as const,
  list: (params: FinalistasParams) => [...finalistasKeys.lists(), params] as const,
}
