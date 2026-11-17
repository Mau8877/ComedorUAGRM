import 'package:comedor_uagrm_mobile/core/theme/app_theme.dart';
import 'package:comedor_uagrm_mobile/features/auth/models/usuario.dart';
import 'package:comedor_uagrm_mobile/features/auth/providers/auth_provider.dart';
import 'package:comedor_uagrm_mobile/features/auth/providers/auth_sesion_provider.dart';
import 'package:comedor_uagrm_mobile/features/auth/screens/solo_postulantes_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../support/fake_adapter.dart';
import '../../widget_test.dart' show FakeAuthSesion;

class _FakeAcciones extends AuthAcciones {
  int cierres = 0;

  @override
  Future<void> cerrarSesion() async => cierres++;
}

void main() {
  testWidgets('un rol que no es postulante ve que la app es solo para postulantes y puede cerrar sesión',
      (tester) async {
    final admin = Usuario.fromJson({...usuarioJson, 'rol': 'ADMINISTRADOR_DUBSS'});
    final acciones = _FakeAcciones();
    await tester.pumpWidget(ProviderScope(
      overrides: [
        authSesionProvider.overrideWith(() => FakeAuthSesion(admin)),
        authProvider.overrideWith(() => acciones),
      ],
      child: MaterialApp(theme: AppTheme.light, home: const SoloPostulantesScreen()),
    ));
    await tester.pumpAndSettle();

    expect(find.text('Esta app es solo para postulantes.'), findsOneWidget);
    expect(find.textContaining('plataforma web'), findsOneWidget);

    await tester.tap(find.widgetWithText(FilledButton, 'Cerrar sesión'));
    await tester.pumpAndSettle();

    expect(acciones.cierres, 1);
  });
}
