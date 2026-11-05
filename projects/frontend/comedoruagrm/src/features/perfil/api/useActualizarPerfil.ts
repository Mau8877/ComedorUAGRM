import { useMutation, useQueryClient } from '@tanstack/react-query'

import { authKeys } from '@/features/auth'

import type { ActualizarPerfilRequest } from '../schemas'
import { perfilKeys } from './keys'
import { actualizarPerfilApi } from './perfilApi'

/**
 * Flujo D del CU15: actualiza correo y/o teléfono. Al terminar bien deja el perfil nuevo en la caché
 * e invalida el usuario actual (el correo también vive ahí).
 * `gcTime: 0`: la mutación no deja la contraseña actual guardada en la caché de TanStack Query.
 */
export function useActualizarPerfil() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: ActualizarPerfilRequest) => actualizarPerfilApi(request),
    gcTime: 0,
    onSuccess: (perfil) => {
      queryClient.setQueryData(perfilKeys.detail(), perfil)
      void queryClient.invalidateQueries({ queryKey: perfilKeys.detail() })
      void queryClient.invalidateQueries({ queryKey: authKeys.usuarioActual })
    },
  })
}
