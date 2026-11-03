import { useNavigate } from '@tanstack/react-router'
import { useMutation, useQueryClient } from '@tanstack/react-query'

import { logoutApi } from './authApi'

/**
 * Flujo C del CU01: revoca la sesión de ESTE dispositivo (el backend vence las
 * cookies) y lleva a /login. Si la sesión ya había expirado o falla la red no
 * se muestra error (A10): lo que importa es dejar la pantalla sin sesión.
 */
export function useCerrarSesion() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: async () => {
      try {
        await logoutApi()
      } catch {
        // A10: sesión ya cerrada o sin conexión, se sigue igual.
      }
    },
    onSettled: async () => {
      queryClient.clear()
      await navigate({ to: '/login' })
    },
  })
}
