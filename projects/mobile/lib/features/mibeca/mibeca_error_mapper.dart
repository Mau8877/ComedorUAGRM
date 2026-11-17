import 'package:dio/dio.dart';

import '../../core/network/api_client.dart';

/// Error de «Mi beca» (CU24) ya traducido a lo que ve el becado.
class ErrorMiBeca {
  final String mensaje;

  /// El usuario no tiene beca (`ERR_MBE_01`, A1): la pantalla lo informa y ofrece ir a «Mis postulaciones».
  final bool sinBeca;

  /// Error de conexión o del servidor: la pantalla ofrece reintentar (A6).
  final bool red;

  const ErrorMiBeca(this.mensaje, {this.sinBeca = false, this.red = false});
}

const _generico = 'Ocurrió un error inesperado. Inténtalo de nuevo.';
const _servidor = 'Ocurrió un error en el servidor. Inténtalo de nuevo en unos minutos.';

/// Traduce un error de la API de «Mi beca». Nunca incluye datos sensibles.
ErrorMiBeca mapearErrorMiBeca(Object error) {
  if (error is DioException) {
    final interno = error.error;
    if (interno is ApiException) return _deApi(interno);
    if (error.response == null) {
      return const ErrorMiBeca('No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.',
          red: true);
    }
    if ((error.response?.statusCode ?? 0) >= 500) return const ErrorMiBeca(_servidor, red: true);
  }
  return const ErrorMiBeca(_generico);
}

ErrorMiBeca _deApi(ApiException e) {
  switch (e.code) {
    case 'ERR_MBE_01':
      return const ErrorMiBeca('No cuentas con una beca: no figuras en el padrón de becados.', sinBeca: true);
    case 'ERR_MBE_02':
      return const ErrorMiBeca('La fecha de inicio no puede ser posterior a la fecha final.');
    case 'ERR_AUTH_07':
      return const ErrorMiBeca('No tienes permiso para ver esta información.');
    case 'ERR_SYS_00':
      return const ErrorMiBeca(_servidor, red: true);
    default:
      return const ErrorMiBeca(_generico);
  }
}
