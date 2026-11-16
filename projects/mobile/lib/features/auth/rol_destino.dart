/// Único rol que puede usar la app móvil.
const rolPostulante = 'POSTULANTE';

/// Ruta de inicio al entrar. La app móvil es solo para `POSTULANTE`: cualquier
/// otro rol (o uno desconocido) va a `/solo-postulantes`, que le indica usar la
/// plataforma web. Este es el único lugar a actualizar si otro rol llega a
/// tener módulo en mobile.
String rutaInicioPorRol(String rol) => rol == rolPostulante ? '/estudiante' : '/solo-postulantes';

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
