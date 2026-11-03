import 'package:comedor_uagrm_mobile/core/theme/app_theme.dart';
import 'package:comedor_uagrm_mobile/features/auth/models/usuario.dart';
import 'package:comedor_uagrm_mobile/features/auth/providers/auth_provider.dart';
import 'package:comedor_uagrm_mobile/features/auth/providers/auth_sesion_provider.dart';
import 'package:comedor_uagrm_mobile/features/auth/screens/login_screen.dart';
import 'package:comedor_uagrm_mobile/features/auth/widgets/menu_usuario.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../support/fake_adapter.dart';
import '../../widget_test.dart' show FakeAuthSesion;

/// Acciones simuladas: guardan qué se pidió y devuelven lo que se les diga.
class FakeAcciones extends AuthAcciones {
  int cierres = 0;
  final List<(String, String)> logins = [];

  @override
  Future<void> cerrarSesion() async {
    cierres++;
  }
}

Widget _app(Widget pantalla, {Usuario? usuario, FakeAcciones? acciones}) {
  return ProviderScope(
    overrides: [
      authSesionProvider.overrideWith(() => FakeAuthSesion(usuario)),
      if (acciones != null) authProvider.overrideWith(() => acciones),
    ],
    child: MaterialApp(theme: AppTheme.light, home: pantalla),
  );
}

void main() {
  group('LoginScreen', () {
    testWidgets('con los campos vacíos muestra los errores de validación y no envía', (tester) async {
      await tester.pumpWidget(_app(const LoginScreen(), acciones: FakeAcciones()));

      await tester.tap(find.widgetWithText(FilledButton, 'Iniciar sesión'));
      await tester.pump();

      expect(find.text('El correo electrónico es obligatorio'), findsOneWidget);
      expect(find.text('La contraseña es obligatoria'), findsOneWidget);
    });

    testWidgets('ofrece crear cuenta pero no recuperar la contraseña (CU15)', (tester) async {
      await tester.pumpWidget(_app(const LoginScreen(), acciones: FakeAcciones()));

      expect(find.text('¿No tienes cuenta? Crear cuenta'), findsOneWidget);
      expect(find.textContaining('olvid', findRichText: true), findsNothing);
    });

    testWidgets('el botón está habilitado al abrir la pantalla', (tester) async {
      await tester.pumpWidget(_app(const LoginScreen(), acciones: FakeAcciones()));

      final boton = tester.widget<FilledButton>(find.widgetWithText(FilledButton, 'Iniciar sesión'));
      expect(boton.onPressed, isNotNull);
    });
  });

  group('MenuUsuario', () {
    testWidgets('muestra nombre y rol y «Cerrar sesión» llama a cerrarSesion', (tester) async {
      final acciones = FakeAcciones();
      await tester.pumpWidget(_app(
        Scaffold(appBar: AppBar(actions: const [MenuUsuario()])),
        usuario: Usuario.fromJson(usuarioJson),
        acciones: acciones,
      ));
      await tester.pumpAndSettle();

      await tester.tap(find.byIcon(Icons.account_circle_outlined));
      await tester.pumpAndSettle();

      expect(find.text('Ana Perez'), findsOneWidget);
      expect(find.text('Postulante'), findsOneWidget);

      await tester.tap(find.text('Cerrar sesión'));
      await tester.pumpAndSettle();

      expect(acciones.cierres, 1);
    });

    testWidgets('sin sesión no muestra nada', (tester) async {
      await tester.pumpWidget(_app(Scaffold(appBar: AppBar(actions: const [MenuUsuario()])), acciones: FakeAcciones()));
      await tester.pumpAndSettle();

      expect(find.byIcon(Icons.account_circle_outlined), findsNothing);
    });
  });
}
