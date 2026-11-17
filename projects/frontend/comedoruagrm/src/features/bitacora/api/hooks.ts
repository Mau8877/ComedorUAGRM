import { keepPreviousData, useQuery } from '@tanstack/react-query'

import type { BitacoraParams } from '../types'
import { listarAccionesApi, listarBitacoraApi } from './bitacoraApi'
import { bitacoraKeys } from './keys'

/** Una página de la bitácora; conserva la anterior mientras llega la siguiente. */
export function useBitacora(params: BitacoraParams) {
  return useQuery({
    queryKey: bitacoraKeys.list(params),
    queryFn: () => listarBitacoraApi(params),
    placeholderData: keepPreviousData,
  })
}

/** Las acciones distintas ya registradas (llenan el filtro de acciones). */
export function useAccionesBitacora() {
  return useQuery({ queryKey: bitacoraKeys.acciones(), queryFn: listarAccionesApi })
}
