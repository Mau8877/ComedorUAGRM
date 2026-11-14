import 'package:flutter/material.dart';

import '../../../shared/widgets/comedoru_logo.dart';

/// Pantalla de espera mientras se restaura la sesión al abrir la app: evita que
/// una pantalla privada (o el login) parpadee antes de saber si hay sesión.
class CargandoScreen extends StatelessWidget {
  const CargandoScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ComedoruLogo(size: 72),
            SizedBox(height: 24),
            CircularProgressIndicator(),
          ],
        ),
      ),
    );
  }
}
