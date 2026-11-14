import 'usuario.dart';

/// Sesión abierta (`SesionResponse`): el token de acceso para el header
/// `Authorization: Bearer`, el refresh token para renovarlo y el usuario.
class Sesion {
  final String accessToken;
  final String refreshToken;
  final String tokenType;

  /// Segundos de vida del token de acceso.
  final int expiresIn;
  final Usuario usuario;

  const Sesion({
    required this.accessToken,
    required this.refreshToken,
    required this.tokenType,
    required this.expiresIn,
    required this.usuario,
  });

  factory Sesion.fromJson(Map<String, dynamic> json) => Sesion(
        accessToken: json['accessToken'] as String,
        refreshToken: json['refreshToken'] as String,
        tokenType: json['tokenType'] as String,
        expiresIn: (json['expiresIn'] as num).toInt(),
        usuario: Usuario.fromJson(json['usuario'] as Map<String, dynamic>),
      );
}
