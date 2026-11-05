import { useMutation, useQueryClient } from '@tanstack/react-query'

import type { RestablecerContrasenaRequest } from '../schemas'
import { restablecerContrasenaApi } from './authApi'

/**
 * Flujo B del CU15: define la contraseña nueva con el token del enlace. El backend cierra todas las
 * sesiones de la cuenta, así que al terminar bien se limpia la caché (no queda nada de ninguna sesión).
 * `gcTime: 0`: la mutación no deja el token ni las contraseñas en la caché de TanStack Query.
 */
export function useRestablecerContrasena() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: RestablecerContrasenaRequest) => restablecerContrasenaApi(request),
    gcTime: 0,
    onSuccess: () => {
      queryClient.clear()
    },
  })
}
