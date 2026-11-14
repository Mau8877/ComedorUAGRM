import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { PublicarComunicadoRequest } from '../types'
import { listarComunicadosApi, publicarComunicadoApi } from './comunicadosApi'
import { comunicadosKeys } from './keys'

/** Los comunicados ya publicados, el más reciente primero. */
export function useComunicados(params: { page: number; pageSize: number }) {
  return useQuery({
    queryKey: comunicadosKeys.list(params),
    queryFn: () => listarComunicadosApi(params),
    placeholderData: keepPreviousData,
  })
}

export function usePublicarComunicado() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (pedido: PublicarComunicadoRequest) => publicarComunicadoApi(pedido),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: comunicadosKeys.lists() })
    },
  })
}
