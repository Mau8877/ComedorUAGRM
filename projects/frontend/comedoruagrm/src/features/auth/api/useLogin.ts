import { useMutation, useQueryClient } from '@tanstack/react-query'

import type { LoginFormValues } from '../schemas'
import { loginApi } from './authApi'
import { authKeys } from './keys'

/** Flujo B del CU01: el backend deja las cookies puestas; acá se guarda el usuario para decidir el destino. */
export function useLogin() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (valores: LoginFormValues) => loginApi(valores.correo.trim(), valores.contrasena),
    onSuccess: (sesion) => {
      queryClient.clear()
      queryClient.setQueryData(authKeys.usuarioActual, sesion.usuario)
    },
  })
}
