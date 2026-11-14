import { redirect } from '@tanstack/react-router'

import { esperarSesionInicial } from './api/useSesion'
import { rutaInicioPorRol } from './rutaInicioPorRol'

/** `beforeLoad` de las rutas privadas: sin sesión redirige a /login; con sesión deja el usuario en el contexto de la ruta. */
export async function exigirSesion() {
  const usuario = await esperarSesionInicial()
  if (!usuario) {
    throw redirect({ to: '/login' })
  }
  return { usuario }
}

/**
 * `beforeLoad` de las rutas de un solo rol (ej. la gestión de cuentas): sin sesión
 * redirige a /login (por `exigirSesion`); con otro rol, al módulo de ese rol.
 * Es solo la capa de interfaz: el backend es quien niega con 403.
 */
export function exigirRol(rol: string) {
  return async () => {
    const { usuario } = await exigirSesion()
    if (usuario.rol !== rol) {
      throw redirect({ to: rutaInicioPorRol(usuario.rol) })
    }
    return { usuario }
  }
}

/** `beforeLoad` de /login y /registro: con sesión activa no tiene sentido verlas, se lleva al módulo del rol. */
export async function redirigirSiHaySesion() {
  const usuario = await esperarSesionInicial()
  if (usuario) {
    throw redirect({ to: rutaInicioPorRol(usuario.rol) })
  }
}
