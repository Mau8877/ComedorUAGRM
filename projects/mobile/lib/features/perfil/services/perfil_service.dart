import 'package:dio/dio.dart';

import '../models/actualizar_perfil_request.dart';
import '../models/perfil.dart';

/// Llamadas a la API del perfil (CU15). Recibe el [Dio] compartido de `ApiClient.instance`,
/// no crea el suyo. Los errores de negocio salen como `ApiException` dentro de un
/// [DioException] (ya lo hace el interceptor).
class PerfilService {
  final Dio _dio;

  PerfilService(this._dio);

  Future<Perfil> obtener() async {
    final response = await _dio.get<Map<String, dynamic>>('/perfil');
    return Perfil.fromJson(response.data!);
  }

  Future<Perfil> actualizar(ActualizarPerfilRequest request) async {
    final response = await _dio.patch<Map<String, dynamic>>('/perfil', data: request.toJson());
    return Perfil.fromJson(response.data!);
  }

  /// El backend cierra las sesiones de los demás dispositivos y conserva la que corresponde al
  /// [refreshToken] (mobile lo manda en el cuerpo; la web usa una cookie).
  Future<void> cambiarContrasena({
    required String contrasenaActual,
    required String contrasenaNueva,
    required String confirmacion,
    String? refreshToken,
  }) async {
    await _dio.post<void>('/auth/cambiar-contrasena', data: {
      'contrasenaActual': contrasenaActual,
      'contrasenaNueva': contrasenaNueva,
      'confirmacionContrasena': confirmacion,
      'refreshToken': ?refreshToken,
    });
  }
}
