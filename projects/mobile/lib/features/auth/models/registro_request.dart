/// Cuerpo de `POST /auth/registro`. No lleva rol: el backend crea siempre un Postulante.
class RegistroRequest {
  final String nombres;
  final String apellidos;
  final String ci;
  final String registroUniversitario;
  final String carreraCodigo;
  final String telefono;
  final String correo;
  final String contrasena;
  final String confirmacionContrasena;

  const RegistroRequest({
    required this.nombres,
    required this.apellidos,
    required this.ci,
    required this.registroUniversitario,
    required this.carreraCodigo,
    required this.telefono,
    required this.correo,
    required this.contrasena,
    required this.confirmacionContrasena,
  });

  Map<String, dynamic> toJson() => {
        'nombres': nombres,
        'apellidos': apellidos,
        'ci': ci,
        'registroUniversitario': registroUniversitario,
        'carreraCodigo': carreraCodigo,
        'telefono': telefono,
        'correo': correo,
        'contrasena': contrasena,
        'confirmacionContrasena': confirmacionContrasena,
      };
}
