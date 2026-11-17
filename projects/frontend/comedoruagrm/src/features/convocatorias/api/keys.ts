// Fábricas de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md. El módulo va primero y `list`/`detail` segundo.
import type { ConvocatoriasListParams, GestionesListParams, TiposDocumentoListParams } from '../types'

export const convocatoriasKeys = {
  all: ['convocatorias'] as const,
  lists: () => [...convocatoriasKeys.all, 'list'] as const,
  list: (params: ConvocatoriasListParams) => [...convocatoriasKeys.lists(), params] as const,
  details: () => [...convocatoriasKeys.all, 'detail'] as const,
  detail: (codigo: string) => [...convocatoriasKeys.details(), codigo] as const,
}

export const gestionesKeys = {
  all: ['gestiones-academicas'] as const,
  lists: () => [...gestionesKeys.all, 'list'] as const,
  list: (params: GestionesListParams) => [...gestionesKeys.lists(), params] as const,
}

export const tiposDocumentoKeys = {
  all: ['tipos-documento'] as const,
  lists: () => [...tiposDocumentoKeys.all, 'list'] as const,
  list: (params: TiposDocumentoListParams) => [...tiposDocumentoKeys.lists(), params] as const,
}
