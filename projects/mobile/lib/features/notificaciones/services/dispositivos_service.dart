import 'package:dio/dio.dart';

/// Llamadas a la API de dispositivos (CU-EXTRA-01). Recibe el [Dio] compartido de
/// `ApiClient.instance`, no crea el suyo.
///
/// ! El token del teléfono no se escribe en ningún log.
class DispositivosService {
  final Dio _dio;

  DispositivosService(this._dio);

  /// Registra (o vuelve a registrar) el token de este teléfono para el usuario de la sesión.
  Future<void> registrar(String token) async {
    await _dio.post<void>('/dispositivos-fcm', data: {'token': token, 'plataforma': 'ANDROID'});
  }

  /// Da de baja el token: este teléfono deja de recibir los avisos de la sesión que se cierra.
  Future<void> darDeBaja(String token) async {
    await _dio.post<void>('/dispositivos-fcm/baja', data: {'token': token});
  }
}
