/// Ruta de inicio de cada rol al entrar. `POSTULANTE` y `ADMINISTRADOR_DUBSS`
/// tienen shell; `DIRECTIVO` y `PERSONAL_COMEDOR` todavía no (los crean los CU
/// de dashboard y de asistencia) y van a `/sin-modulo`. Este es el único lugar a
/// actualizar cuando eso cambie. Un rol desconocido vuelve al login.
String rutaInicioPorRol(String rol) {
  switch (rol) {
    case 'POSTULANTE':
      return '/estudiante';
    case 'ADMINISTRADOR_DUBSS':
      return '/admin';
    case 'DIRECTIVO':
    case 'PERSONAL_COMEDOR':
      return '/sin-modulo';
    default:
      return '/login';
  }
}

/// Nombre legible del rol para mostrar en pantalla (uno desconocido se muestra tal cual).
String etiquetaRol(String rol) {
  switch (rol) {
    case 'POSTULANTE':
      return 'Postulante';
    case 'ADMINISTRADOR_DUBSS':
      return 'Administrador DUBSS';
    case 'DIRECTIVO':
      return 'Directivo';
    case 'PERSONAL_COMEDOR':
      return 'Personal del comedor';
    default:
      return rol;
  }
}
