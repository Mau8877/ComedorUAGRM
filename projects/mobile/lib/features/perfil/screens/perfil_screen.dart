import 'package:flutter/material.dart';

import '../widgets/perfil_vista.dart';

/// «Mi perfil» como pantalla completa (CU15), para cualquier rol. Se abre desde «Mi perfil» del menú
/// de cuenta; la tecla atrás vuelve al módulo.
class PerfilScreen extends StatelessWidget {
  const PerfilScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Mi perfil')),
      body: const SafeArea(child: PerfilVista()),
    );
  }
}
