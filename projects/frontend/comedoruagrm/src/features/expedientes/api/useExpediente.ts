import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { finalistasKeys } from '@/features/finalistas'

import type { DevolverPostulacionRequest, RegistrarAuditoriaRequest } from '../types'
import {
  accesoDocumentoApi,
  devolverPostulacionApi,
  obtenerExpedienteApi,
  registrarAuditoriaApi,
} from './expedientesApi'
import { expedientesKeys } from './keys'

/** El expediente del finalista; no consulta mientras no haya código. */
export function useExpediente(codigo: string | undefined) {
  return useQuery({
    queryKey: expedientesKeys.detail(codigo as string),
    queryFn: () => obtenerExpedienteApi(codigo as string),
    enabled: Boolean(codigo),
  })
}

/** Registra una auditoría y refresca el expediente y la lista de finalistas (columna «Auditoría»). */
export function useRegistrarAuditoria() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      codigo,
      pedido,
      claveIdempotencia,
    }: {
      codigo: string
      pedido: RegistrarAuditoriaRequest
      claveIdempotencia?: string
    }) => registrarAuditoriaApi(codigo, pedido, claveIdempotencia),
    onSuccess: (_resultado, { codigo }) => {
      void queryClient.invalidateQueries({ queryKey: expedientesKeys.detail(codigo) })
      void queryClient.invalidateQueries({ queryKey: finalistasKeys.lists() })
    },
  })
}

/** Pide la URL temporal de un documento; es una mutación porque cada apertura genera una URL nueva. */
export function useAccesoDocumento() {
  return useMutation({
    mutationFn: ({ finalista, documento }: { finalista: string; documento: string }) =>
      accesoDocumentoApi(finalista, documento),
  })
}

/**
 * Devuelve la postulación para corrección (CU19) y refresca el expediente y la lista de finalistas
 * (columna «Auditoría»). No lleva clave de idempotencia: el botón se deshabilita mientras se envía y una
 * segunda petición recibe `ERR_DEV_01`.
 */
export function useDevolverPostulacion() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ codigo, pedido }: { codigo: string; pedido: DevolverPostulacionRequest }) =>
      devolverPostulacionApi(codigo, pedido),
    onSuccess: (_resultado, { codigo }) => {
      void queryClient.invalidateQueries({ queryKey: expedientesKeys.detail(codigo) })
      void queryClient.invalidateQueries({ queryKey: finalistasKeys.lists() })
    },
  })
}
