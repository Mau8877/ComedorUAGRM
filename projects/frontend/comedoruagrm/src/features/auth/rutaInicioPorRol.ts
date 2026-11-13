/**
 * Destino de cada rol al entrar. El Directivo aterriza en su dashboard (CU12); mientras los CU de postulación,
 * auditoría y asistencia no creen sus módulos, los demás roles aterrizan en /panel; este es el único lugar a
 * actualizar cuando eso cambie. Un rol desconocido vuelve al login.
 */
export function rutaInicioPorRol(rol: string): '/panel' | '/dashboard' | '/login' {
  switch (rol) {
    case 'DIRECTIVO':
      return '/dashboard'
    case 'POSTULANTE':
    case 'ADMINISTRADOR_DUBSS':
    case 'PERSONAL_COMEDOR':
      return '/panel'
    default:
      return '/login'
  }
}
