import { keepPreviousData, useQuery } from '@tanstack/react-query'

import type { AsistenciasParams } from '../types'
import { listarAsistenciasApi, obtenerMiBecaApi } from './mibecaApi'
import { mibecaKeys } from './keys'

/** La beca del usuario. Sin reintentos: un 404 (sin beca) no se arregla repitiendo la consulta. */
export function useMiBeca() {
  return useQuery({ queryKey: mibecaKeys.detail(), queryFn: obtenerMiBecaApi, retry: false })
}

/** Una página del historial de asistencia; conserva la anterior mientras llega la siguiente. */
export function useAsistenciasBecado(params: AsistenciasParams, enabled = true) {
  return useQuery({
    queryKey: mibecaKeys.list(params),
    queryFn: () => listarAsistenciasApi(params),
    placeholderData: keepPreviousData,
    enabled,
  })
}
