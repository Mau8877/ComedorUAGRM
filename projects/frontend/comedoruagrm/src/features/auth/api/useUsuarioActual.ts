import { useQuery } from '@tanstack/react-query'

import { queryClient } from '@/store'

import { usuarioActualApi } from './authApi'
import { authKeys } from './keys'
import type { Usuario } from '../types'

const VIGENCIA_MS = 60 * 1000

/** Usuario de la sesión actual, o `null` si no hay sesión. `retry: false`: un 401 no se reintenta. */
export function useUsuarioActual({ enabled = true }: { enabled?: boolean } = {}) {
  return useQuery({
    queryKey: authKeys.usuarioActual,
    queryFn: usuarioActualApi,
    enabled,
    retry: false,
    staleTime: VIGENCIA_MS,
  })
}

/** Para los guards del router (fuera de React): pregunta al backend, que es el único que puede leer las cookies HttpOnly. */
export function cargarUsuarioActual(): Promise<Usuario | null> {
  return queryClient.fetchQuery({
    queryKey: authKeys.usuarioActual,
    queryFn: usuarioActualApi,
    retry: false,
    staleTime: VIGENCIA_MS,
  })
}
