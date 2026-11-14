import { useQuery } from '@tanstack/react-query'

import { perfilKeys } from './keys'
import { obtenerPerfilApi } from './perfilApi'

/** Flujo C del CU15: el perfil del usuario autenticado. */
export function usePerfil() {
  return useQuery({
    queryKey: perfilKeys.detail(),
    queryFn: obtenerPerfilApi,
  })
}
