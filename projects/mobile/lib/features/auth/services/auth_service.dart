import 'package:dio/dio.dart';

import '../models/carrera.dart';
import '../models/registro_request.dart';
import '../models/sesion.dart';
import '../models/usuario.dart';

/// Llamadas a la API de cuentas (CU01). Recibe el [Dio] compartido de
/// `ApiClient.instance`, no crea el suyo. Los errores de negocio salen como
/// `ApiException` dentro de un [DioException] (ya lo hace el interceptor).
class AuthService {
  final Dio _dio;

  AuthService(this._dio);

  Future<List<Carrera>> listarCarreras() async {
    // Con `meta` el interceptor entrega {data, meta}.
    final response = await _dio.get<Map<String, dynamic>>(
      '/carreras',
      queryParameters: {'pageSize': 200},
    );
    final lista = response.data!['data'] as List<dynamic>;
    return lista.map((c) => Carrera.fromJson(c as Map<String, dynamic>)).toList();
  }

  Future<Sesion> registrar(RegistroRequest request) async {
    final response = await _dio.post<Map<String, dynamic>>('/auth/registro', data: request.toJson());
    return Sesion.fromJson(response.data!);
  }

  Future<Sesion> iniciarSesion(String correo, String contrasena) async {
    final response = await _dio.post<Map<String, dynamic>>(
      '/auth/login',
      data: {'correo': correo, 'contrasena': contrasena},
    );
    return Sesion.fromJson(response.data!);
  }

  Future<Sesion> renovar(String refreshToken) async {
    final response = await _dio.post<Map<String, dynamic>>(
      '/auth/refresh',
      data: {'refreshToken': refreshToken},
    );
    return Sesion.fromJson(response.data!);
  }

  Future<void> cerrarSesion(String refreshToken) async {
    await _dio.post<void>('/auth/logout', data: {'refreshToken': refreshToken});
  }

  /// Pide el enlace de recuperación (CU15, flujo A). El backend responde siempre igual exista o no
  /// la cuenta: no se lee nada de la respuesta, para no poder distinguir los casos.
  Future<void> solicitarRecuperacion(String correo) async {
    await _dio.post<void>('/auth/recuperar-contrasena', data: {'correo': correo});
  }

  Future<Usuario> usuarioActual() async {
    final response = await _dio.get<Map<String, dynamic>>('/auth/usuario-actual');
    return Usuario.fromJson(response.data!);
  }
}
