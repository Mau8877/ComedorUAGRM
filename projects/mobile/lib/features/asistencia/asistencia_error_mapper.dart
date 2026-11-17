import 'package:dio/dio.dart';

import '../../core/network/api_client.dart';

/// Error de la asistencia (CU10) ya traducido a lo que ve el Personal del Comedor.
class ErrorAsistencia {
  final String mensaje;

  /// Error de conexión o del servidor: se puede reintentar sin riesgo (registrar es idempotente).
  final bool red;

  const ErrorAsistencia(this.mensaje, {this.red = false});
}

const _generico = 'Ocurrió un error inesperado. Inténtalo de nuevo.';
const _servidor = 'Ocurrió un error en el servidor. Inténtalo de nuevo en unos minutos.';
const _conexion = 'No se pudo conectar con el servidor. Revisa la conexión e inténtalo de nuevo.';

/// Traduce un error de la API de asistencia (CU10, A1 a A5). Nunca incluye el CI ni el nombre del becado.
ErrorAsistencia mapearErrorAsistencia(Object error) {
  if (error is DioException) {
    final interno = error.error;
    if (interno is ApiException) return _deApi(interno);
    if (error.response == null) return const ErrorAsistencia(_conexion, red: true);
    if ((error.response?.statusCode ?? 0) >= 500) return const ErrorAsistencia(_servidor, red: true);
  }
  return const ErrorAsistencia(_generico);
}

ErrorAsistencia _deApi(ApiException e) {
  switch (e.code) {
    case 'ERR_ASI_01':
      return const ErrorAsistencia('El estudiante no figura en el padrón de becados.');
    case 'ERR_ASI_02':
      return const ErrorAsistencia('La beca no está vigente: no se puede registrar la asistencia.');
    case 'ERR_ASI_03':
      return const ErrorAsistencia('La beca ya no existe. Busca al becado de nuevo.');
    case 'ERR_AUTH_07':
      return const ErrorAsistencia('No tienes permiso para registrar asistencia.');
    case 'ERR_SYS_00':
      return const ErrorAsistencia(_servidor, red: true);
    case 'ERR_SYS_01':
      return ErrorAsistencia(e.message);
    default:
      return const ErrorAsistencia(_generico);
  }
}
