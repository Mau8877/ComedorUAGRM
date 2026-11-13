import 'package:dio/dio.dart';

import '../models/mi_postulacion_detalle.dart';
import '../models/pagina_postulaciones.dart';

/// Llamadas a la API de «Mis postulaciones» (CU04), solo lectura. Recibe el [Dio] compartido de
/// `ApiClient.instance`, no crea el suyo. Los errores de negocio salen como `ApiException` dentro de un
/// [DioException] (ya lo hace el interceptor).
class PostulacionesService {
  final Dio _dio;

  PostulacionesService(this._dio);

  /// Una página de las postulaciones enviadas del usuario (la más reciente primero por defecto). Lo vacío no viaja.
  Future<PaginaPostulaciones> listarMias({
    String? gestionAcademica,
    int page = 1,
    int pageSize = 20,
    String? sort,
  }) async {
    final response = await _dio.get<Map<String, dynamic>>('/postulaciones/mias', queryParameters: {
      'page': page,
      'pageSize': pageSize,
      'filter[gestionAcademica]': ?gestionAcademica,
      'sort': ?sort,
    });
    return PaginaPostulaciones.fromJson(response.data!);
  }

  /// El detalle de una postulación propia; una ajena o inexistente da `ERR_POS_01` (404).
  Future<MiPostulacionDetalle> obtener(String codigo) async {
    final response = await _dio.get<Map<String, dynamic>>('/postulaciones/$codigo');
    return MiPostulacionDetalle.fromJson(response.data!);
  }
}
