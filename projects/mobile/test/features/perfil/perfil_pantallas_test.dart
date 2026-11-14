import 'package:comedor_uagrm_mobile/core/theme/app_theme.dart';
import 'package:comedor_uagrm_mobile/features/auth/auth_error_mapper.dart';
import 'package:comedor_uagrm_mobile/features/auth/models/usuario.dart';
import 'package:comedor_uagrm_mobile/features/auth/providers/auth_provider.dart';
import 'package:comedor_uagrm_mobile/features/auth/providers/auth_sesion_provider.dart';
import 'package:comedor_uagrm_mobile/features/auth/screens/sin_modulo_screen.dart';
import 'package:comedor_uagrm_mobile/features/auth/widgets/menu_usuario.dart';
import 'package:comedor_uagrm_mobile/features/perfil/models/actualizar_perfil_request.dart';
import 'package:comedor_uagrm_mobile/features/perfil/models/perfil.dart';
import 'package:comedor_uagrm_mobile/features/perfil/providers/perfil_provider.dart';
import 'package:comedor_uagrm_mobile/features/perfil/widgets/cambiar_contrasena_form.dart';
import 'package:comedor_uagrm_mobile/features/perfil/widgets/datos_contacto_form.dart';
import 'package:comedor_uagrm_mobile/features/perfil/widgets/perfil_vista.dart';
import 'package:comedor_uagrm_mobile/layouts/estudiante/estudiante_shell.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import '../../support/fake_adapter.dart';
import '../../widget_test.dart' show FakeAuthSesion;
import 'perfil_logica_test.dart' show perfilAdminJson, perfilPostulanteJson;

/// Acciones simuladas: guardan lo pedido y devuelven el resultado que se les diga.
class FakePerfilAcciones extends PerfilAcciones {
  final List<ActualizarPerfilRequest> actualizaciones = [];
  final List<(String, String, String)> cambios = [];
  AuthError? resultado;

  FakePerfilAcciones([this.resultado]);

  @override
  Future<AuthError?> actualizar(ActualizarPerfilRequest request) async {
    actualizaciones.add(request);
    return resultado;
  }

  @override
  Future<AuthError?> cambiarContrasena({
    required String contrasenaActual,
    required String contrasenaNueva,
    required String confirmacion,
  }) async {
    cambios.add((contrasenaActual, contrasenaNueva, confirmacion));
    return resultado;
  }
}

class FakeAcciones extends AuthAcciones {
  int cierres = 0;

  @override
  Future<void> cerrarSesion() async => cierres++;
}

final _postulante = Perfil.fromJson(perfilPostulanteJson);
final _admin = Perfil.fromJson(perfilAdminJson);

Widget _app(Widget pantalla, {Perfil? perfil, FakePerfilAcciones? acciones, Usuario? usuario}) {
  return ProviderScope(
    overrides: [
      authSesionProvider.overrideWith(() => FakeAuthSesion(usuario ?? Usuario.fromJson(usuarioJson))),
      if (perfil != null) perfilDetailProvider.overrideWith((ref) async => perfil),
      if (acciones != null) perfilProvider.overrideWith(() => acciones),
    ],
    child: MaterialApp(theme: AppTheme.light, home: Scaffold(body: pantalla)),
  );
}

Finder _campo(String etiqueta) => find.widgetWithText(TextFormField, etiqueta);

void main() {
  group('PerfilVista (flujo C)', () {
    testWidgets('el Postulante ve sus datos con registro universitario y carrera', (tester) async {
      await tester.pumpWidget(_app(const PerfilVista(), perfil: _postulante, acciones: FakePerfilAcciones()));
      await tester.pumpAndSettle();

      expect(find.text('Mis datos'), findsOneWidget);
      expect(find.text('Ana'), findsOneWidget);
      expect(find.text('219012345'), findsOneWidget);
      expect(find.text('Ingeniería Informática (Tecnología)'), findsOneWidget);
      expect(find.text('Postulante'), findsOneWidget);
      expect(find.text('Activa'), findsOneWidget);
    });

    testWidgets('el CI se ve bloqueado y con su nota, nunca como campo editable (A4)', (tester) async {
      await tester.pumpWidget(_app(const PerfilVista(), perfil: _postulante, acciones: FakePerfilAcciones()));
      await tester.pumpAndSettle();

      final ci = tester.widget<TextFormField>(_campo('CI'));
      expect(ci.enabled, isFalse);
      expect(ci.initialValue, '1234567');
      expect(find.text('El CI no se puede modificar.'), findsOneWidget);
    });

    testWidgets('el Administrador no ve registro universitario, carrera ni teléfono', (tester) async {
      await tester.pumpWidget(_app(const PerfilVista(), perfil: _admin, acciones: FakePerfilAcciones()));
      await tester.pumpAndSettle();

      expect(find.text('Administrador DUBSS'), findsOneWidget);
      expect(find.text('Registro universitario'), findsNothing);
      expect(find.text('Carrera'), findsNothing);
      expect(find.text('Teléfono'), findsNothing);
      expect(find.text('Correo electrónico'), findsOneWidget);
    });

    testWidgets('un error al cargar muestra el aviso con «Reintentar»', (tester) async {
      await tester.pumpWidget(ProviderScope(
        overrides: [
          authSesionProvider.overrideWith(() => FakeAuthSesion(Usuario.fromJson(usuarioJson))),
          perfilDetailProvider.overrideWith((ref) => Future<Perfil>.error('sin conexión')),
        ],
        child: MaterialApp(theme: AppTheme.light, home: const Scaffold(body: PerfilVista())),
      ));
      await tester.pumpAndSettle();

      expect(find.text('Reintentar'), findsNothing); // un error genérico no es de red
      expect(find.byIcon(Icons.warning_amber_rounded), findsOneWidget);
    });
  });

  group('DatosContactoForm (flujo D)', () {
    testWidgets('sin cambios el botón «Guardar cambios» está deshabilitado', (tester) async {
      await tester.pumpWidget(_app(SingleChildScrollView(child: DatosContactoForm(perfil: _postulante)), acciones: FakePerfilAcciones()));
      await tester.pumpAndSettle();

      final boton = tester.widget<FilledButton>(find.widgetWithText(FilledButton, 'Guardar cambios'));
      expect(boton.onPressed, isNull);
    });

    testWidgets('cambiar solo el teléfono no pide la contraseña y manda solo el teléfono', (tester) async {
      final acciones = FakePerfilAcciones();
      await tester.pumpWidget(_app(SingleChildScrollView(child: DatosContactoForm(perfil: _postulante)), acciones: acciones));
      await tester.pumpAndSettle();

      await tester.enterText(_campo('Teléfono'), '71111111');
      await tester.pump();
      expect(find.text('Contraseña actual'), findsNothing);
      await tester.tap(find.widgetWithText(FilledButton, 'Guardar cambios'));
      await tester.pumpAndSettle();

      expect(acciones.actualizaciones.single.toJson(), {'telefono': '71111111'});
      expect(find.text('Datos actualizados'), findsOneWidget);
    });

    testWidgets('cambiar el correo muestra la contraseña actual, la exige y la manda (A7)', (tester) async {
      final acciones = FakePerfilAcciones();
      await tester.pumpWidget(_app(SingleChildScrollView(child: DatosContactoForm(perfil: _postulante)), acciones: acciones));
      await tester.pumpAndSettle();

      await tester.enterText(_campo('Correo electrónico'), 'nuevo@uagrm.edu.bo');
      await tester.pump();
      expect(find.text('Contraseña actual'), findsOneWidget);

      await tester.tap(find.widgetWithText(FilledButton, 'Guardar cambios'));
      await tester.pump();
      expect(find.text('Ingresa tu contraseña actual para cambiar el correo'), findsOneWidget);
      expect(acciones.actualizaciones, isEmpty);

      await tester.enterText(_campo('Contraseña actual'), 'secreta1');
      await tester.tap(find.widgetWithText(FilledButton, 'Guardar cambios'));
      await tester.pumpAndSettle();

      expect(acciones.actualizaciones.single.toJson(), {'correo': 'nuevo@uagrm.edu.bo', 'contrasenaActual': 'secreta1'});
    });

    testWidgets('si el correo vuelve al original la contraseña actual desaparece', (tester) async {
      await tester.pumpWidget(_app(SingleChildScrollView(child: DatosContactoForm(perfil: _postulante)), acciones: FakePerfilAcciones()));
      await tester.pumpAndSettle();

      await tester.enterText(_campo('Correo electrónico'), 'otro@uagrm.edu.bo');
      await tester.pump();
      expect(find.text('Contraseña actual'), findsOneWidget);
      await tester.enterText(_campo('Correo electrónico'), 'ana@uagrm.edu.bo');
      await tester.pump();

      expect(find.text('Contraseña actual'), findsNothing);
    });

    testWidgets('ERR_US_02 marca el correo (A5) y conserva lo escrito', (tester) async {
      final acciones = FakePerfilAcciones(const AuthError('Ya existe una cuenta con ese correo', campo: CampoAuth.correo));
      await tester.pumpWidget(_app(SingleChildScrollView(child: DatosContactoForm(perfil: _postulante)), acciones: acciones));
      await tester.pumpAndSettle();

      await tester.enterText(_campo('Correo electrónico'), 'otro@uagrm.edu.bo');
      await tester.pump();
      await tester.enterText(_campo('Contraseña actual'), 'secreta1');
      await tester.tap(find.widgetWithText(FilledButton, 'Guardar cambios'));
      await tester.pumpAndSettle();

      expect(find.text('Ya existe una cuenta con ese correo'), findsOneWidget);
      expect(find.text('otro@uagrm.edu.bo'), findsOneWidget);
      expect(find.text('Datos actualizados'), findsNothing);
    });

    testWidgets('ERR_AUTH_09 marca la contraseña actual (A6)', (tester) async {
      final acciones = FakePerfilAcciones(const AuthError('La contraseña actual es incorrecta.', campo: CampoAuth.contrasenaActual));
      await tester.pumpWidget(_app(SingleChildScrollView(child: DatosContactoForm(perfil: _postulante)), acciones: acciones));
      await tester.pumpAndSettle();

      await tester.enterText(_campo('Correo electrónico'), 'otro@uagrm.edu.bo');
      await tester.pump();
      await tester.enterText(_campo('Contraseña actual'), 'mala');
      await tester.tap(find.widgetWithText(FilledButton, 'Guardar cambios'));
      await tester.pumpAndSettle();

      expect(find.text('La contraseña actual es incorrecta.'), findsOneWidget);
    });

    testWidgets('un error de red muestra el aviso con «Reintentar» (A10)', (tester) async {
      final acciones = FakePerfilAcciones(const AuthError('No se pudo conectar con el servidor.', red: true));
      await tester.pumpWidget(_app(SingleChildScrollView(child: DatosContactoForm(perfil: _postulante)), acciones: acciones));
      await tester.pumpAndSettle();

      await tester.enterText(_campo('Teléfono'), '71111111');
      await tester.pump();
      await tester.ensureVisible(find.widgetWithText(FilledButton, 'Guardar cambios'));
      await tester.tap(find.widgetWithText(FilledButton, 'Guardar cambios'));
      await tester.pumpAndSettle();

      expect(find.text('No se pudo conectar con el servidor.'), findsOneWidget);
      acciones.resultado = null;
      await tester.tap(find.text('Reintentar'));
      await tester.pumpAndSettle();
      expect(acciones.actualizaciones, hasLength(2));
    });
  });

  group('CambiarContrasenaForm (flujo E)', () {
    Future<void> llenar(WidgetTester tester, String actual, String nueva, String confirmacion) async {
      await tester.enterText(_campo('Contraseña actual'), actual);
      await tester.enterText(_campo('Contraseña nueva'), nueva);
      await tester.enterText(_campo('Confirmar contraseña nueva'), confirmacion);
    }

    final boton = find.widgetWithText(FilledButton, 'Cambiar contraseña');

    testWidgets('muestra el requisito de la contraseña nueva', (tester) async {
      await tester.pumpWidget(_app(const SingleChildScrollView(child: CambiarContrasenaForm()), acciones: FakePerfilAcciones()));
      await tester.pumpAndSettle();

      expect(find.textContaining('de 6 a 72 caracteres'), findsOneWidget);
    });

    testWidgets('una contraseña nueva corta o sin coincidir muestra el error y no envía (CA07)', (tester) async {
      final acciones = FakePerfilAcciones();
      await tester.pumpWidget(_app(const SingleChildScrollView(child: CambiarContrasenaForm()), acciones: acciones));
      await tester.pumpAndSettle();

      await llenar(tester, 'actual-1', '123', '123');
      await tester.ensureVisible(boton);
      await tester.tap(boton);
      await tester.pump();
      expect(find.text('La contraseña debe tener al menos 6 caracteres'), findsOneWidget);

      await llenar(tester, 'actual-1', 'nueva-123', 'otra-1234');
      await tester.tap(boton);
      await tester.pump();

      expect(find.text('Las contraseñas no coinciden'), findsOneWidget);
      expect(acciones.cambios, isEmpty);
    });

    testWidgets('al cambiar manda las tres contraseñas, avisa y limpia el formulario sin tocar la sesión', (tester) async {
      final acciones = FakePerfilAcciones();
      await tester.pumpWidget(_app(const SingleChildScrollView(child: CambiarContrasenaForm()), acciones: acciones));
      await tester.pumpAndSettle();

      await llenar(tester, 'actual-1', 'nueva-123', 'nueva-123');
      await tester.ensureVisible(boton);
      await tester.tap(boton);
      await tester.pumpAndSettle();

      expect(acciones.cambios.single, ('actual-1', 'nueva-123', 'nueva-123'));
      expect(find.text('Contraseña actualizada. Cerramos tu sesión en los demás dispositivos.'), findsOneWidget);
      for (final etiqueta in ['Contraseña actual', 'Contraseña nueva', 'Confirmar contraseña nueva']) {
        expect(tester.widget<TextFormField>(_campo(etiqueta)).controller!.text, isEmpty);
      }
    });

    testWidgets('ERR_AUTH_09 marca la contraseña actual y conserva lo escrito (A6)', (tester) async {
      final acciones = FakePerfilAcciones(const AuthError('La contraseña actual es incorrecta.', campo: CampoAuth.contrasenaActual));
      await tester.pumpWidget(_app(const SingleChildScrollView(child: CambiarContrasenaForm()), acciones: acciones));
      await tester.pumpAndSettle();

      await llenar(tester, 'mala-1', 'nueva-123', 'nueva-123');
      await tester.ensureVisible(boton);
      await tester.tap(boton);
      await tester.pumpAndSettle();

      expect(find.text('La contraseña actual es incorrecta.'), findsOneWidget);
      expect(tester.widget<TextFormField>(_campo('Contraseña nueva')).controller!.text, 'nueva-123');
    });

    testWidgets('permite mostrar y ocultar la contraseña', (tester) async {
      await tester.pumpWidget(_app(const SingleChildScrollView(child: CambiarContrasenaForm()), acciones: FakePerfilAcciones()));
      await tester.pumpAndSettle();

      final antes = tester.widgetList<EditableText>(find.byType(EditableText)).first.obscureText;
      await tester.tap(find.byTooltip('Mostrar contraseña').first);
      await tester.pump();
      final despues = tester.widgetList<EditableText>(find.byType(EditableText)).first.obscureText;

      expect(antes, isTrue);
      expect(despues, isFalse);
    });
  });

  group('accesos al perfil', () {
    GoRouter router(Widget inicio) => GoRouter(
          initialLocation: '/inicio',
          routes: [
            GoRoute(path: '/inicio', builder: (_, _) => inicio),
            GoRoute(path: '/perfil', builder: (_, _) => const Scaffold(body: Text('pantalla-perfil'))),
          ],
        );

    Widget appCon(GoRouter r, {Usuario? usuario, FakeAcciones? acciones}) => ProviderScope(
          overrides: [
            authSesionProvider.overrideWith(() => FakeAuthSesion(usuario ?? Usuario.fromJson(usuarioJson))),
            if (acciones != null) authProvider.overrideWith(() => acciones),
          ],
          child: MaterialApp.router(theme: AppTheme.light, routerConfig: r),
        );

    testWidgets('«Mi perfil» del menú de cuenta abre /perfil', (tester) async {
      final r = router(Scaffold(appBar: AppBar(actions: const [MenuUsuario()])));
      await tester.pumpWidget(appCon(r, acciones: FakeAcciones()));
      await tester.pumpAndSettle();

      await tester.tap(find.byIcon(Icons.account_circle_outlined));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Mi perfil'));
      await tester.pumpAndSettle();

      expect(find.text('pantalla-perfil'), findsOneWidget);
    });

    testWidgets('«Cerrar sesión» sigue funcionando junto a «Mi perfil»', (tester) async {
      final acciones = FakeAcciones();
      final r = router(Scaffold(appBar: AppBar(actions: const [MenuUsuario()])));
      await tester.pumpWidget(appCon(r, acciones: acciones));
      await tester.pumpAndSettle();

      await tester.tap(find.byIcon(Icons.account_circle_outlined));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Cerrar sesión'));
      await tester.pumpAndSettle();

      expect(acciones.cierres, 1);
    });

    testWidgets('un rol sin módulo (Directivo) llega a su perfil desde «sin módulo»', (tester) async {
      final directivo = Usuario.fromJson({...usuarioJson, 'rol': 'DIRECTIVO'});
      final r = router(const SinModuloScreen());
      await tester.pumpWidget(appCon(r, usuario: directivo, acciones: FakeAcciones()));
      await tester.pumpAndSettle();

      await tester.tap(find.widgetWithText(OutlinedButton, 'Mi perfil'));
      await tester.pumpAndSettle();

      expect(find.text('pantalla-perfil'), findsOneWidget);
    });

    testWidgets('la sección «Perfil» del shell del Estudiante muestra el perfil real', (tester) async {
      final r = router(const EstudianteShell());
      await tester.pumpWidget(ProviderScope(
        overrides: [
          authSesionProvider.overrideWith(() => FakeAuthSesion(Usuario.fromJson(usuarioJson))),
          perfilDetailProvider.overrideWith((ref) async => _postulante),
          perfilProvider.overrideWith(FakePerfilAcciones.new),
        ],
        child: MaterialApp.router(theme: AppTheme.light, routerConfig: r),
      ));
      await tester.pumpAndSettle();

      expect(find.text('Mis datos'), findsNothing);
      await tester.tap(find.text('Perfil'));
      await tester.pumpAndSettle();

      expect(find.text('Mis datos'), findsOneWidget);
      expect(find.text('Datos de contacto'), findsOneWidget);
    });
  });
}
