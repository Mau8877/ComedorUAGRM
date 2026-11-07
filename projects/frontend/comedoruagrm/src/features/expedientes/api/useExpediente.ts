import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { finalistasKeys } from '@/features/finalistas'

import type { RegistrarAuditoriaRequest } from '../types'
import { accesoDocumentoApi, obtenerExpedienteApi, registrarAuditoriaApi } from './expedientesApi'
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
