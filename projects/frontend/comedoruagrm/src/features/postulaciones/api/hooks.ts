import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { MisPostulacionesParams } from '../types'
import {
  accesoDocumentoApi,
  adjuntarDocumentoApi,
  eliminarDocumentoApi,
  listarDocumentosApi,
  listarMisPostulacionesApi,
  obtenerPostulacionApi,
  reemplazarDocumentoApi,
} from './postulacionesApi'
import { postulacionesKeys } from './keys'

/** Una página de «Mis postulaciones»; conserva la anterior mientras llega la siguiente. */
export function useMisPostulaciones(params: MisPostulacionesParams) {
  return useQuery({
    queryKey: postulacionesKeys.mias(params),
    queryFn: () => listarMisPostulacionesApi(params),
    placeholderData: keepPreviousData,
  })
}

/** El detalle de una postulación propia; no consulta mientras no haya código. */
export function usePostulacion(codigo: string | undefined) {
  return useQuery({
    queryKey: postulacionesKeys.detalle(codigo as string),
    queryFn: () => obtenerPostulacionApi(codigo as string),
    enabled: Boolean(codigo),
  })
}

// ---- Documentos de la postulación (CU16) ----

/** Los documentos que exige la convocatoria y su estado; no consulta mientras no haya código. */
export function useDocumentos(codigo: string | undefined) {
  return useQuery({
    queryKey: postulacionesKeys.documentos(codigo as string),
    queryFn: () => listarDocumentosApi(codigo as string),
    enabled: Boolean(codigo),
  })
}

/** Vuelve a pedir la lista de documentos (después de subir, reemplazar o eliminar, o si la postulación cambió). */
export function useInvalidarDocumentos(codigo: string) {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: postulacionesKeys.documentos(codigo) })
}

export function useAdjuntarDocumento(codigo: string) {
  const invalidar = useInvalidarDocumentos(codigo)
  return useMutation({
    mutationFn: ({ tipoDocumento, archivo }: { tipoDocumento: string; archivo: File }) =>
      adjuntarDocumentoApi(codigo, tipoDocumento, archivo),
    onSuccess: invalidar,
  })
}

export function useReemplazarDocumento(codigo: string) {
  const invalidar = useInvalidarDocumentos(codigo)
  return useMutation({
    mutationFn: ({ documento, archivo }: { documento: string; archivo: File }) =>
      reemplazarDocumentoApi(codigo, documento, archivo),
    onSuccess: invalidar,
  })
}

export function useEliminarDocumento(codigo: string) {
  const invalidar = useInvalidarDocumentos(codigo)
  return useMutation({
    mutationFn: (documento: string) => eliminarDocumentoApi(codigo, documento),
    onSuccess: invalidar,
    // Si el documento ya no existe o la postulación cambió de estado, la lista se actualiza igual.
    onError: invalidar,
  })
}

/** Pide la dirección temporal de un archivo; quien llama la abre en otra pestaña. */
export function useAccesoDocumento(codigo: string) {
  return useMutation({ mutationFn: (documento: string) => accesoDocumentoApi(codigo, documento) })
}
