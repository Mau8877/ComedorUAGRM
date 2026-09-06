import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:comedor_uagrm_mobile/core/theme/app_theme.dart';
import 'package:comedor_uagrm_mobile/layouts/estudiante/estudiante_shell.dart';

// * No se testea `MyApp` completo -- `main()` depende de `dotenv.load()`
// * (lee un archivo real), que no tiene sentido resolver en un widget test.
// * Se arma acá un router mínimo standalone para verificar que el shell de
// * un rol renderiza bien montado en un MaterialApp.router real.
void main() {
  testWidgets('muestra la sección inicial del shell de estudiante', (tester) async {
    final router = GoRouter(
      initialLocation: '/estudiante',
      routes: [
        GoRoute(path: '/estudiante', builder: (context, state) => const EstudianteShell()),
      ],
    );

    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp.router(theme: AppTheme.light, routerConfig: router),
      ),
    );

    expect(find.text('Mi Menú'), findsWidgets);
    expect(find.byIcon(Icons.person_outline), findsOneWidget);
  });
}
