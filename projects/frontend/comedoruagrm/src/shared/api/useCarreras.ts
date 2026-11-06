import { useQuery } from '@tanstack/react-query'

import { listarCarrerasApi } from './carrerasApi'
import { sharedKeys } from './keys'

/** Catálogo de carreras (público: el registro lo pide antes de tener cuenta). */
export function useCarreras() {
  return useQuery({
    queryKey: sharedKeys.carreras(),
    queryFn: listarCarrerasApi,
    staleTime: 10 * 60 * 1000,
  })
}
