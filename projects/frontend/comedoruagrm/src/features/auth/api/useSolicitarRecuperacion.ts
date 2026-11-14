import { useMutation } from '@tanstack/react-query'

import { solicitarRecuperacionApi } from './authApi'

/**
 * Flujo A del CU15: pide el enlace de recuperación. No devuelve nada que distinga los casos
 * (el backend responde siempre igual); quien lo usa muestra el mensaje neutro al terminar bien.
 * `gcTime: 0`: la mutación no deja el correo guardado en la caché de TanStack Query.
 */
export function useSolicitarRecuperacion() {
  return useMutation({
    mutationFn: (correo: string) => solicitarRecuperacionApi(correo.trim()),
    gcTime: 0,
  })
}
