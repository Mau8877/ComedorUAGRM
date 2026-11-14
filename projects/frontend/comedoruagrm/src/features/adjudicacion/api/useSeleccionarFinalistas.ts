import { useMutation, useQueryClient } from '@tanstack/react-query'

import { finalistasKeys } from '@/features/finalistas'

import type { SeleccionarFinalistasRequest } from '../types'
import { seleccionarFinalistasApi } from './adjudicacionApi'
import { adjudicacionKeys } from './keys'

/** Selecciona finalistas (CU05) y refresca el panel de la convocatoria y la lista de finalistas. */
export function useSeleccionarFinalistas() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ convocatoria, pedido }: { convocatoria: string; pedido: SeleccionarFinalistasRequest }) =>
      seleccionarFinalistasApi(convocatoria, pedido),
    onSuccess: (_resultado, { convocatoria }) => {
      void queryClient.invalidateQueries({ queryKey: adjudicacionKeys.panel(convocatoria) })
      void queryClient.invalidateQueries({ queryKey: finalistasKeys.lists() })
    },
  })
}
