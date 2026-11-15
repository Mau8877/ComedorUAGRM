import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { ConvocatoriaRequest, GestionRequest } from '../schemas'
import type { ConvocatoriasListParams, GestionesListParams, TiposDocumentoListParams } from '../types'
import {
  abrirConvocatoriaApi,
  cerrarConvocatoriaApi,
  crearConvocatoriaApi,
  crearGestionApi,
  editarConvocatoriaApi,
  editarGestionApi,
  eliminarConvocatoriaApi,
  listarConvocatoriasApi,
  listarGestionesApi,
  listarTiposDocumentoApi,
  obtenerConvocatoriaApi,
} from './convocatoriasApi'
import { convocatoriasKeys, gestionesKeys, tiposDocumentoKeys } from './keys'

/** Lista paginada de convocatorias; conserva la página anterior mientras llega la siguiente. */
export function useConvocatorias(params: ConvocatoriasListParams) {
  return useQuery({
    queryKey: convocatoriasKeys.list(params),
    queryFn: () => listarConvocatoriasApi(params),
    placeholderData: keepPreviousData,
  })
}

/** El detalle de una convocatoria (documentos y si tiene postulaciones); sin código no consulta. */
export function useConvocatoria(codigo: string | undefined) {
  return useQuery({
    queryKey: convocatoriasKeys.detail(codigo ?? ''),
    queryFn: () => obtenerConvocatoriaApi(codigo as string),
    enabled: Boolean(codigo),
  })
}

export function useGestiones(params: GestionesListParams) {
  return useQuery({
    queryKey: gestionesKeys.list(params),
    queryFn: () => listarGestionesApi(params),
    placeholderData: keepPreviousData,
  })
}

export function useTiposDocumento(params: TiposDocumentoListParams) {
  return useQuery({
    queryKey: tiposDocumentoKeys.list(params),
    queryFn: () => listarTiposDocumentoApi(params),
    placeholderData: keepPreviousData,
  })
}

/** Cada mutación de convocatoria invalida los listados y el detalle cacheado de esa convocatoria. */
function useInvalidarConvocatorias() {
  const queryClient = useQueryClient()
  return (codigo?: string) => {
    void queryClient.invalidateQueries({ queryKey: convocatoriasKeys.lists() })
    if (codigo) void queryClient.invalidateQueries({ queryKey: convocatoriasKeys.detail(codigo) })
  }
}

/** Para refrescar la lista cuando el backend dice que la convocatoria ya no existe o cambió de estado (A8, A9). */
export const useRefrescarConvocatorias = useInvalidarConvocatorias

export function useCrearConvocatoria() {
  const invalidar = useInvalidarConvocatorias()
  return useMutation({
    mutationFn: (request: ConvocatoriaRequest) => crearConvocatoriaApi(request),
    onSuccess: () => invalidar(),
  })
}

export function useEditarConvocatoria() {
  const invalidar = useInvalidarConvocatorias()
  return useMutation({
    mutationFn: ({ codigo, request }: { codigo: string; request: ConvocatoriaRequest }) =>
      editarConvocatoriaApi(codigo, request),
    onSuccess: (_, { codigo }) => invalidar(codigo),
  })
}

export function useAbrirConvocatoria() {
  const invalidar = useInvalidarConvocatorias()
  return useMutation({
    mutationFn: (codigo: string) => abrirConvocatoriaApi(codigo),
    onSuccess: (_, codigo) => invalidar(codigo),
  })
}

export function useCerrarConvocatoria() {
  const invalidar = useInvalidarConvocatorias()
  return useMutation({
    mutationFn: (codigo: string) => cerrarConvocatoriaApi(codigo),
    onSuccess: (_, codigo) => invalidar(codigo),
  })
}

export function useEliminarConvocatoria() {
  const invalidar = useInvalidarConvocatorias()
  return useMutation({
    mutationFn: (codigo: string) => eliminarConvocatoriaApi(codigo),
    onSuccess: (_, codigo) => invalidar(codigo),
  })
}

/** Tras crear o editar una gestión se refrescan todas las listas de gestiones (el selector del formulario incluido). */
function useInvalidarGestiones() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: gestionesKeys.all })
}

/** Para refrescar la lista cuando el backend dice que la gestión ya no existe (A8). */
export const useRefrescarGestiones = useInvalidarGestiones

export function useCrearGestion() {
  const invalidar = useInvalidarGestiones()
  return useMutation({
    mutationFn: (request: GestionRequest) => crearGestionApi(request),
    onSuccess: invalidar,
  })
}

export function useEditarGestion() {
  const invalidar = useInvalidarGestiones()
  const invalidarConvocatorias = useInvalidarConvocatorias()
  return useMutation({
    mutationFn: ({ codigo, request }: { codigo: string; request: GestionRequest }) => editarGestionApi(codigo, request),
    // La etiqueta de la gestión se ve en cada fila de convocatorias: también se refrescan.
    onSuccess: () => {
      void invalidar()
      invalidarConvocatorias()
    },
  })
}
