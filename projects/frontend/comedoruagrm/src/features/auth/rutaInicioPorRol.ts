/**
 * Destino de cada rol al entrar. Mientras los CU de postulación, auditoría,
 * dashboard y asistencia no creen sus módulos, todos aterrizan en /panel; este
 * es el único lugar a actualizar cuando eso cambie. Un rol desconocido vuelve al login.
 */
export function rutaInicioPorRol(rol: string): '/panel' | '/login' {
  switch (rol) {
    case 'POSTULANTE':
    case 'ADMINISTRADOR_DUBSS':
    case 'DIRECTIVO':
    case 'PERSONAL_COMEDOR':
      return '/panel'
    default:
      return '/login'
  }
}
