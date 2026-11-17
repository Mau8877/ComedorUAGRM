import 'package:dio/dio.dart';

import '../models/asistencia_registrada.dart';
import '../models/becado_asistencia.dart';

/// Llamadas a la API de la asistencia digital (CU10). Recibe el [Dio] compartido de `ApiClient.instance`. Los
/// errores de negocio salen como `ApiException` dentro de un [DioException] (ya lo hace el interceptor).
///
/// ! El CI viaja en el cuerpo de un `POST`, nunca en la URL, y no se escribe en ningún log.
class AsistenciaService {
  final Dio _dio;

  AsistenciaService(this._dio);

  Future<BecadoAsistencia> buscarBecado(String ci) async {
    final response = await _dio.post<Map<String, dynamic>>('/asistencias/buscar-becado', data: {'ci': ci});
    return BecadoAsistencia.fromJson(response.data!);
  }

  Future<AsistenciaRegistrada> registrar(String becaCodigo) async {
    final response = await _dio.post<Map<String, dynamic>>('/asistencias', data: {'becaCodigo': becaCodigo});
    return AsistenciaRegistrada.fromJson(response.data!);
  }
}
