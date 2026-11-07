import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { FinalistasParams, NotificarFinalistasRequest, RegistrarEntregaRequest } from '../types'
import { listarFinalistasApi, notificarFinalistasApi, registrarEntregaApi } from './finalistasApi'
import { finalistasKeys } from './keys'

// El push y el correo se despachan en segundo plano: tras notificar, el estado «Enviada» o «Fallida» del
// aviso recién se conoce un momento después. Se vuelve a consultar el listado a los pocos segundos.
const RETARDO_REFRESCO_MS = 4000

/** Una página de finalistas de la convocatoria; conserva la anterior mientras llega la siguiente. */
export function useFinalistas(params: FinalistasParams | undefined) {
  return useQuery({
    queryKey: finalistasKeys.list(params as FinalistasParams),
    queryFn: () => listarFinalistasApi(params as FinalistasParams),
    enabled: Boolean(params),
    placeholderData: keepPreviousData,
  })
}

export function useNotificarFinalistas() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (pedido: NotificarFinalistasRequest) => notificarFinalistasApi(pedido),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: finalistasKeys.lists() })
      window.setTimeout(() => {
        void queryClient.invalidateQueries({ queryKey: finalistasKeys.lists() })
      }, RETARDO_REFRESCO_MS)
    },
  })
}

export function useRegistrarEntrega() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ codigo, pedido }: { codigo: string; pedido: RegistrarEntregaRequest }) =>
      registrarEntregaApi(codigo, pedido),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: finalistasKeys.lists() })
    },
  })
}
