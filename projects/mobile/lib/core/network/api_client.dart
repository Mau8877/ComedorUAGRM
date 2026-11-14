import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

import '../storage/token_storage.dart';

/// Excepción tipada para un error de negocio devuelto por el backend
/// (`status: "failed"`, ver RESPONSES_BACKEND.md) -- nunca se deja propagar
/// un [DioException] crudo, sin código, hasta la UI.
class ApiException implements Exception {
  final String code;
  final String message;

  ApiException({required this.code, required this.message});

  @override
  String toString() => 'ApiException($code): $message';
}

/// Cliente HTTP único de la app -- todo `service` de cada feature llama a
/// través de acá, nunca crea su propia instancia de [Dio] (ver
/// CONSUMO_API_MOBILE.md).
///
/// * Un interceptor agrega `Authorization: Bearer <token de acceso>` y, ante
/// * un 401 de una ruta protegida, renueva la sesión una sola vez (aunque
/// * fallen varias peticiones juntas) y reintenta la petición. Los callbacks
/// * [onRenovar] y [onSesionInvalida] los registra la feature de auth, para
/// * que `core/` no importe `features/`. Un 401 de login, registro, refresh o
/// * logout es un error del formulario y no dispara nada.
/// * Otro desempaqueta el sobre estándar del backend
/// * (`{status, data, message, error, timestamp, meta?}`): en éxito deja
/// * pasar `data` (y `meta` si vino) como si fuera la respuesta real; en
/// * error, lanza [ApiException] en vez de un [DioException] sin código de
/// * negocio.
/// * Mobile NO usa las cookies de la web: aunque el backend también las
/// * mande, este cliente no las guarda y manda el token en el header.
/// ! Ni los tokens ni los cuerpos de `/auth/*` se escriben en el log.
class ApiClient {
  static Dio? _instance;

  /// Intenta renovar la sesión con el refresh token guardado; `true` si lo logró.
  static Future<bool> Function()? onRenovar;

  /// Se llama cuando la sesión ya no se puede recuperar (A8): hay que ir al login.
  static VoidCallback? onSesionInvalida;

  static Future<bool>? _renovando;

  static const _rutasDeAcceso = ['/auth/login', '/auth/registro', '/auth/refresh', '/auth/logout'];
  static const _marcaReintentada = 'reintentada';

  static Dio get instance {
    _instance ??= construir(
      dotenv.env['API_BASE_URL'] ?? 'http://10.0.2.2:8080/api/v1',
      TokenStorage.instance,
      logs: dotenv.env['ENABLE_API_LOGS'] == 'true',
    );
    return _instance!;
  }

  static bool _esRutaDeAcceso(String path) => _rutasDeAcceso.any(path.contains);

  /// Una sola renovación a la vez: las demás peticiones esperan el mismo resultado.
  static Future<bool> _renovarUnaVez() {
    final renovar = onRenovar;
    if (renovar == null) return Future.value(false);
    return _renovando ??= renovar().catchError((_) => false).whenComplete(() => _renovando = null);
  }

  /// Arma el cliente. Público solo para poder probarlo con un adaptador HTTP simulado.
  @visibleForTesting
  static Dio construir(String baseUrl, TokenStorage tokens, {bool logs = false}) {
    final dio = Dio(BaseOptions(baseUrl: baseUrl));

    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          final token = tokens.accessToken;
          if (token != null) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          handler.next(options);
        },
        onError: (error, handler) async {
          final opciones = error.requestOptions;
          final esExpirada = error.response?.statusCode == 401 &&
              opciones.extra[_marcaReintentada] != true &&
              !_esRutaDeAcceso(opciones.path);
          if (!esExpirada) {
            handler.next(error);
            return;
          }

          if (await _renovarUnaVez()) {
            opciones.extra[_marcaReintentada] = true;
            opciones.headers['Authorization'] = 'Bearer ${tokens.accessToken}';
            try {
              handler.resolve(await dio.fetch<dynamic>(opciones));
            } on DioException catch (e) {
              handler.next(e);
            }
            return;
          }
          onSesionInvalida?.call();
          handler.next(error);
        },
      ),
    );

    dio.interceptors.add(
      InterceptorsWrapper(
        onResponse: (response, handler) {
          final envelope = response.data as Map<String, dynamic>;
          response.data = envelope['meta'] != null
              ? {'data': envelope['data'], 'meta': envelope['meta']}
              : envelope['data'];
          handler.next(response);
        },
        onError: (error, handler) {
          final envelope = error.response?.data;
          if (envelope is Map<String, dynamic> && envelope['error'] != null) {
            handler.reject(
              DioException(
                requestOptions: error.requestOptions,
                error: ApiException(
                  code: envelope['error'] as String,
                  message: envelope['message'] as String? ?? 'Ocurrió un error inesperado',
                ),
                response: error.response,
              ),
            );
            return;
          }
          handler.next(error);
        },
      ),
    );

    if (logs) {
      dio.interceptors.add(_LogSeguro());
    }

    return dio;
  }
}

/// Log de peticiones sin datos sensibles: nunca imprime headers (llevaría el
/// `Authorization`) ni el cuerpo de `/auth/*` (lleva contraseñas y tokens).
class _LogSeguro extends Interceptor {
  bool _esAuth(String path) => path.contains('/auth/');

  @override
  void onResponse(Response<dynamic> response, ResponseInterceptorHandler handler) {
    final path = response.requestOptions.path;
    final cuerpo = _esAuth(path) ? '(oculto)' : '${response.data}';
    debugPrint('[API] ${response.requestOptions.method} $path -> ${response.statusCode} $cuerpo');
    handler.next(response);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    debugPrint('[API] ${err.requestOptions.method} ${err.requestOptions.path} -> ${err.response?.statusCode ?? err.type.name}');
    handler.next(err);
  }
}
