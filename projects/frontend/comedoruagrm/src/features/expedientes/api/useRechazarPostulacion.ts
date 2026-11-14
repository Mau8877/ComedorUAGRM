import { useMutation, useQueryClient } from '@tanstack/react-query'

import { finalistasKeys } from '@/features/finalistas'

import type { CerrarPostulacionRequest } from '../types'
import { rechazarPostulacionApi } from './expedientesApi'
import { expedientesKeys } from './keys'

/**
 * Rechaza la postulación (CU20) y refresca el expediente y la lista de finalistas (insignia de estado).
 * No lleva clave de idempotencia: el botón se deshabilita mientras se envía y una segunda petición
 * recibe `ERR_ARC_01`.
 */
export function useRechazarPostulacion() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ codigo, pedido }: { codigo: string; pedido: CerrarPostulacionRequest }) =>
      rechazarPostulacionApi(codigo, pedido),
    onSuccess: (_resultado, { codigo }) => {
      void queryClient.invalidateQueries({ queryKey: expedientesKeys.detail(codigo) })
      void queryClient.invalidateQueries({ queryKey: finalistasKeys.lists() })
    },
  })
}
