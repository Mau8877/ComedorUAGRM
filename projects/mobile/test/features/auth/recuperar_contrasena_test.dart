import 'package:comedor_uagrm_mobile/core/theme/app_theme.dart';
import 'package:comedor_uagrm_mobile/features/auth/auth_error_mapper.dart';
import 'package:comedor_uagrm_mobile/features/auth/providers/auth_provider.dart';
import 'package:comedor_uagrm_mobile/features/auth/providers/auth_sesion_provider.dart';
import 'package:comedor_uagrm_mobile/features/auth/screens/recuperar_contrasena_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import '../../widget_test.dart' show FakeAuthSesion;

/// Acciones simuladas: guardan los correos pedidos y devuelven el resultado que se les diga.
class FakeRecuperacion extends AuthAcciones {
  final List<String> correos = [];
  AuthError? resultado;

  FakeRecuperacion([this.resultado]);

  @override
  Future<AuthError?> solicitarRecuperacion(String correo) async {
    correos.add(correo);
    return resultado;
  }
}

Widget _app(FakeRecuperacion acciones) {
  final router = GoRouter(
    initialLocation: '/recuperar-contrasena',
    routes: [
      GoRoute(path: '/recuperar-contrasena', builder: (_, _) => const RecuperarContrasenaScreen()),
      GoRoute(path: '/login', builder: (_, _) => const Scaffold(body: Text('pantalla-login'))),
    ],
  );
  return ProviderScope(
    overrides: [
      authSesionProvider.overrideWith(() => FakeAuthSesion(null)),
      authProvider.overrideWith(() => acciones),
    ],
    child: MaterialApp.router(theme: AppTheme.light, routerConfig: router),
  );
}

void main() {
  group('RecuperarContrasenaScreen', () {
    testWidgets('con el correo vacío o mal formado muestra el error y no llama a la API', (tester) async {
      final acciones = FakeRecuperacion();
      await tester.pumpWidget(_app(acciones));
      await tester.pumpAndSettle();

      await tester.tap(find.widgetWithText(FilledButton, 'Enviar enlace'));
      await tester.pump();
      expect(find.text('El correo electrónico es obligatorio'), findsOneWidget);

      await tester.enterText(find.byType(TextFormField), 'no-es-correo');
      await tester.tap(find.widgetWithText(FilledButton, 'Enviar enlace'));
      await tester.pump();

      expect(find.text('El correo electrónico no es válido'), findsOneWidget);
      expect(acciones.correos, isEmpty);
    });

    testWidgets('con un correo válido pide el enlace y muestra siempre el mismo mensaje neutro', (tester) async {
      final acciones = FakeRecuperacion();
      await tester.pumpWidget(_app(acciones));
      await tester.pumpAndSettle();

      await tester.enterText(find.byType(TextFormField), 'ana@uagrm.edu.bo');
      await tester.tap(find.widgetWithText(FilledButton, 'Enviar enlace'));
      await tester.pumpAndSettle();

      expect(acciones.correos, ['ana@uagrm.edu.bo']);
      expect(find.text(mensajeRecuperacionEnviada), findsOneWidget);
      // Ni el correo ni ninguna pista de si la cuenta existe; y el formulario ya no está.
      expect(find.textContaining('ana@uagrm'), findsNothing);
      expect(find.byType(TextFormField), findsNothing);
    });

    testWidgets('el mensaje avisa que el enlace se abre en el navegador', (tester) async {
      expect(mensajeRecuperacionEnviada, contains('navegador'));
      expect(mensajeRecuperacionEnviada, contains('60 minutos'));
    });

    testWidgets('«Enviar de nuevo» vuelve al formulario vacío (A11)', (tester) async {
      await tester.pumpWidget(_app(FakeRecuperacion()));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextFormField), 'ana@uagrm.edu.bo');
      await tester.tap(find.widgetWithText(FilledButton, 'Enviar enlace'));
      await tester.pumpAndSettle();

      await tester.tap(find.widgetWithText(OutlinedButton, 'Enviar de nuevo'));
      await tester.pumpAndSettle();

      expect(find.byType(TextFormField), findsOneWidget);
      expect(find.text(mensajeRecuperacionEnviada), findsNothing);
      expect(tester.widget<TextFormField>(find.byType(TextFormField)).controller!.text, isEmpty);
    });

    testWidgets('un error de red muestra el aviso con «Reintentar» y no el mensaje neutro (A9, A10)', (tester) async {
      final acciones = FakeRecuperacion(const AuthError('No se pudo conectar con el servidor.', red: true));
      await tester.pumpWidget(_app(acciones));
      await tester.pumpAndSettle();

      await tester.enterText(find.byType(TextFormField), 'ana@uagrm.edu.bo');
      await tester.tap(find.widgetWithText(FilledButton, 'Enviar enlace'));
      await tester.pumpAndSettle();

      expect(find.text('No se pudo conectar con el servidor.'), findsOneWidget);
      expect(find.text(mensajeRecuperacionEnviada), findsNothing);

      acciones.resultado = null;
      await tester.tap(find.text('Reintentar'));
      await tester.pumpAndSettle();

      expect(acciones.correos, ['ana@uagrm.edu.bo', 'ana@uagrm.edu.bo']);
      expect(find.text(mensajeRecuperacionEnviada), findsOneWidget);
    });

    testWidgets('«Volver a iniciar sesión» lleva al login', (tester) async {
      await tester.pumpWidget(_app(FakeRecuperacion()));
      await tester.pumpAndSettle();

      await tester.tap(find.widgetWithText(TextButton, 'Volver a iniciar sesión'));
      await tester.pumpAndSettle();

      expect(find.text('pantalla-login'), findsOneWidget);
    });
  });
}
