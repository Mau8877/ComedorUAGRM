import '../../auth/models/carrera.dart';

/// Datos que solo tiene el Postulante (`PerfilPostulanteResponse`).
class PerfilPostulante {
  final String registroUniversitario;
  final Carrera carrera;
  final String telefono;

  const PerfilPostulante({
    required this.registroUniversitario,
    required this.carrera,
    required this.telefono,
  });

  factory PerfilPostulante.fromJson(Map<String, dynamic> json) => PerfilPostulante(
        registroUniversitario: json['registroUniversitario'] as String,
        carrera: Carrera.fromJson(json['carrera'] as Map<String, dynamic>),
        telefono: json['telefono'] as String,
      );
}

/// Perfil del usuario autenticado (`PerfilResponse`, CU15). Nunca trae la contraseña ni sus hashes.
class Perfil {
  final String codigo;
  final String nombres;
  final String apellidos;
  final String ci;
  final String correo;

  /// Nombre del rol tal cual lo manda el backend (`POSTULANTE`, `ADMINISTRADOR_DUBSS`...).
  final String rol;
  final bool activo;

  /// `null` si la cuenta no es de un Postulante.
  final PerfilPostulante? postulante;

  const Perfil({
    required this.codigo,
    required this.nombres,
    required this.apellidos,
    required this.ci,
    required this.correo,
    required this.rol,
    required this.activo,
    this.postulante,
  });

  factory Perfil.fromJson(Map<String, dynamic> json) {
    final postulante = json['postulante'];
    return Perfil(
      codigo: json['codigo'] as String,
      nombres: json['nombres'] as String,
      apellidos: json['apellidos'] as String,
      ci: json['ci'] as String,
      correo: json['correo'] as String,
      rol: json['rol'] as String,
      activo: json['activo'] as bool,
      postulante: postulante == null ? null : PerfilPostulante.fromJson(postulante as Map<String, dynamic>),
    );
  }

  bool get esPostulante => postulante != null;
}
