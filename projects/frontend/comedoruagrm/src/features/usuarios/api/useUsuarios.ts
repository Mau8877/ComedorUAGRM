import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { EditarUsuarioPersonalRequest } from '../schemas'
import type { UsuariosListParams } from '../types'
import { usuariosKeys } from './keys'
import {
  cambiarRolApi,
  crearUsuarioApi,
  desactivarUsuarioApi,
  editarUsuarioApi,
  listarUsuariosApi,
  reactivarUsuarioApi,
} from './usuariosApi'
import type { CrearUsuarioPersonalRequest } from '../schemas'

/** Lista paginada de cuentas del personal; conserva la página anterior mientras llega la siguiente. */
export function useUsuarios(params: UsuariosListParams) {
  return useQuery({
    queryKey: usuariosKeys.list(params),
    queryFn: () => listarUsuariosApi(params),
    placeholderData: keepPreviousData,
  })
}

/** Cada mutación invalida todos los listados al terminar bien (cualquier rol, filtro o página). */
function useInvalidarListas() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: usuariosKeys.lists() })
}

/** Para refrescar la lista cuando el backend dice que la cuenta ya no existe o ya estaba en ese estado (A8, A9). */
export const useRefrescarListas = useInvalidarListas

/**
 * `gcTime: 0`: el resultado trae la contraseña temporal, que no debe quedar en la
 * caché de TanStack Query. Vive solo en el estado del componente que muestra el resultado.
 */
export function useCrearUsuario() {
  const invalidar = useInvalidarListas()
  return useMutation({
    mutationFn: (request: CrearUsuarioPersonalRequest) => crearUsuarioApi(request),
    gcTime: 0,
    onSuccess: invalidar,
  })
}

export function useEditarUsuario() {
  const invalidar = useInvalidarListas()
  return useMutation({
    mutationFn: ({ codigo, cambios }: { codigo: string; cambios: Partial<EditarUsuarioPersonalRequest> }) =>
      editarUsuarioApi(codigo, cambios),
    onSuccess: invalidar,
  })
}

export function useCambiarRol() {
  const invalidar = useInvalidarListas()
  return useMutation({
    mutationFn: ({ codigo, rol }: { codigo: string; rol: string }) => cambiarRolApi(codigo, rol),
    onSuccess: invalidar,
  })
}

export function useDesactivarUsuario() {
  const invalidar = useInvalidarListas()
  return useMutation({
    mutationFn: (codigo: string) => desactivarUsuarioApi(codigo),
    onSuccess: invalidar,
  })
}

export function useReactivarUsuario() {
  const invalidar = useInvalidarListas()
  return useMutation({
    mutationFn: (codigo: string) => reactivarUsuarioApi(codigo),
    onSuccess: invalidar,
  })
}
