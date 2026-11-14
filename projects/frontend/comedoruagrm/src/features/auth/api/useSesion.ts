import { cargarUsuarioActual, useUsuarioActual } from './useUsuarioActual'

export type EstadoSesion = 'cargando' | 'sin-sesion' | 'con-sesion'

/** Estado de sesión para componentes: sale de GET /auth/usuario-actual (las cookies HttpOnly no se leen desde JS). */
export function useSesion() {
  const consulta = useUsuarioActual()
  const estado: EstadoSesion = consulta.isPending ? 'cargando' : consulta.data ? 'con-sesion' : 'sin-sesion'
  return { estado, usuario: consulta.data ?? null }
}

/** Para el guard del router: espera a saber si hay sesión antes de decidir a dónde ir. */
export const esperarSesionInicial = cargarUsuarioActual
