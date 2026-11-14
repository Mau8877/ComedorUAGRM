import { useMutation } from '@tanstack/react-query'

import type { CambiarContrasenaRequest } from '../schemas'
import { cambiarContrasenaApi } from './perfilApi'

/**
 * Flujo E del CU15: cambia la contraseña desde el perfil. NO toca la sesión local: la actual sigue abierta
 * (el backend solo cierra las de los demás dispositivos).
 * `gcTime: 0`: la mutación no deja las contraseñas guardadas en la caché de TanStack Query.
 */
export function useCambiarContrasena() {
  return useMutation({
    mutationFn: (request: CambiarContrasenaRequest) => cambiarContrasenaApi(request),
    gcTime: 0,
  })
}
