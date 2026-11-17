import 'package:dio/dio.dart';

import '../models/mi_beca.dart';
import '../models/pagina_asistencias.dart';

/// Llamadas a la API de «Mi beca» (CU24), solo lectura. Recibe el [Dio] compartido de `ApiClient.instance`. La beca
/// es siempre la del usuario de la sesión: la app no manda ningún identificador. Los errores de negocio salen como
/// `ApiException` dentro de un [DioException] (ya lo hace el interceptor).
class MiBecaService {
  final Dio _dio;

  MiBecaService(this._dio);

  /// La beca del usuario; sin beca da `ERR_MBE_01` (404).
  Future<MiBeca> obtener() async {
    final response = await _dio.get<Map<String, dynamic>>('/mi-beca');
    return MiBeca.fromJson(response.data!);
  }

  /// Una página del historial (la más reciente primero por defecto). Lo vacío no viaja; `desde` y `hasta` van
  /// como `AAAA-MM-DD`.
  Future<PaginaAsistencias> listarAsistencias({String? desde, String? hasta, int page = 1, int pageSize = 20}) async {
    final response = await _dio.get<Map<String, dynamic>>('/mi-beca/asistencias', queryParameters: {
      'page': page,
      'pageSize': pageSize,
      'filter[desde]': ?desde,
      'filter[hasta]': ?hasta,
    });
    return PaginaAsistencias.fromJson(response.data!);
  }
}
