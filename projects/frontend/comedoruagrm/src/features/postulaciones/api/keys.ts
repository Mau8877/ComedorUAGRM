import type { MisPostulacionesParams } from '../types'

// Fábrica de queryKey del módulo -- ver TANSTACK_QUERY_FRONTEND.md. Las mutaciones de documentos (CU16) invalidan
// `documentos(codigo)`; el resto son lecturas.
export const postulacionesKeys = {
  all: ['postulaciones'] as const,
  mias: (params: MisPostulacionesParams) => [...postulacionesKeys.all, 'mias', params] as const,
  detalle: (codigo: string) => [...postulacionesKeys.all, 'detalle', codigo] as const,
  documentos: (codigo: string) => [...postulacionesKeys.all, 'documentos', codigo] as const,
  miBorrador: () => [...postulacionesKeys.all, 'mi-borrador'] as const,
}
