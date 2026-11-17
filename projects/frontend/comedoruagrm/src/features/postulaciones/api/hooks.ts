import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { EnviarPostulacionRequest, GuardarBorradorRequest, MisPostulacionesParams } from '../types'
import {
  accesoDocumentoApi,
  adjuntarDocumentoApi,
  eliminarDocumentoApi,
  enviarPostulacionApi,
  guardarBorradorApi,
  iniciarPostulacionApi,
  listarDocumentosApi,
  listarMisPostulacionesApi,
  miBorradorApi,
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

// ---- Enviar la postulación (CU-02) ----

/** «Nueva postulación»: abre (o recupera) la postulación en edición de la convocatoria abierta. */
export function useIniciarPostulacion() {
  return useMutation({ mutationFn: iniciarPostulacionApi })
}

/**
 * Envía el formulario. Al terminar, «Mis postulaciones» y el detalle quedan desactualizados (aparece una postulación
 * nueva o cambia su estado), así que se invalidan; la lista de documentos ya no se edita.
 */
export function useEnviarPostulacion(codigo: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (respuestas: EnviarPostulacionRequest) => enviarPostulacionApi(codigo, respuestas),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: postulacionesKeys.all }),
  })
}

// ---- Borrador de la postulación (CU-17) ----

/** Guarda el avance del formulario. Deja desactualizado «Mi borrador» (cambia lo que falta y la hora del guardado). */
export function useGuardarBorrador(codigo: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (borrador: GuardarBorradorRequest) => guardarBorradorApi(codigo, borrador),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: postulacionesKeys.miBorrador() }),
  })
}

/** «Mi borrador»: el borrador de la convocatoria abierta (`null` si no hay). */
export function useMiBorrador() {
  return useQuery({ queryKey: postulacionesKeys.miBorrador(), queryFn: miBorradorApi })
}
