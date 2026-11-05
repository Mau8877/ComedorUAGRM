import 'package:comedor_uagrm_mobile/core/network/api_client.dart';
import 'package:comedor_uagrm_mobile/core/storage/token_storage.dart';
import 'package:comedor_uagrm_mobile/features/auth/auth_error_mapper.dart';
import 'package:comedor_uagrm_mobile/features/auth/providers/auth_servicios_provider.dart';
import 'package:comedor_uagrm_mobile/features/auth/providers/auth_sesion_provider.dart';
import 'package:comedor_uagrm_mobile/features/auth/providers/auth_usuario_provider.dart';
import 'package:comedor_uagrm_mobile/features/auth/models/usuario.dart';
import 'package:comedor_uagrm_mobile/features/perfil/models/actualizar_perfil_request.dart';
import 'package:comedor_uagrm_mobile/features/perfil/models/perfil.dart';
import 'package:comedor_uagrm_mobile/features/perfil/providers/perfil_provider.dart';
import 'package:comedor_uagrm_mobile/features/perfil/providers/perfil_servicios_provider.dart';
import 'package:comedor_uagrm_mobile/features/perfil/services/perfil_service.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../support/fake_adapter.dart';
import '../../widget_test.dart' show FakeAuthSesion;

const perfilPostulanteJson = {
  'codigo': 'u1',
  'nombres': 'Ana',
  'apellidos': 'Perez',
  'ci': '1234567',
  'correo': 'ana@uagrm.edu.bo',
  'rol': 'POSTULANTE',
  'activo': true,
  'postulante': {
    'registroUniversitario': '219012345',
    'carrera': {'codigo': 'c1', 'nombre': 'Ingeniería Informática', 'facultad': 'Tecnología'},
    'telefono': '70000001',
  },
};

const perfilAdminJson = {
  'codigo': 'u2',
  'nombres': 'Luis',
  'apellidos': 'Rojas',
  'ci': '7654321',
  'correo': 'luis@uagrm.edu.bo',
  'rol': 'ADMINISTRADOR_DUBSS',
  'activo': true,
  'postulante': null,
};

Perfil _perfil([Map<String, dynamic> json = perfilPostulanteJson]) => Perfil.fromJson(json);

DioException _apiError(String code, [String message = 'mensaje']) => DioException(
      requestOptions: RequestOptions(path: '/x'),
      error: ApiException(code: code, message: message),
      response: Response(requestOptions: RequestOptions(path: '/x'), statusCode: 400),
    );

void main() {
  group('Perfil.fromJson', () {
    test('de un Postulante trae registro, carrera y teléfono', () {
      final perfil = _perfil();

      expect(perfil.ci, '1234567');
      expect(perfil.esPostulante, isTrue);
      expect(perfil.postulante!.registroUniversitario, '219012345');
      expect(perfil.postulante!.carrera.nombre, 'Ingeniería Informática');
      expect(perfil.postulante!.telefono, '70000001');
    });

    test('de otro rol no trae datos de Postulante', () {
      final perfil = _perfil(perfilAdminJson);

      expect(perfil.esPostulante, isFalse);
      expect(perfil.postulante, isNull);
      expect(perfil.rol, 'ADMINISTRADOR_DUBSS');
    });
  });

  group('ActualizarPerfilRequest', () {
    ActualizarPerfilRequest desde({String correo = 'ana@uagrm.edu.bo', String telefono = '70000001', String clave = '', Perfil? perfil}) =>
        ActualizarPerfilRequest.desdeCambios(
          perfil: perfil ?? _perfil(),
          correo: correo,
          telefono: telefono,
          contrasenaActual: clave,
        );

    test('solo el teléfono: no pide ni manda contraseña', () {
      final request = desde(telefono: ' 71111111 ', clave: 'ignorada');

      expect(request.toJson(), {'telefono': '71111111'});
    });

    test('el correo cambiado viaja con la contraseña actual', () {
      final request = desde(correo: ' nuevo@uagrm.edu.bo ', clave: 'secreta1');

      expect(request.toJson(), {'correo': 'nuevo@uagrm.edu.bo', 'contrasenaActual': 'secreta1'});
    });

    test('un correo que solo cambia de mayúsculas o espacios no cuenta como cambio', () {
      final request = desde(correo: '  ANA@uagrm.edu.bo ');

      expect(request.vacio, isTrue);
      expect(request.toJson(), isEmpty);
    });

    test('nunca incluye el CI y omite lo que no cambió', () {
      final request = desde(correo: 'otro@uagrm.edu.bo', clave: 'x');

      expect(request.toJson().keys, isNot(contains('ci')));
      expect(request.toJson().keys, isNot(contains('telefono')));
    });

    test('un usuario sin teléfono (no Postulante) nunca manda teléfono', () {
      final request = desde(perfil: _perfil(perfilAdminJson), correo: 'luis@uagrm.edu.bo', telefono: '999');

      expect(request.vacio, isTrue);
    });
  });

  group('PerfilService', () {
    late FakeAdapter adapter;

    PerfilService servicioCon(ResponseBody Function(RequestOptions) responder) {
      FlutterSecureStorage.setMockInitialValues({});
      adapter = FakeAdapter(responder);
      final dio = ApiClient.construir('http://api.test/api/v1', TokenStorage());
      dio.httpClientAdapter = adapter;
      return PerfilService(dio);
    }

    test('obtener pide GET /perfil y devuelve el perfil', () async {
      final servicio = servicioCon((_) => respuestaJson(200, exito(perfilPostulanteJson)));

      final perfil = await servicio.obtener();

      expect(perfil.correo, 'ana@uagrm.edu.bo');
      expect(adapter.peticiones.single.method, 'GET');
      expect(adapter.peticiones.single.path, '/perfil');
    });

    test('actualizar hace PATCH /perfil solo con lo que cambió', () async {
      final servicio = servicioCon((_) => respuestaJson(200, exito(perfilPostulanteJson)));

      await servicio.actualizar(const ActualizarPerfilRequest(telefono: '71111111'));

      expect(adapter.peticiones.single.method, 'PATCH');
      expect(adapter.peticiones.single.path, '/perfil');
      expect(adapter.peticiones.single.data, {'telefono': '71111111'});
    });

    test('cambiarContrasena hace POST /auth/cambiar-contrasena con el refresh token en el cuerpo', () async {
      final servicio = servicioCon((_) => respuestaJson(200, exito(null)));

      await servicio.cambiarContrasena(
        contrasenaActual: 'actual-1',
        contrasenaNueva: 'nueva-123',
        confirmacion: 'nueva-123',
        refreshToken: 'refresh-guardado',
      );

      expect(adapter.peticiones.single.path, '/auth/cambiar-contrasena');
      expect(adapter.peticiones.single.data, {
        'contrasenaActual': 'actual-1',
        'contrasenaNueva': 'nueva-123',
        'confirmacionContrasena': 'nueva-123',
        'refreshToken': 'refresh-guardado',
      });
    });

    test('cambiarContrasena sin refresh token no manda ese campo', () async {
      final servicio = servicioCon((_) => respuestaJson(200, exito(null)));

      await servicio.cambiarContrasena(contrasenaActual: 'a', contrasenaNueva: 'nueva-123', confirmacion: 'nueva-123');

      expect((adapter.peticiones.single.data as Map).keys, isNot(contains('refreshToken')));
    });
  });

  group('mapearError (CU15)', () {
    test('ERR_AUTH_09 marca la contraseña actual con su propio mensaje', () {
      final error = mapearError(_apiError('ERR_AUTH_09', 'lo que diga el backend'));

      expect(error.mensaje, 'La contraseña actual es incorrecta.');
      expect(error.campo, CampoAuth.contrasenaActual);
    });

    test('ERR_US_02 marca el correo y ERR_US_13 / ERR_SYS_01 muestran el mensaje del backend', () {
      expect(mapearError(_apiError('ERR_US_02', 'Ya existe una cuenta con ese correo')).campo, CampoAuth.correo);
      expect(mapearError(_apiError('ERR_US_13', 'Sin teléfono')).mensaje, 'Sin teléfono');
      expect(mapearError(_apiError('ERR_SYS_01', 'Dato inválido')).mensaje, 'Dato inválido');
    });

    test('un error de red ofrece reintentar', () {
      final error = mapearError(DioException(requestOptions: RequestOptions(path: '/x'), type: DioExceptionType.connectionError));

      expect(error.red, isTrue);
    });
  });

  group('providers del perfil', () {
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
        perfilServiceProvider.overrideWithValue(PerfilService(dio)),
        authSesionProvider.overrideWith(() => FakeAuthSesion(Usuario.fromJson(usuarioJson))),
      ]);
      addTearDown(c.dispose);
      return c;
    }

    test('perfilDetailProvider carga el perfil del usuario autenticado', () async {
      final c = contenedor((_) => respuestaJson(200, exito(perfilPostulanteJson)));

      final perfil = await c.read(perfilDetailProvider.future);

      expect(perfil.ci, '1234567');
    });

    test('actualizar devuelve null al salir bien y deja el correo nuevo en el usuario de la sesión', () async {
      final c = contenedor((_) => respuestaJson(200, exito({...perfilPostulanteJson, 'correo': 'nuevo@uagrm.edu.bo'})));
      await c.read(authSesionProvider.future);

      final error = await c.read(perfilProvider.notifier).actualizar(
            const ActualizarPerfilRequest(correo: 'nuevo@uagrm.edu.bo', contrasenaActual: 'secreta1'),
          );

      expect(error, isNull);
      expect(c.read(authUsuarioProvider)?.correo, 'nuevo@uagrm.edu.bo');
      expect(c.read(perfilProvider), isFalse);
    });

    test('actualizar con un correo ajeno devuelve el error de ese campo (A5)', () async {
      final c = contenedor((_) => respuestaJson(409, fallo('ERR_US_02', mensaje: 'Ya existe una cuenta con ese correo')));

      final error = await c.read(perfilProvider.notifier).actualizar(const ActualizarPerfilRequest(correo: 'otro@uagrm.edu.bo'));

      expect(error?.campo, CampoAuth.correo);
    });

    test('cambiarContrasena manda el refresh token guardado y devuelve null', () async {
      final c = contenedor(
        (_) => respuestaJson(200, exito(null)),
        guardado: {'comedoruagrm.refresh_token': 'refresh-guardado'},
      );

      final error = await c.read(perfilProvider.notifier).cambiarContrasena(
            contrasenaActual: 'actual-1',
            contrasenaNueva: 'nueva-123',
            confirmacion: 'nueva-123',
          );

      expect(error, isNull);
      expect((adapter.peticiones.single.data as Map)['refreshToken'], 'refresh-guardado');
    });

    test('una contraseña actual incorrecta (400) marca ese campo y NO cierra la sesión (A6)', () async {
      final c = contenedor(
        (_) => respuestaJson(400, fallo('ERR_AUTH_09')),
        guardado: {'comedoruagrm.refresh_token': 'refresh-guardado'},
      );
      await c.read(authSesionProvider.future);

      final error = await c.read(perfilProvider.notifier).cambiarContrasena(
            contrasenaActual: 'mala',
            contrasenaNueva: 'nueva-123',
            confirmacion: 'nueva-123',
          );

      expect(error?.campo, CampoAuth.contrasenaActual);
      expect(c.read(authUsuarioProvider), isNotNull);
      expect(await tokens.leerRefreshToken(), 'refresh-guardado');
    });

    test('un error de red se devuelve para reintentar y no cambia la sesión', () async {
      final c = contenedor((o) => throw DioException(requestOptions: o, type: DioExceptionType.connectionError));
      await c.read(authSesionProvider.future);

      final error = await c.read(perfilProvider.notifier).cambiarContrasena(
            contrasenaActual: 'a',
            contrasenaNueva: 'nueva-123',
            confirmacion: 'nueva-123',
          );

      expect(error?.red, isTrue);
      expect(c.read(authUsuarioProvider), isNotNull);
    });
  });
}
