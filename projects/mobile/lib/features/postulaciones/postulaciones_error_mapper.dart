import 'package:dio/dio.dart';

import '../../core/network/api_client.dart';

/// Error de «Mis postulaciones» ya traducido a lo que ve la persona.
class ErrorPostulaciones {
  final String mensaje;

  /// La postulación no existe o no es del usuario (`ERR_POS_01`): la pantalla dice «no encontrada».
  final bool noEncontrada;

  /// Error de conexión o del servidor: la pantalla ofrece reintentar (A10).
  final bool red;

  const ErrorPostulaciones(this.mensaje, {this.noEncontrada = false, this.red = false});
}

const _generico = 'Ocurrió un error inesperado. Inténtalo de nuevo.';
const _servidor = 'Ocurrió un error en el servidor. Inténtalo de nuevo en unos minutos.';

/// Traduce un error de la API de «Mis postulaciones» (CU04). Nunca incluye datos sensibles.
ErrorPostulaciones mapearErrorPostulaciones(Object error) {
  if (error is DioException) {
    final interno = error.error;
    if (interno is ApiException) return _deApi(interno);
    if (error.response == null) {
      return const ErrorPostulaciones(
        'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.',
        red: true,
      );
    }
    if ((error.response?.statusCode ?? 0) >= 500) return const ErrorPostulaciones(_servidor, red: true);
  }
  return const ErrorPostulaciones(_generico);
}

ErrorPostulaciones _deApi(ApiException e) {
  switch (e.code) {
    case 'ERR_POS_01':
      return const ErrorPostulaciones('La postulación no existe o no es tuya', noEncontrada: true);
    case 'ERR_AUTH_07':
      return const ErrorPostulaciones('No tienes permiso para esta acción.');
    case 'ERR_SYS_00':
      return const ErrorPostulaciones(_servidor, red: true);
    case 'ERR_SYS_05':
      return ErrorPostulaciones(e.message);
    default:
      return const ErrorPostulaciones(_generico);
  }
}
