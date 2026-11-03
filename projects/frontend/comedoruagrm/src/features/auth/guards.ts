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

/** `beforeLoad` de /login y /registro: con sesión activa no tiene sentido verlas, se lleva al módulo del rol. */
export async function redirigirSiHaySesion() {
  const usuario = await esperarSesionInicial()
  if (usuario) {
    throw redirect({ to: rutaInicioPorRol(usuario.rol) })
  }
}
