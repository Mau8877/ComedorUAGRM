import 'package:comedor_uagrm_mobile/core/network/api_client.dart';
import 'package:comedor_uagrm_mobile/core/storage/token_storage.dart';
import 'package:comedor_uagrm_mobile/features/auth/auth_error_mapper.dart';
import 'package:comedor_uagrm_mobile/features/auth/models/registro_request.dart';
import 'package:comedor_uagrm_mobile/features/auth/providers/auth_provider.dart';
import 'package:comedor_uagrm_mobile/features/auth/providers/auth_servicios_provider.dart';
import 'package:comedor_uagrm_mobile/features/auth/providers/auth_sesion_provider.dart';
import 'package:comedor_uagrm_mobile/features/auth/providers/auth_usuario_provider.dart';
import 'package:comedor_uagrm_mobile/features/auth/services/auth_service.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../support/fake_adapter.dart';

const _registro = RegistroRequest(
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

void main() {
  late TokenStorage tokens;
  late FakeAdapter adapter;

  ProviderContainer contenedor(ResponseBody Function(RequestOptions) responder, {Map<String, String> guardado = const {}}) {
    FlutterSecureStorage.setMockInitialValues({...guardado});
    tokens = TokenStorage();
    adapter = FakeAdapter(responder);
    final dio = ApiClient.construir('http://api.test/api/v1', tokens);
    dio.httpClientAdapter = adapter;
    final c = ProviderContainer(overrides: [
      tokenStorageProvider.overrideWithValue(tokens),
      authServiceProvider.overrideWithValue(AuthService(dio)),
    ]);
    addTearDown(c.dispose);
    return c;
  }

  const refreshGuardado = {'comedoruagrm.refresh_token': 'refresh-guardado'};

  tearDown(() {
    ApiClient.onRenovar = null;
    ApiClient.onSesionInvalida = null;
  });

  group('authSesionProvider (restaurar la sesión al abrir la app)', () {
    test('sin refresh token guardado no hay sesión y no se llama a la API', () async {
      final c = contenedor((_) => respuestaJson(200, exito(sesionJson())));

      expect(await c.read(authSesionProvider.future), isNull);
      expect(adapter.peticiones, isEmpty);
    });

    test('con refresh token válido restaura la sesión y guarda el par nuevo', () async {
      final c = contenedor((_) => respuestaJson(200, exito(sesionJson(access: 'a2', refresh: 'r2'))), guardado: refreshGuardado);

      final usuario = await c.read(authSesionProvider.future);

      expect(usuario?.correo, 'ana@uagrm.edu.bo');
      expect(tokens.accessToken, 'a2');
      expect(await tokens.leerRefreshToken(), 'r2');
      expect(adapter.peticiones.single.data, {'refreshToken': 'refresh-guardado'});
      expect(c.read(authUsuarioProvider)?.nombres, 'Ana');
    });

    test('si el backend rechaza el refresh token (ERR_AUTH_06) borra la sesión', () async {
      final c = contenedor((_) => respuestaJson(401, fallo('ERR_AUTH_06')), guardado: refreshGuardado);

      expect(await c.read(authSesionProvider.future), isNull);
      expect(await tokens.leerRefreshToken(), isNull);
    });

    test('si solo falla la red no hay sesión pero el refresh token se conserva', () async {
      final c = contenedor(
        (o) => throw DioException(requestOptions: o, type: DioExceptionType.connectionError),
        guardado: refreshGuardado,
      );

      expect(await c.read(authSesionProvider.future), isNull);
      expect(await tokens.leerRefreshToken(), 'refresh-guardado');
    });

    test('cuando la sesión ya no se puede renovar (A8) cierra la sesión', () async {
      final c = contenedor((_) => respuestaJson(200, exito(sesionJson())), guardado: refreshGuardado);
      await c.read(authSesionProvider.future);
      expect(c.read(authUsuarioProvider), isNotNull);

      ApiClient.onSesionInvalida!();
      await Future<void>.delayed(Duration.zero);

      expect(c.read(authUsuarioProvider), isNull);
      expect(await tokens.leerRefreshToken(), isNull);
    });
  });

  group('authProvider (acciones)', () {
    test('iniciarSesion correcto deja la sesión abierta y guarda los tokens', () async {
      final c = contenedor((_) => respuestaJson(200, exito(sesionJson())));
      await c.read(authSesionProvider.future);

      final error = await c.read(authProvider.notifier).iniciarSesion(' ana@uagrm.edu.bo ', 'secreta');

      expect(error, isNull);
      expect(c.read(authUsuarioProvider)?.rol, 'POSTULANTE');
      expect(tokens.accessToken, 'acceso-1');
      expect(await tokens.leerRefreshToken(), 'refresh-1');
      expect(adapter.peticiones.single.data, containsPair('correo', 'ana@uagrm.edu.bo'));
    });

    test('credenciales incorrectas devuelven el mensaje genérico y no abren sesión', () async {
      final c = contenedor((_) => respuestaJson(401, fallo('ERR_AUTH_04')));
      await c.read(authSesionProvider.future);

      final error = await c.read(authProvider.notifier).iniciarSesion('ana@uagrm.edu.bo', 'mala');

      expect(error?.mensaje, 'Correo o contraseña incorrectos.');
      expect(c.read(authUsuarioProvider), isNull);
      expect(tokens.accessToken, isNull);
    });

    test('registrar correcto abre la sesión', () async {
      final c = contenedor((_) => respuestaJson(201, exito(sesionJson())));
      await c.read(authSesionProvider.future);

      expect(await c.read(authProvider.notifier).registrar(_registro), isNull);
      expect(c.read(authUsuarioProvider)?.correo, 'ana@uagrm.edu.bo');
    });

    test('un 409 marca el campo en conflicto y no abre sesión', () async {
      final c = contenedor((_) => respuestaJson(409, fallo('ERR_US_01', mensaje: 'Ya existe una cuenta con ese CI')));
      await c.read(authSesionProvider.future);

      final error = await c.read(authProvider.notifier).registrar(_registro);

      expect(error?.campo, CampoAuth.ci);
      expect(error?.mensaje, 'Ya existe una cuenta con ese CI');
      expect(c.read(authUsuarioProvider), isNull);
    });

    test('cerrarSesion revoca la sesión de este dispositivo y borra los tokens', () async {
      final c = contenedor(
        (o) => o.path.contains('/auth/logout') ? respuestaJson(200, exito(null)) : respuestaJson(200, exito(sesionJson())),
        guardado: refreshGuardado,
      );
      await c.read(authSesionProvider.future);

      await c.read(authProvider.notifier).cerrarSesion();

      expect(adapter.peticiones.last.path, '/auth/logout');
      expect(adapter.peticiones.last.data, {'refreshToken': 'refresh-1'});
      expect(c.read(authUsuarioProvider), isNull);
      expect(await tokens.leerRefreshToken(), isNull);
      expect(tokens.accessToken, isNull);
    });

    test('cerrarSesion limpia igual aunque el backend falle o la sesión ya hubiera expirado (A10)', () async {
      final c = contenedor(
        (o) => o.path.contains('/auth/logout') ? respuestaJson(401, fallo('ERR_AUTH_06')) : respuestaJson(200, exito(sesionJson())),
        guardado: refreshGuardado,
      );
      await c.read(authSesionProvider.future);

      await c.read(authProvider.notifier).cerrarSesion();

      expect(c.read(authUsuarioProvider), isNull);
      expect(await tokens.leerRefreshToken(), isNull);
      expect(c.read(authProvider), isFalse);
    });

    test('una segunda acción mientras hay una en curso se ignora (A7)', () async {
      final c = contenedor((_) => respuestaJson(200, exito(sesionJson())));
      await c.read(authSesionProvider.future);
      final acciones = c.read(authProvider.notifier);

      final primera = acciones.iniciarSesion('ana@uagrm.edu.bo', 'secreta');
      final segunda = await acciones.iniciarSesion('ana@uagrm.edu.bo', 'secreta');
      await primera;

      expect(segunda, isNotNull);
      expect(adapter.peticiones, hasLength(1));
    });
  });
}
