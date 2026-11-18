import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { FinalistasParams, NotificarFinalistasRequest, RegistrarEntregaRequest } from '../types'
import { listarFinalistasApi, notificarFinalistasApi, registrarEntregaApi } from './finalistasApi'
import { finalistasKeys } from './keys'

// El push y el correo se despachan en segundo plano y se reintentan (2 s y 8 s de espera por defecto, unos
// 15 s en total): tras notificar, el aviso está «Enviando…» y recién después se sabe si quedó «Enviada» o
// «Fallida». Mientras alguna fila esté «Enviando…» el listado se vuelve a consultar cada pocos segundos.
const INTERVALO_ENVIANDO_MS = 3000

/** Una página de finalistas de la convocatoria; conserva la anterior mientras llega la siguiente. */
export function useFinalistas(params: FinalistasParams | undefined) {
  return useQuery({
    queryKey: finalistasKeys.list(params as FinalistasParams),
    queryFn: () => listarFinalistasApi(params as FinalistasParams),
    enabled: Boolean(params),
    placeholderData: keepPreviousData,
    refetchInterval: (consulta) =>
      consulta.state.data?.data.some((f) => f.estadoNotificacion === 'ENVIANDO') ? INTERVALO_ENVIANDO_MS : false,
  })
}

export function useNotificarFinalistas() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (pedido: NotificarFinalistasRequest) => notificarFinalistasApi(pedido),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: finalistasKeys.lists() })
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
