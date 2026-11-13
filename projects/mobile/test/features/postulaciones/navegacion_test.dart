import 'dart:async';

import 'package:comedor_uagrm_mobile/core/routing/app_router.dart';
import 'package:comedor_uagrm_mobile/core/theme/app_theme.dart';
import 'package:comedor_uagrm_mobile/features/auth/models/usuario.dart';
import 'package:comedor_uagrm_mobile/features/auth/providers/auth_sesion_provider.dart';
import 'package:comedor_uagrm_mobile/features/notificaciones/destino_aviso.dart';
import 'package:comedor_uagrm_mobile/features/notificaciones/models/mensaje_push.dart';
import 'package:comedor_uagrm_mobile/features/notificaciones/providers/avisos_tocados_provider.dart';
import 'package:comedor_uagrm_mobile/features/notificaciones/providers/notificaciones_servicios_provider.dart';
import 'package:comedor_uagrm_mobile/features/notificaciones/services/push_service.dart';
import 'package:comedor_uagrm_mobile/features/notificaciones/widgets/aviso_push.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/providers/postulaciones_servicios_provider.dart';
import 'package:comedor_uagrm_mobile/layouts/estudiante/estudiante_shell.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import '../../support/fake_adapter.dart';
import '../../widget_test.dart' show FakeAuthSesion;
import 'postulaciones_logica_test.dart' show resumenJson;
import 'postulaciones_pantallas_test.dart' show ServicioFalso, pagina;

/// Firebase simulado: solo importan los avisos tocados.
class PushFalso implements PushService {
  final tocados = StreamController<MensajePush>.broadcast();

  @override
  Future<bool> pedirPermiso() async => true;

  @override
  Future<String?> obtenerToken() async => null;

  @override
  Stream<String> get tokenRenovado => const Stream.empty();

  @override
  Stream<MensajePush> get mensajes => const Stream.empty();

  @override
  Stream<MensajePush> get avisosTocados => tocados.stream;
}

/// Sesión que empieza cerrada y se puede abrir desde el test (como un login).
class SesionControlable extends AuthSesionNotifier {
  final Usuario? inicial;

  SesionControlable([this.inicial]);

  @override
  Future<Usuario?> build() async => inicial;

  void iniciar(Usuario usuario) => state = AsyncData(usuario);
}

MensajePush aviso({String? postulacion}) =>
    MensajePush(titulo: 'Titulo', cuerpo: 'Cuerpo', postulacionCodigo: postulacion);

Future<void> asentar() async {
  for (var i = 0; i < 10; i++) {
    await Future<void>.delayed(Duration.zero);
  }
}

void main() {
  final usuario = Usuario.fromJson(usuarioJson);

  group('rutaDeAviso (el dato del push -> la ruta)', () {
    test('un aviso con postulacionCodigo lleva al detalle de esa postulación', () {
      expect(rutaDeAviso(aviso(postulacion: 'post-9')), '/mis-postulaciones/post-9');
    });

    test('un aviso sin código, con código vacío o sin aviso no lleva a ningún lado', () {
      expect(rutaDeAviso(aviso()), isNull);
      expect(rutaDeAviso(aviso(postulacion: '')), isNull);
      expect(rutaDeAviso(null), isNull);
    });
  });

  group('navegacionDeAvisosProvider (tocar un push, CU04 A6)', () {
    late PushFalso push;
    late List<String> navegadas;

    ProviderContainer contenedor(SesionControlable sesion) {
      push = PushFalso();
      navegadas = [];
      final c = ProviderContainer(overrides: [
        authSesionProvider.overrideWith(() => sesion),
        pushServiceProvider.overrideWithValue(push),
        navegarARutaProvider.overrideWithValue(navegadas.add),
      ]);
      addTearDown(c.dispose);
      c.read(navegacionDeAvisosProvider);
      return c;
    }

    test('con sesión, tocar un aviso con postulación navega al detalle', () async {
      final c = contenedor(SesionControlable(usuario));
      await c.read(authSesionProvider.future);

      push.tocados.add(aviso(postulacion: 'post-1'));
      await asentar();

      expect(navegadas, ['/mis-postulaciones/post-1']);
    });

    test('un aviso sin código no navega: la app solo se abre', () async {
      final c = contenedor(SesionControlable(usuario));
      await c.read(authSesionProvider.future);

      push.tocados.add(aviso());
      await asentar();

      expect(navegadas, isEmpty);
    });

    test('sin sesión no navega todavía: guarda el destino y navega al iniciar sesión', () async {
      final sesion = SesionControlable();
      final c = contenedor(sesion);
      await c.read(authSesionProvider.future);

      push.tocados.add(aviso(postulacion: 'post-2'));
      await asentar();
      expect(navegadas, isEmpty);
      expect(c.read(destinoAvisoProvider), '/mis-postulaciones/post-2');

      sesion.iniciar(usuario);
      await asentar();

      expect(navegadas, ['/mis-postulaciones/post-2']);
      expect(c.read(destinoAvisoProvider), isNull);
    });

    test('el destino pendiente se usa una sola vez', () async {
      final sesion = SesionControlable();
      final c = contenedor(sesion);
      await c.read(authSesionProvider.future);
      push.tocados.add(aviso(postulacion: 'post-3'));
      await asentar();
      sesion.iniciar(usuario);
      await asentar();

      sesion.iniciar(Usuario.fromJson({...usuarioJson, 'nombres': 'Otra'}));
      await asentar();

      expect(navegadas, ['/mis-postulaciones/post-3']);
    });
  });

  group('redirect global', () {
    test('sin sesión, el detalle lleva primero al login', () {
      expect(
        redirectDeSesion(sesion: const AsyncData<Usuario?>(null), ubicacion: '/mis-postulaciones/post-1'),
        '/login',
      );
    });

    test('con sesión el detalle se deja pasar', () {
      expect(
        redirectDeSesion(sesion: AsyncData<Usuario?>(usuario), ubicacion: '/mis-postulaciones/post-1'),
        isNull,
      );
    });
  });

  group('aviso en primer plano', () {
    testWidgets('con «Ver» el aviso ofrece abrir la postulación; sin él no hay acción', (tester) async {
      final llave = GlobalKey<ScaffoldMessengerState>();
      var vistos = 0;
      await tester.pumpWidget(MaterialApp(
        scaffoldMessengerKey: llave,
        theme: AppTheme.light,
        home: const Scaffold(body: SizedBox()),
      ));

      mostrarAvisoPush(llave, aviso(postulacion: 'post-1'), alVer: () => vistos++);
      await tester.pumpAndSettle();
      await tester.tap(find.text('Ver'));
      await tester.pumpAndSettle();
      expect(vistos, 1);

      mostrarAvisoPush(llave, aviso());
      await tester.pumpAndSettle();
      expect(find.text('Ver'), findsNothing);
    });
  });

  group('EstudianteShell: sección «Mis postulaciones»', () {
    Widget app(ServicioFalso servicio) => ProviderScope(
          overrides: [
            authSesionProvider.overrideWith(() => FakeAuthSesion(Usuario.fromJson(usuarioJson))),
            postulacionesServiceProvider.overrideWithValue(servicio),
          ],
          child: MaterialApp.router(
            theme: AppTheme.light,
            routerConfig: GoRouter(
              initialLocation: '/estudiante',
              routes: [
                GoRoute(path: '/estudiante', builder: (context, state) => const EstudianteShell()),
                GoRoute(
                  path: '/mis-postulaciones/:codigo',
                  builder: (context, state) => Scaffold(body: Text('detalle ${state.pathParameters['codigo']}')),
                ),
              ],
            ),
          ),
        );

    testWidgets('la barra tiene la sección y abre la lista de postulaciones', (tester) async {
      final servicio = ServicioFalso()..listar = (_, _) async => pagina([resumenJson(1), resumenJson(2)]);
      await tester.pumpWidget(app(servicio));
      await tester.pumpAndSettle();

      await tester.tap(find.descendant(of: find.byType(NavigationBar), matching: find.text('Mis postulaciones')));
      await tester.pumpAndSettle();

      expect(find.text('Beca 1'), findsOneWidget);
      expect(find.text('Beca 2'), findsOneWidget);
      expect(servicio.llamadas, hasLength(1));
    });

    testWidgets('desde la lista, tocar una postulación abre su detalle', (tester) async {
      final servicio = ServicioFalso()..listar = (_, _) async => pagina([resumenJson(1)]);
      await tester.pumpWidget(app(servicio));
      await tester.pumpAndSettle();
      await tester.tap(find.descendant(of: find.byType(NavigationBar), matching: find.text('Mis postulaciones')));
      await tester.pumpAndSettle();

      await tester.tap(find.text('Beca 1'));
      await tester.pumpAndSettle();

      expect(find.text('detalle post-1'), findsOneWidget);
    });

    testWidgets('«Perfil» sigue siendo la última sección y las demás no se tocan', (tester) async {
      await tester.pumpWidget(app(ServicioFalso()));
      await tester.pumpAndSettle();

      final etiquetas = tester
          .widgetList<NavigationDestination>(find.byType(NavigationDestination))
          .map((d) => d.label)
          .toList();

      expect(etiquetas, ['Mi Menú', 'Mis Pedidos', 'Mis postulaciones', 'Perfil']);
    });
  });
}
