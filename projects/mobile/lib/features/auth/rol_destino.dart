/// Roles que pueden usar la app móvil: el postulante (su módulo) y el personal
/// del comedor (registrar asistencia, CU10).
const rolPostulante = 'POSTULANTE';
const rolPersonalComedor = 'PERSONAL_COMEDOR';

/// Ruta de inicio al entrar. `POSTULANTE` va a su módulo y `PERSONAL_COMEDOR` a
/// registrar asistencia; cualquier otro rol (o uno desconocido) va a
/// `/solo-postulantes`, que le indica usar la plataforma web. Este es el único
/// lugar a actualizar si otro rol llega a tener módulo en mobile.
String rutaInicioPorRol(String rol) {
  switch (rol) {
    case rolPostulante:
      return '/estudiante';
    case rolPersonalComedor:
      return '/asistencia';
    default:
      return '/solo-postulantes';
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
