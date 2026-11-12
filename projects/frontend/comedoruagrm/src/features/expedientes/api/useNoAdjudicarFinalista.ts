import { useMutation, useQueryClient } from '@tanstack/react-query'

import { adjudicacionKeys } from '@/features/adjudicacion'
import { finalistasKeys } from '@/features/finalistas'

import type { NoAdjudicarRequest } from '../types'
import { noAdjudicarFinalistaApi } from './expedientesApi'
import { expedientesKeys } from './keys'

/** «No adjudicar» (CU05) y refresca el expediente, la lista de finalistas y el panel de cupos. Sin idempotencia. */
export function useNoAdjudicarFinalista() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ codigo, pedido }: { codigo: string; pedido: NoAdjudicarRequest }) =>
      noAdjudicarFinalistaApi(codigo, pedido),
    onSuccess: (_resultado, { codigo }) => {
      void queryClient.invalidateQueries({ queryKey: expedientesKeys.detail(codigo) })
      void queryClient.invalidateQueries({ queryKey: finalistasKeys.lists() })
      void queryClient.invalidateQueries({ queryKey: adjudicacionKeys.panels() })
    },
  })
}
