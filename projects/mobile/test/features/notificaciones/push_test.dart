import 'dart:async';

import 'package:comedor_uagrm_mobile/core/network/api_client.dart';
import 'package:comedor_uagrm_mobile/core/storage/token_storage.dart';
import 'package:comedor_uagrm_mobile/features/auth/providers/auth_provider.dart';
import 'package:comedor_uagrm_mobile/features/auth/providers/auth_servicios_provider.dart';
import 'package:comedor_uagrm_mobile/features/auth/providers/auth_sesion_provider.dart';
import 'package:comedor_uagrm_mobile/features/auth/services/auth_service.dart';
import 'package:comedor_uagrm_mobile/features/notificaciones/models/mensaje_push.dart';
import 'package:comedor_uagrm_mobile/features/notificaciones/providers/avisos_push_provider.dart';
import 'package:comedor_uagrm_mobile/features/notificaciones/providers/notificaciones_servicios_provider.dart';
import 'package:comedor_uagrm_mobile/features/notificaciones/providers/push_registro_provider.dart';
import 'package:comedor_uagrm_mobile/features/notificaciones/services/dispositivos_service.dart';
import 'package:comedor_uagrm_mobile/features/notificaciones/services/push_service.dart';
import 'package:comedor_uagrm_mobile/features/notificaciones/widgets/aviso_push.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../support/fake_adapter.dart';

/// Firebase simulado: permiso, token, renovaciones y avisos controlables desde el test.
class PushFalso implements PushService {
  bool permiso = true;
  String? token = 'tok-1';
  int vecesPidioPermiso = 0;
  final renovaciones = StreamController<String>.broadcast();
  final avisos = StreamController<MensajePush>.broadcast();

  @override
  Future<bool> pedirPermiso() async {
    vecesPidioPermiso++;
    return permiso;
  }

  @override
  Future<String?> obtenerToken() async => token;

  @override
  Stream<String> get tokenRenovado => renovaciones.stream;

  @override
  Stream<MensajePush> get mensajes => avisos.stream;
}

Future<void> asentar() async {
  for (var i = 0; i < 10; i++) {
    await Future<void>.delayed(Duration.zero);
  }
}

void main() {
  late FakeAdapter adapter;
  late PushFalso push;

  const refreshGuardado = {'comedoruagrm.refresh_token': 'refresh-guardado'};

  ProviderContainer contenedor({
    Map<String, String> guardado = const {},
    ResponseBody Function(RequestOptions)? responder,
  }) {
    FlutterSecureStorage.setMockInitialValues({...guardado});
    final tokens = TokenStorage();
    adapter = FakeAdapter(responder ?? (_) => respuestaJson(200, exito(sesionJson())));
    final dio = ApiClient.construir('http://api.test/api/v1', tokens);
    dio.httpClientAdapter = adapter;
    push = PushFalso();
    final c = ProviderContainer(overrides: [
      tokenStorageProvider.overrideWithValue(tokens),
      authServiceProvider.overrideWithValue(AuthService(dio)),
      pushServiceProvider.overrideWithValue(push),
      dispositivosServiceProvider.overrideWithValue(DispositivosService(dio)),
    ]);
    addTearDown(c.dispose);
    return c;
  }

  Iterable<RequestOptions> aDispositivos() => adapter.peticiones.where((p) => p.path == '/dispositivos-fcm');

  tearDown(() {
    ApiClient.onRenovar = null;
    ApiClient.onSesionInvalida = null;
  });

  group('DispositivosService', () {
    test('registrar hace POST /dispositivos-fcm con el token y la plataforma ANDROID', () async {
      FlutterSecureStorage.setMockInitialValues({});
      adapter = FakeAdapter((_) => respuestaJson(201, exito({'codigo': 'd1', 'plataforma': 'ANDROID'})));
      final dio = ApiClient.construir('http://api.test/api/v1', TokenStorage())..httpClientAdapter = adapter;

      await DispositivosService(dio).registrar('tok-1');

      expect(adapter.peticiones.single.method, 'POST');
      expect(adapter.peticiones.single.path, '/dispositivos-fcm');
      expect(adapter.peticiones.single.data, {'token': 'tok-1', 'plataforma': 'ANDROID'});
    });

    test('darDeBaja hace POST /dispositivos-fcm/baja solo con el token', () async {
      FlutterSecureStorage.setMockInitialValues({});
      adapter = FakeAdapter((_) => respuestaJson(200, exito(null)));
      final dio = ApiClient.construir('http://api.test/api/v1', TokenStorage())..httpClientAdapter = adapter;

      await DispositivosService(dio).darDeBaja('tok-1');

      expect(adapter.peticiones.single.path, '/dispositivos-fcm/baja');
      expect(adapter.peticiones.single.data, {'token': 'tok-1'});
    });
  });

  group('pushRegistroProvider (registro y baja según la sesión)', () {
    test('al restaurarse la sesión pide permiso, obtiene el token y lo registra', () async {
      final c = contenedor(guardado: refreshGuardado);
      c.read(pushRegistroProvider);

      await c.read(authSesionProvider.future);
      await asentar();

      expect(push.vecesPidioPermiso, 1);
      expect(aDispositivos().single.data, {'token': 'tok-1', 'plataforma': 'ANDROID'});
      expect(c.read(pushRegistroProvider), 'tok-1');
    });

    test('al iniciar sesión (no solo al restaurarla) también registra el teléfono', () async {
      final c = contenedor();
      c.read(pushRegistroProvider);
      await c.read(authSesionProvider.future);
      expect(aDispositivos(), isEmpty);

      await c.read(authProvider.notifier).iniciarSesion('ana@uagrm.edu.bo', 'secreta');
      await asentar();

      expect(aDispositivos(), hasLength(1));
      expect(c.read(pushRegistroProvider), 'tok-1');
    });

    test('sin sesión no pide permiso ni llama a la API', () async {
      final c = contenedor();
      c.read(pushRegistroProvider);

      await c.read(authSesionProvider.future);
      await asentar();

      expect(push.vecesPidioPermiso, 0);
      expect(adapter.peticiones, isEmpty);
      expect(c.read(pushRegistroProvider), isNull);
    });

    test('si el usuario no concede el permiso no registra nada y la app sigue (A1, CA05)', () async {
      final c = contenedor(guardado: refreshGuardado);
      c.read(pushRegistroProvider);
      push.permiso = false;

      await c.read(authSesionProvider.future);
      await asentar();

      expect(push.vecesPidioPermiso, 1);
      expect(aDispositivos(), isEmpty);
      expect(c.read(pushRegistroProvider), isNull);
    });

    test('sin token de Firebase no registra nada', () async {
      final c = contenedor(guardado: refreshGuardado);
      c.read(pushRegistroProvider);
      push.token = null;

      await c.read(authSesionProvider.future);
      await asentar();

      expect(aDispositivos(), isEmpty);
    });

    test('cuando Firebase renueva el token, lo vuelve a registrar (CA03)', () async {
      final c = contenedor(guardado: refreshGuardado);
      c.read(pushRegistroProvider);
      await c.read(authSesionProvider.future);
      await asentar();

      push.renovaciones.add('tok-2');
      await asentar();

      expect(aDispositivos().map((p) => (p.data as Map)['token']), ['tok-1', 'tok-2']);
      expect(c.read(pushRegistroProvider), 'tok-2');
    });

    test('si el registro falla por red, la app sigue y no queda token registrado', () async {
      final c = contenedor(
        guardado: refreshGuardado,
        responder: (o) => o.path == '/dispositivos-fcm' ? respuestaJson(500, fallo('ERR_SYS_00')) : respuestaJson(200, exito(sesionJson())),
      );
      c.read(pushRegistroProvider);

      await c.read(authSesionProvider.future);
      await asentar();

      expect(c.read(pushRegistroProvider), isNull);
    });

    test('cerrarSesion da de baja el teléfono ANTES de revocar la sesión (CA04)', () async {
      final c = contenedor(guardado: refreshGuardado);
      c.read(pushRegistroProvider);
      await c.read(authSesionProvider.future);
      await asentar();

      await c.read(authProvider.notifier).cerrarSesion();

      final rutas = adapter.peticiones.map((p) => p.path).toList();
      expect(rutas.indexOf('/dispositivos-fcm/baja'), isNonNegative);
      expect(rutas.indexOf('/dispositivos-fcm/baja'), lessThan(rutas.indexOf('/auth/logout')));
      expect(adapter.peticiones.firstWhere((p) => p.path == '/dispositivos-fcm/baja').data, {'token': 'tok-1'});
      expect(c.read(pushRegistroProvider), isNull);
    });

    test('cerrarSesion cierra igual aunque la baja falle (A8)', () async {
      final c = contenedor(
        guardado: refreshGuardado,
        responder: (o) => o.path == '/dispositivos-fcm/baja' ? respuestaJson(500, fallo('ERR_SYS_00')) : respuestaJson(200, exito(sesionJson())),
      );
      c.read(pushRegistroProvider);
      await c.read(authSesionProvider.future);
      await asentar();

      await c.read(authProvider.notifier).cerrarSesion();

      expect(await c.read(authSesionProvider.future), isNull);
      expect(c.read(authProvider), isFalse);
    });

    test('cerrarSesion sin teléfono registrado no llama a la baja', () async {
      final c = contenedor(guardado: refreshGuardado);
      c.read(pushRegistroProvider);
      push.permiso = false;
      await c.read(authSesionProvider.future);
      await asentar();

      await c.read(authProvider.notifier).cerrarSesion();

      expect(adapter.peticiones.where((p) => p.path == '/dispositivos-fcm/baja'), isEmpty);
    });
  });

  group('avisos con la app abierta', () {
    test('avisosPushProvider entrega los mensajes que llegan por Firebase', () async {
      final c = contenedor();
      final recibidos = <MensajePush>[];
      c.listen<AsyncValue<MensajePush>>(avisosPushProvider, (_, s) {
        final m = s.valueOrNull;
        if (m != null) recibidos.add(m);
      });

      push.avisos.add(const MensajePush(titulo: 'Titulo', cuerpo: 'Cuerpo', evento: 'PRUEBA'));
      await asentar();

      expect(recibidos.single.titulo, 'Titulo');
      expect(recibidos.single.evento, 'PRUEBA');
    });

    testWidgets('mostrarAvisoPush muestra título y cuerpo sin cambiar de pantalla (CA12)', (tester) async {
      final llave = GlobalKey<ScaffoldMessengerState>();
      await tester.pumpWidget(MaterialApp(
        scaffoldMessengerKey: llave,
        home: const Scaffold(body: Text('Pantalla actual')),
      ));

      mostrarAvisoPush(llave, const MensajePush(titulo: 'Resultados', cuerpo: 'Ya puedes verlos'));
      await tester.pump();

      expect(find.text('Resultados'), findsOneWidget);
      expect(find.text('Ya puedes verlos'), findsOneWidget);
      expect(find.text('Pantalla actual'), findsOneWidget);
    });

    testWidgets('mostrarAvisoPush sin messenger montado no falla', (tester) async {
      mostrarAvisoPush(GlobalKey<ScaffoldMessengerState>(), const MensajePush(titulo: 'x', cuerpo: 'y'));
    });
  });
}
