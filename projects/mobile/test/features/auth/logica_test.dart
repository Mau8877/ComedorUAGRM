import 'package:comedor_uagrm_mobile/core/network/api_client.dart';
import 'package:comedor_uagrm_mobile/core/routing/app_router.dart';
import 'package:comedor_uagrm_mobile/features/auth/auth_error_mapper.dart';
import 'package:comedor_uagrm_mobile/features/auth/auth_validators.dart';
import 'package:comedor_uagrm_mobile/features/auth/models/sesion.dart';
import 'package:comedor_uagrm_mobile/features/auth/models/usuario.dart';
import 'package:comedor_uagrm_mobile/features/auth/rol_destino.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../support/fake_adapter.dart';

DioException _apiError(String code, [String message = 'mensaje']) => DioException(
      requestOptions: RequestOptions(path: '/x'),
      error: ApiException(code: code, message: message),
      response: Response(requestOptions: RequestOptions(path: '/x'), statusCode: 400),
    );

Usuario _usuario(String rol) => Usuario.fromJson({...usuarioJson, 'rol': rol});

void main() {
  group('modelos', () {
    test('Sesion.fromJson anida al Usuario', () {
      final sesion = Sesion.fromJson(sesionJson());

      expect(sesion.accessToken, 'acceso-1');
      expect(sesion.expiresIn, 900);
      expect(sesion.usuario.nombreCompleto, 'Ana Perez');
    });
  });

  group('rutaInicioPorRol', () {
    test('el postulante va a su módulo', () {
      expect(rutaInicioPorRol('POSTULANTE'), '/estudiante');
    });

    test('cualquier otro rol (o uno desconocido) va a /solo-postulantes', () {
      for (final rol in ['ADMINISTRADOR_DUBSS', 'DIRECTIVO', 'PERSONAL_COMEDOR', 'HACKER']) {
        expect(rutaInicioPorRol(rol), '/solo-postulantes', reason: rol);
      }
    });

    test('etiquetaRol traduce y deja tal cual uno desconocido', () {
      expect(etiquetaRol('ADMINISTRADOR_DUBSS'), 'Administrador DUBSS');
      expect(etiquetaRol('OTRO'), 'OTRO');
    });
  });

  group('mapearError', () {
    test('ERR_AUTH_04 da siempre el mismo mensaje de credenciales', () {
      expect(mapearError(_apiError('ERR_AUTH_04', 'lo que diga el backend')).mensaje, 'Correo o contraseña incorrectos.');
    });

    test('ERR_AUTH_05 muestra el mensaje del backend (trae los minutos)', () {
      expect(mapearError(_apiError('ERR_AUTH_05', 'Vuelve a intentarlo en 5 minutos.')).mensaje, 'Vuelve a intentarlo en 5 minutos.');
    });

    test('ERR_AUTH_03 indica que la cuenta está desactivada y a quién acudir', () {
      expect(mapearError(_apiError('ERR_AUTH_03')).mensaje, contains('DUBSS'));
    });

    test('marca el campo en conflicto', () {
      expect(mapearError(_apiError('ERR_US_01')).campo, CampoAuth.ci);
      expect(mapearError(_apiError('ERR_US_02')).campo, CampoAuth.correo);
      expect(mapearError(_apiError('ERR_US_03')).campo, CampoAuth.registroUniversitario);
      expect(mapearError(_apiError('ERR_US_04')).campo, CampoAuth.carrera);
    });

    test('un error de conexión ofrece reintentar', () {
      final sinRespuesta = DioException(requestOptions: RequestOptions(path: '/x'), type: DioExceptionType.connectionError);

      final error = mapearError(sinRespuesta);

      expect(error.red, isTrue);
      expect(error.campo, isNull);
    });

    test('un código desconocido da el mensaje genérico sin filtrar detalles', () {
      final error = mapearError(_apiError('ERR_SYS_00', 'stacktrace interno'));

      expect(error.mensaje, isNot(contains('stacktrace')));
      expect(error.red, isFalse);
    });
  });

  group('AuthValidators', () {
    test('contraseña: 5 caracteres falla, 6 pasa sin mayúscula ni número, 73 falla', () {
      expect(AuthValidators.contrasena('12345'), 'La contraseña debe tener al menos 6 caracteres');
      expect(AuthValidators.contrasena('abcdef'), isNull);
      expect(AuthValidators.contrasena('a' * 73), isNotNull);
      expect(AuthValidators.contrasena(''), 'La contraseña es obligatoria');
    });

    test('la confirmación debe coincidir', () {
      expect(AuthValidators.confirmacion('otra', 'secreta'), 'Las contraseñas no coinciden');
      expect(AuthValidators.confirmacion('secreta', 'secreta'), isNull);
    });

    test('registro universitario: 9 dígitos y sin cero inicial', () {
      for (final malo in ['21901234', '2190123456', '019012345', '21901234a']) {
        expect(AuthValidators.registroUniversitario(malo), isNotNull, reason: malo);
      }
      expect(AuthValidators.registroUniversitario('100000000'), isNull);
      expect(AuthValidators.registroUniversitario(''), 'El registro universitario es obligatorio');
    });

    test('correo con y sin formato válido', () {
      expect(AuthValidators.correo('ana@uagrm.edu.bo'), isNull);
      expect(AuthValidators.correo('no-es-un-correo'), 'El correo electrónico no es válido');
      expect(AuthValidators.correo(' '), 'El correo electrónico es obligatorio');
    });

    test('campos obligatorios en español', () {
      expect(AuthValidators.nombres(''), 'Los nombres son obligatorios');
      expect(AuthValidators.ci(null), 'El CI es obligatorio');
      expect(AuthValidators.carrera(null), 'La carrera es obligatoria');
      expect(AuthValidators.ci('1' * 21), isNotNull);
      expect(AuthValidators.ci('12a45'), 'El CI solo puede contener dígitos');
      expect(AuthValidators.ci('1234567'), isNull);
    });
  });

  group('redirectDeSesion', () {
    const cargando = AsyncLoading<Usuario?>();
    const sinSesion = AsyncData<Usuario?>(null);

    test('mientras se restaura la sesión todo va a /cargando', () {
      expect(redirectDeSesion(sesion: cargando, ubicacion: '/estudiante'), '/cargando');
      expect(redirectDeSesion(sesion: cargando, ubicacion: '/login'), '/cargando');
      expect(redirectDeSesion(sesion: cargando, ubicacion: '/cargando'), isNull);
    });

    test('sin sesión una ruta privada lleva a /login (CA15, CA18)', () {
      expect(redirectDeSesion(sesion: sinSesion, ubicacion: '/estudiante'), '/login');
      expect(redirectDeSesion(sesion: sinSesion, ubicacion: '/'), '/login');
      expect(redirectDeSesion(sesion: sinSesion, ubicacion: '/solo-postulantes'), '/login');
      expect(redirectDeSesion(sesion: sinSesion, ubicacion: '/perfil'), '/login');
    });

    test('sin sesión se pueden ver /login, /registro y /recuperar-contrasena', () {
      expect(redirectDeSesion(sesion: sinSesion, ubicacion: '/login'), isNull);
      expect(redirectDeSesion(sesion: sinSesion, ubicacion: '/registro'), isNull);
      expect(redirectDeSesion(sesion: sinSesion, ubicacion: '/recuperar-contrasena'), isNull);
    });

    test('con sesión /recuperar-contrasena lleva al módulo del rol (CU15)', () {
      expect(redirectDeSesion(sesion: AsyncData<Usuario?>(_usuario('POSTULANTE')), ubicacion: '/recuperar-contrasena'), '/estudiante');
    });

    test('mientras se restaura la sesión /recuperar-contrasena también espera en /cargando', () {
      expect(redirectDeSesion(sesion: cargando, ubicacion: '/recuperar-contrasena'), '/cargando');
    });

    test('al salir de /cargando sin sesión va a /login', () {
      expect(redirectDeSesion(sesion: sinSesion, ubicacion: '/cargando'), '/login');
    });

    test('con sesión, /login, /registro, / y /cargando llevan al módulo del rol', () {
      final postulante = AsyncData<Usuario?>(_usuario('POSTULANTE'));
      for (final ruta in ['/login', '/registro', '/', '/cargando']) {
        expect(redirectDeSesion(sesion: postulante, ubicacion: ruta), '/estudiante', reason: ruta);
      }
      expect(redirectDeSesion(sesion: AsyncData<Usuario?>(_usuario('ADMINISTRADOR_DUBSS')), ubicacion: '/login'), '/solo-postulantes');
    });

    test('un rol que no es postulante aterriza en /solo-postulantes al iniciar o restaurar la sesión', () {
      for (final rol in ['ADMINISTRADOR_DUBSS', 'DIRECTIVO', 'PERSONAL_COMEDOR']) {
        final sesion = AsyncData<Usuario?>(_usuario(rol));
        for (final ruta in ['/login', '/registro', '/', '/cargando']) {
          expect(redirectDeSesion(sesion: sesion, ubicacion: ruta), '/solo-postulantes', reason: '$rol $ruta');
        }
      }
    });

    test('un rol que no es postulante no puede navegar a pantallas de postulante', () {
      final admin = AsyncData<Usuario?>(_usuario('ADMINISTRADOR_DUBSS'));
      for (final ruta in ['/estudiante', '/admin', '/perfil', '/mis-postulaciones/abc']) {
        expect(redirectDeSesion(sesion: admin, ubicacion: ruta), '/solo-postulantes', reason: ruta);
      }
      expect(redirectDeSesion(sesion: admin, ubicacion: '/solo-postulantes'), isNull);
    });

    test('un postulante no se queda en /solo-postulantes', () {
      expect(redirectDeSesion(sesion: AsyncData<Usuario?>(_usuario('POSTULANTE')), ubicacion: '/solo-postulantes'), '/estudiante');
    });

    test('con sesión una ruta privada se deja pasar', () {
      final postulante = AsyncData<Usuario?>(_usuario('POSTULANTE'));
      expect(redirectDeSesion(sesion: postulante, ubicacion: '/estudiante'), isNull);
      expect(redirectDeSesion(sesion: postulante, ubicacion: '/ingredientes'), isNull);
    });

    test('una sesión que se invalida (A8) pasa de con-sesión a /login', () {
      expect(redirectDeSesion(sesion: sinSesion, ubicacion: '/estudiante'), '/login');
    });
  });
}
