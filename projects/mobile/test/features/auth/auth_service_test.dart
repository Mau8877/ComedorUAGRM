import 'package:comedor_uagrm_mobile/core/network/api_client.dart';
import 'package:comedor_uagrm_mobile/core/storage/token_storage.dart';
import 'package:comedor_uagrm_mobile/features/auth/models/registro_request.dart';
import 'package:comedor_uagrm_mobile/features/auth/services/auth_service.dart';
import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../support/fake_adapter.dart';

void main() {
  late FakeAdapter adapter;

  AuthService servicioCon(ResponseBody Function(RequestOptions) responder) {
    FlutterSecureStorage.setMockInitialValues({});
    adapter = FakeAdapter(responder);
    final dio = ApiClient.construir('http://api.test/api/v1', TokenStorage());
    dio.httpClientAdapter = adapter;
    return AuthService(dio);
  }

  const registro = RegistroRequest(
    nombres: 'Ana',
    apellidos: 'Perez',
    ci: '1234567',
    registroUniversitario: '219012345',
    carreraCodigo: 'c1',
    telefono: '70000001',
    correo: 'ana@uagrm.edu.bo',
    contrasena: 'secreta',
    confirmacionContrasena: 'secreta',
  );

  test('listarCarreras pide GET /carreras?pageSize=200 y devuelve el catálogo', () async {
    final servicio = servicioCon((_) => respuestaJson(
          200,
          exito([
            {'codigo': 'c1', 'nombre': 'Ingeniería Informática', 'facultad': 'Tecnología'},
          ], meta: {'page': 1, 'pageSize': 200, 'totalItems': 1, 'totalPages': 1}),
        ));

    final carreras = await servicio.listarCarreras();

    expect(carreras.single.nombre, 'Ingeniería Informática');
    expect(adapter.peticiones.single.method, 'GET');
    expect(adapter.peticiones.single.path, '/carreras');
    expect(adapter.peticiones.single.queryParameters, {'pageSize': 200});
  });

  test('solicitarRecuperacion hace POST /auth/recuperar-contrasena solo con el correo', () async {
    final servicio = servicioCon((_) => respuestaJson(200, exito(null)));

    await servicio.solicitarRecuperacion('ana@uagrm.edu.bo');

    expect(adapter.peticiones.single.method, 'POST');
    expect(adapter.peticiones.single.path, '/auth/recuperar-contrasena');
    expect(adapter.peticiones.single.data, {'correo': 'ana@uagrm.edu.bo'});
  });

  test('registrar hace POST /auth/registro con todos los campos y devuelve la sesión', () async {
    final servicio = servicioCon((_) => respuestaJson(201, exito(sesionJson())));

    final sesion = await servicio.registrar(registro);

    expect(sesion.usuario.correo, 'ana@uagrm.edu.bo');
    expect(adapter.peticiones.single.path, '/auth/registro');
    expect(adapter.peticiones.single.data, containsPair('confirmacionContrasena', 'secreta'));
    expect(adapter.peticiones.single.data, isNot(contains('rol')));
  });

  test('iniciarSesion hace POST /auth/login con correo y contraseña', () async {
    final servicio = servicioCon((_) => respuestaJson(200, exito(sesionJson())));

    final sesion = await servicio.iniciarSesion('ana@uagrm.edu.bo', 'secreta');

    expect(sesion.accessToken, 'acceso-1');
    expect(adapter.peticiones.single.data, {'correo': 'ana@uagrm.edu.bo', 'contrasena': 'secreta'});
  });

  test('renovar manda el refresh token en el cuerpo (mobile no usa cookies)', () async {
    final servicio = servicioCon((_) => respuestaJson(200, exito(sesionJson(refresh: 'refresh-2'))));

    final sesion = await servicio.renovar('refresh-1');

    expect(sesion.refreshToken, 'refresh-2');
    expect(adapter.peticiones.single.path, '/auth/refresh');
    expect(adapter.peticiones.single.data, {'refreshToken': 'refresh-1'});
  });

  test('cerrarSesion hace POST /auth/logout con el refresh token', () async {
    final servicio = servicioCon((_) => respuestaJson(200, exito(null)));

    await servicio.cerrarSesion('refresh-1');

    expect(adapter.peticiones.single.path, '/auth/logout');
    expect(adapter.peticiones.single.data, {'refreshToken': 'refresh-1'});
  });

  test('usuarioActual hace GET /auth/usuario-actual', () async {
    final servicio = servicioCon((_) => respuestaJson(200, exito(usuarioJson)));

    final usuario = await servicio.usuarioActual();

    expect(usuario.nombreCompleto, 'Ana Perez');
  });

  test('un error ERR_US_01 llega como ApiException con su code', () async {
    final servicio = servicioCon((_) => respuestaJson(409, fallo('ERR_US_01')));

    await expectLater(
      servicio.registrar(registro),
      throwsA(isA<DioException>().having((e) => (e.error as ApiException).code, 'code', 'ERR_US_01')),
    );
  });
}
