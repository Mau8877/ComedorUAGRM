import { useQuery } from '@tanstack/react-query'

import { listarCarrerasApi } from './authApi'
import { carrerasKeys } from './keys'

/** Catálogo para el selector del registro (público: se pide antes de tener cuenta). */
export function useCarreras() {
  return useQuery({
    queryKey: carrerasKeys.list({ pageSize: 200 }),
    queryFn: listarCarrerasApi,
    staleTime: 10 * 60 * 1000,
  })
}
