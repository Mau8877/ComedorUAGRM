import 'package:dio/dio.dart';

import '../../core/network/api_client.dart';

/// Error de los documentos de la postulación (CU16) ya traducido a lo que ve la persona.
class ErrorDocumentos {
  final String mensaje;

  /// La postulación o el documento ya no existen o no son del usuario: la pantalla actualiza la lista.
  final bool refrescar;

  /// La postulación ya no se puede modificar (ya se envió o la convocatoria cerró): la pantalla actualiza la lista.
  final bool noEditable;

  /// Error de conexión o del servidor: la persona puede reintentar sin perder los demás documentos (A4).
  final bool red;

  const ErrorDocumentos(this.mensaje, {this.refrescar = false, this.noEditable = false, this.red = false});
}

const _generico = 'Ocurrió un error inesperado. Inténtalo de nuevo.';
const _servidor = 'Ocurrió un error en el servidor. Inténtalo de nuevo en unos minutos.';
const _conexion =
    'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo; tus otros documentos siguen guardados.';

/// Traduce un error de la API de documentos (CU16). Los `400` traen su motivo en español. Nunca incluye datos sensibles.
ErrorDocumentos mapearErrorDocumentos(Object error) {
  if (error is DioException) {
    final interno = error.error;
    if (interno is ApiException) return _deApi(interno);
    if (error.response == null) return const ErrorDocumentos(_conexion, red: true);
    if ((error.response?.statusCode ?? 0) >= 500) return const ErrorDocumentos(_servidor, red: true);
  }
  return const ErrorDocumentos(_generico);
}

ErrorDocumentos _deApi(ApiException e) {
  switch (e.code) {
    case 'ERR_DOC_04':
    case 'ERR_DOC_09':
      return const ErrorDocumentos('El documento o la postulación ya no existe.', refrescar: true);
    case 'ERR_DOC_06':
      return const ErrorDocumentos(
        'La postulación ya fue enviada: sus documentos no se pueden modificar.',
        noEditable: true,
      );
    case 'ERR_DOC_07':
      return const ErrorDocumentos('La convocatoria ya no está abierta.', noEditable: true);
    case 'ERR_AUTH_07':
      return const ErrorDocumentos('No tienes permiso para esta acción.');
    case 'ERR_SYS_00':
      return const ErrorDocumentos(_servidor, red: true);
    case 'ERR_DOC_01':
    case 'ERR_DOC_02':
    case 'ERR_DOC_03':
    case 'ERR_DOC_05':
    case 'ERR_DOC_08':
    case 'ERR_SYS_05':
    case 'ERR_SYS_06':
      return ErrorDocumentos(e.message);
    default:
      return const ErrorDocumentos(_generico);
  }
}
