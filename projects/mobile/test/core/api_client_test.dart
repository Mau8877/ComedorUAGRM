import 'package:comedor_uagrm_mobile/core/network/api_client.dart';
import 'package:comedor_uagrm_mobile/core/storage/token_storage.dart';
import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';

import '../support/fake_adapter.dart';

void main() {
  late TokenStorage tokens;

  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
    tokens = TokenStorage();
  });

  tearDown(() {
    ApiClient.onRenovar = null;
    ApiClient.onSesionInvalida = null;
  });

  Dio clienteCon(FakeAdapter adapter) {
    final dio = ApiClient.construir('http://api.test/api/v1', tokens);
    dio.httpClientAdapter = adapter;
    return dio;
  }

  test('agrega Authorization: Bearer cuando hay token de acceso', () async {
    tokens.accessToken = 'mi-token';
    final adapter = FakeAdapter((_) => respuestaJson(200, exito({'ok': true})));

    await clienteCon(adapter).get<dynamic>('/x');

    expect(adapter.peticiones.single.headers['Authorization'], 'Bearer mi-token');
  });

  test('no manda Authorization sin sesión', () async {
    final adapter = FakeAdapter((_) => respuestaJson(200, exito({'ok': true})));

    await clienteCon(adapter).get<dynamic>('/x');

    expect(adapter.peticiones.single.headers.containsKey('Authorization'), isFalse);
  });

  test('desempaqueta el sobre de éxito y el de error se vuelve ApiException con su código', () async {
    final ok = FakeAdapter((_) => respuestaJson(200, exito({'a': 1})));
    expect((await clienteCon(ok).get<dynamic>('/x')).data, {'a': 1});

    final mal = FakeAdapter((_) => respuestaJson(409, fallo('ERR_US_01')));
    await expectLater(
      clienteCon(mal).get<dynamic>('/x'),
      throwsA(isA<DioException>().having((e) => (e.error as ApiException).code, 'code', 'ERR_US_01')),
    );
  });

  test('ante un 401 renueva la sesión y reintenta la petición con el token nuevo', () async {
    tokens.accessToken = 'vencido';
    var renovaciones = 0;
    ApiClient.onRenovar = () async {
      renovaciones++;
      tokens.accessToken = 'nuevo';
      return true;
    };
    final adapter = FakeAdapter((o) => o.headers['Authorization'] == 'Bearer nuevo'
        ? respuestaJson(200, exito({'ok': true}))
        : respuestaJson(401, fallo('ERR_AUTH_01')));

    final respuesta = await clienteCon(adapter).get<dynamic>('/x');

    expect(respuesta.data, {'ok': true});
    expect(renovaciones, 1);
    expect(adapter.peticiones, hasLength(2));
  });

  test('renueva una sola vez aunque fallen tres peticiones a la vez', () async {
    tokens.accessToken = 'vencido';
    var renovaciones = 0;
    ApiClient.onRenovar = () async {
      renovaciones++;
      await Future<void>.delayed(const Duration(milliseconds: 20));
      tokens.accessToken = 'nuevo';
      return true;
    };
    final adapter = FakeAdapter((o) => o.headers['Authorization'] == 'Bearer nuevo'
        ? respuestaJson(200, exito({'ok': true}))
        : respuestaJson(401, fallo('ERR_AUTH_01')));
    final dio = clienteCon(adapter);

    await Future.wait([dio.get<dynamic>('/a'), dio.get<dynamic>('/b'), dio.get<dynamic>('/c')]);

    expect(renovaciones, 1);
  });

  test('si no se puede renovar avisa que la sesión es inválida y propaga el error', () async {
    ApiClient.onRenovar = () async => false;
    var sesionInvalida = 0;
    ApiClient.onSesionInvalida = () => sesionInvalida++;
    final adapter = FakeAdapter((_) => respuestaJson(401, fallo('ERR_AUTH_01')));

    await expectLater(
      clienteCon(adapter).get<dynamic>('/x'),
      throwsA(isA<DioException>().having((e) => (e.error as ApiException).code, 'code', 'ERR_AUTH_01')),
    );

    expect(sesionInvalida, 1);
  });

  test('si el reintento también da 401 no renueva de nuevo ni entra en bucle', () async {
    var renovaciones = 0;
    ApiClient.onRenovar = () async {
      renovaciones++;
      return true;
    };
    final adapter = FakeAdapter((_) => respuestaJson(401, fallo('ERR_AUTH_01')));

    await expectLater(clienteCon(adapter).get<dynamic>('/x'), throwsA(isA<DioException>()));

    expect(renovaciones, 1);
    expect(adapter.peticiones, hasLength(2));
  });

  test('un 401 de login, registro, refresh o logout es un error del formulario: no renueva', () async {
    var renovaciones = 0;
    ApiClient.onRenovar = () async {
      renovaciones++;
      return true;
    };
    final adapter = FakeAdapter((_) => respuestaJson(401, fallo('ERR_AUTH_04')));
    final dio = clienteCon(adapter);

    for (final ruta in ['/auth/login', '/auth/registro', '/auth/refresh', '/auth/logout']) {
      await expectLater(dio.post<dynamic>(ruta), throwsA(isA<DioException>()));
    }

    expect(renovaciones, 0);
  });

  test('un error que no es 401 no dispara la renovación', () async {
    var renovaciones = 0;
    ApiClient.onRenovar = () async {
      renovaciones++;
      return true;
    };
    final adapter = FakeAdapter((_) => respuestaJson(500, fallo('ERR_SYS_00')));

    await expectLater(clienteCon(adapter).get<dynamic>('/x'), throwsA(isA<DioException>()));

    expect(renovaciones, 0);
  });
}
