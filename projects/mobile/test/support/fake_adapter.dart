import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';

/// Adaptador HTTP de pruebas: responde con lo que decida `responder` y guarda las peticiones.
class FakeAdapter implements HttpClientAdapter {
  final ResponseBody Function(RequestOptions) responder;
  final List<RequestOptions> peticiones = [];

  FakeAdapter(this.responder);

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    peticiones.add(options);
    return responder(options);
  }

  @override
  void close({bool force = false}) {}
}

ResponseBody respuestaJson(int status, Map<String, dynamic> cuerpo) => ResponseBody.fromString(
      jsonEncode(cuerpo),
      status,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );

/// Sobre de éxito del backend (ver RESPONSES_BACKEND.md).
Map<String, dynamic> exito(Object? data, {Map<String, dynamic>? meta}) => {
      'status': 'success',
      'data': data,
      'message': '',
      'timestamp': '',
      'meta': ?meta,
    };

/// Sobre de error del backend con su código catalogado.
Map<String, dynamic> fallo(String codigo, {String mensaje = 'mensaje del backend'}) => {
      'status': 'failed',
      'data': null,
      'message': mensaje,
      'timestamp': '',
      'error': codigo,
    };

const usuarioJson = {
  'codigo': 'u1',
  'nombres': 'Ana',
  'apellidos': 'Perez',
  'correo': 'ana@uagrm.edu.bo',
  'rol': 'POSTULANTE',
};

Map<String, dynamic> sesionJson({String access = 'acceso-1', String refresh = 'refresh-1', String rol = 'POSTULANTE'}) => {
      'accessToken': access,
      'refreshToken': refresh,
      'tokenType': 'Bearer',
      'expiresIn': 900,
      'usuario': {...usuarioJson, 'rol': rol},
    };
