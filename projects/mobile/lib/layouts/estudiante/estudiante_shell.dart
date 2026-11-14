import 'package:flutter/material.dart';

import '../../features/auth/widgets/menu_usuario.dart';
import '../../features/perfil/widgets/perfil_vista.dart';
import '../../features/postulaciones/widgets/mis_postulaciones_vista.dart';
import '../../shared/widgets/comedoru_logo.dart';

/// Shell del rol Estudiante -- AppBar + `NavigationBar` (Material 3) inferior,
/// cuatro secciones. Equivalente mobile de `EstudianteLayout` en el frontend
/// web (ver ARQUITECTURA_FRONTEND.md#layouts-por-rol): una barra inferior es
/// el patrón mobile estándar para navegación de nivel superior con pocas
/// secciones, a diferencia del sidebar que usa el rol Admin.
class EstudianteShell extends StatefulWidget {
  const EstudianteShell({super.key});

  @override
  State<EstudianteShell> createState() => _EstudianteShellState();
}

class _EstudianteSeccion {
  final String label;
  final IconData icon;

  const _EstudianteSeccion(this.label, this.icon);
}

const _secciones = [
  _EstudianteSeccion('Mi Menú', Icons.restaurant_outlined),
  _EstudianteSeccion('Mis Pedidos', Icons.list_alt_outlined),
  _EstudianteSeccion('Mis postulaciones', Icons.assignment_outlined),
  _EstudianteSeccion('Perfil', Icons.person_outline),
];

/// Posición de «Mis postulaciones» (CU04) en [_secciones].
const _indicePostulaciones = 2;

/// Posición de «Perfil» en [_secciones].
const _indicePerfil = 3;

class _EstudianteShellState extends State<EstudianteShell> {
  int _selected = 0;

  @override
  Widget build(BuildContext context) {
    final seccionActual = _secciones[_selected];

    return Scaffold(
      appBar: AppBar(title: const Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            ComedoruLogo(size: 32),
            SizedBox(width: 10),
            Text('ComedorU'),
          ],
        ),
        actions: const [MenuUsuario()],
      ),
      // La sección «Perfil» (CU15) muestra el perfil real; las demás siguen de prueba hasta que exista su feature.
      body: _selected == _indicePerfil
          ? const PerfilVista()
          : _selected == _indicePostulaciones
              ? const MisPostulacionesVista()
              : Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(seccionActual.label, style: Theme.of(context).textTheme.headlineSmall),
                  const SizedBox(height: 8),
                  Text(
                    'Pantalla de prueba del layout -- acá va el contenido real de "${seccionActual.label}" cuando exista esa feature.',
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
                ],
              ),
            ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selected,
        onDestinationSelected: (index) => setState(() => _selected = index),
        destinations: [
          for (final seccion in _secciones)
            NavigationDestination(icon: Icon(seccion.icon), label: seccion.label),
        ],
      ),
    );
  }
}
