import 'package:dio/dio.dart';

import '../../core/network/api_client.dart';

/// Error del envío de la postulación (CU-02) ya traducido a lo que ve la persona.
class ErrorEnvio {
  final String mensaje;

  /// No hay convocatoria abierta (`ERR_ENV_01`): se muestra como aviso, no como fallo.
  final bool sinConvocatoria;

  /// Ya tiene una postulación vigente o no es elegible (`ERR_VAL_01`/`ERR_VAL_02`): no puede postular.
  final bool noPuedePostular;

  /// La convocatoria cerró (`ERR_VAL_03`) o la postulación ya se envió (`ERR_ENV_03`): el plazo finalizó.
  final bool cerrada;

  /// Error de conexión o del servidor: se puede reintentar sin perder lo escrito.
  final bool red;

  const ErrorEnvio(
    this.mensaje, {
    this.sinConvocatoria = false,
    this.noPuedePostular = false,
    this.cerrada = false,
    this.red = false,
  });
}

const _generico = 'Ocurrió un error inesperado. Inténtalo de nuevo.';
const _servidor = 'Ocurrió un error en el servidor. Inténtalo de nuevo en unos minutos.';
const _conexion =
    'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo; lo que escribiste se conserva.';

/// Traduce un error de la API del envío (CU-02). Los `400` y `409` traen su motivo en español. Nunca incluye datos
/// sensibles.
ErrorEnvio mapearErrorEnvio(Object error) {
  if (error is DioException) {
    final interno = error.error;
    if (interno is ApiException) return _deApi(interno);
    if (error.response == null) return const ErrorEnvio(_conexion, red: true);
    if ((error.response?.statusCode ?? 0) >= 500) return const ErrorEnvio(_servidor, red: true);
  }
  return const ErrorEnvio(_generico);
}

ErrorEnvio _deApi(ApiException e) {
  switch (e.code) {
    case 'ERR_ENV_01':
      return ErrorEnvio(e.message, sinConvocatoria: true);
    case 'ERR_VAL_01':
    case 'ERR_VAL_02':
      return ErrorEnvio(e.message, noPuedePostular: true);
    case 'ERR_VAL_03':
      return const ErrorEnvio('La convocatoria ya no recibe postulaciones: el plazo finalizó.', cerrada: true);
    case 'ERR_ENV_02':
      return const ErrorEnvio('La postulación no existe o no es tuya.', noPuedePostular: true);
    case 'ERR_ENV_03':
      return const ErrorEnvio('Esta postulación ya no se puede enviar.', cerrada: true);
    case 'ERR_AUTH_07':
      return const ErrorEnvio('No tienes permiso para esta acción.');
    case 'ERR_SYS_00':
      return const ErrorEnvio(_servidor, red: true);
    default:
      // ERR_ENV_04 (falta algo) y ERR_ENV_05 (dato inválido): el backend explica qué en español.
      return ErrorEnvio(e.message);
  }
}
