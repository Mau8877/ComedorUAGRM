import { useMutation, useQueryClient } from '@tanstack/react-query'

import { aRegistroRequest, type RegistroFormValues } from '../schemas'
import { registrarApi } from './authApi'
import { authKeys } from './keys'

/**
 * Flujo A del CU01: el backend crea la cuenta y deja la sesión abierta con las
 * cookies, así que acá solo se guarda el usuario devuelto. `isPending` evita
 * el doble envío (A7). No toca ningún almacenamiento del navegador.
 */
export function useRegistro() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (valores: RegistroFormValues) => registrarApi(aRegistroRequest(valores)),
    onSuccess: ({ sesion }) => {
      // Se limpia la caché por si quedaban datos de otra persona de una sesión anterior.
      queryClient.clear()
      queryClient.setQueryData(authKeys.usuarioActual, sesion.usuario)
    },
  })
}
