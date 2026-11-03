import 'package:dio/dio.dart';

import '../../core/network/api_client.dart';

/// Campo del formulario de registro en conflicto con un dato ya existente.
enum CampoAuth { ci, correo, registroUniversitario, carrera }

/// Error ya traducido a lo que ve la persona.
class AuthError {
  final String mensaje;

  /// Campo en conflicto (CI, correo, registro universitario, carrera), si aplica.
  final CampoAuth? campo;

  /// Error de conexión: la pantalla ofrece reintentar (A9).
  final bool red;

  const AuthError(this.mensaje, {this.campo, this.red = false});
}

const _generico = 'Ocurrió un error inesperado. Inténtalo de nuevo.';

/// Traduce un error de la API a un [AuthError] (CU01: A1, A3, A4, A6, A9). Las
/// reglas de contraseña, el bloqueo y el mensaje único de credenciales los
/// aplica el backend; acá solo se muestran. Nunca incluye datos sensibles.
AuthError mapearError(Object error) {
  if (error is DioException) {
    final interno = error.error;
    if (interno is ApiException) return _deApi(interno);
    if (error.response == null) {
      return const AuthError(
        'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.',
        red: true,
      );
    }
  }
  return const AuthError(_generico);
}

AuthError _deApi(ApiException e) {
  switch (e.code) {
    case 'ERR_AUTH_04':
      return const AuthError('Correo o contraseña incorrectos.');
    case 'ERR_AUTH_05': // el backend ya dice cuántos minutos faltan
      return AuthError(e.message);
    case 'ERR_AUTH_03':
      return const AuthError('Tu cuenta está desactivada. Comunícate con la DUBSS.');
    case 'ERR_US_01':
      return AuthError(e.message, campo: CampoAuth.ci);
    case 'ERR_US_02':
      return AuthError(e.message, campo: CampoAuth.correo);
    case 'ERR_US_03':
      return AuthError(e.message, campo: CampoAuth.registroUniversitario);
    case 'ERR_US_04':
      return AuthError(e.message, campo: CampoAuth.carrera);
    case 'ERR_SYS_01':
      return AuthError(e.message);
    case 'ERR_SYS_02':
      return const AuthError('Demasiados intentos en poco tiempo. Espera un momento e inténtalo de nuevo.');
    default:
      return const AuthError(_generico);
  }
}
