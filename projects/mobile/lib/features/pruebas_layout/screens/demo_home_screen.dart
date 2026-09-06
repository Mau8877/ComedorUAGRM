import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

/// Pantalla de entrada temporal -- equivalente mobile del `index.tsx` del
/// frontend (ver ARQUITECTURA_FRONTEND.md), solo para navegar a los shells
/// de cada rol mientras no exista login real. Se reemplaza por la primera
/// feature real detrás del login, igual que `_authenticated/panel.tsx` del
/// lado web.
class DemoHomeScreen extends StatelessWidget {
  const DemoHomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('ComedorUAGRM')),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text('Rutas', style: Theme.of(context).textTheme.headlineSmall),
              const SizedBox(height: 16),
              FilledButton(
                onPressed: () => context.go('/admin'),
                child: const Text('Layout Admin'),
              ),
              const SizedBox(height: 12),
              FilledButton(
                onPressed: () => context.go('/estudiante'),
                child: const Text('Layout Estudiante'),
              ),
              const SizedBox(height: 12),
              FilledButton(
                onPressed: () => context.go('/ingredientes'),
                child: const Text('Ingredientes (mock)'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
