/// Usuario autenticado (`UsuarioResponse` del backend).
class Usuario {
  final String codigo;
  final String nombres;
  final String apellidos;
  final String correo;

  /// Nombre del rol tal cual lo manda el backend (`POSTULANTE`, `ADMINISTRADOR_DUBSS`...).
  final String rol;

  const Usuario({
    required this.codigo,
    required this.nombres,
    required this.apellidos,
    required this.correo,
    required this.rol,
  });

  factory Usuario.fromJson(Map<String, dynamic> json) => Usuario(
        codigo: json['codigo'] as String,
        nombres: json['nombres'] as String,
        apellidos: json['apellidos'] as String,
        correo: json['correo'] as String,
        rol: json['rol'] as String,
      );

  String get nombreCompleto => '$nombres $apellidos';
}
