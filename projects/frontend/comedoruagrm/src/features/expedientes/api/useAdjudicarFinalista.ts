import { useMutation, useQueryClient } from '@tanstack/react-query'

import { adjudicacionKeys } from '@/features/adjudicacion'
import { finalistasKeys } from '@/features/finalistas'

import { adjudicarFinalistaApi } from './expedientesApi'
import { expedientesKeys } from './keys'

/**
 * Adjudica la beca (CU05) y refresca el expediente, la lista de finalistas y el panel de cupos. No lleva clave
 * de idempotencia: el botón se deshabilita mientras se envía y una segunda petición recibe `ERR_ADJ_01`.
 */
export function useAdjudicarFinalista() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ codigo }: { codigo: string }) => adjudicarFinalistaApi(codigo),
    onSuccess: (_resultado, { codigo }) => {
      void queryClient.invalidateQueries({ queryKey: expedientesKeys.detail(codigo) })
      void queryClient.invalidateQueries({ queryKey: finalistasKeys.lists() })
      void queryClient.invalidateQueries({ queryKey: adjudicacionKeys.panels() })
    },
  })
}
