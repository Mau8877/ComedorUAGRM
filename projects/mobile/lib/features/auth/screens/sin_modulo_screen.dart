import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/auth_provider.dart';
import '../providers/auth_usuario_provider.dart';
import '../rol_destino.dart';

/// Aviso para los roles que todavía no tienen módulo en la app (`DIRECTIVO` y
/// `PERSONAL_COMEDOR`, ver `rutaInicioPorRol`), con la opción de cerrar sesión.
class SinModuloScreen extends ConsumerWidget {
  const SinModuloScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final usuario = ref.watch(authUsuarioProvider);
    final cerrando = ref.watch(authProvider);
    final tema = Theme.of(context);

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  usuario == null ? 'Hola' : 'Hola, ${usuario.nombres}',
                  style: tema.textTheme.headlineSmall,
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 8),
                Text(
                  usuario == null
                      ? 'Tu módulo aún no está disponible.'
                      : 'Tu módulo (${etiquetaRol(usuario.rol)}) aún no está disponible.',
                  style: tema.textTheme.bodyMedium,
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 24),
                FilledButton(
                  onPressed: cerrando ? null : () => ref.read(authProvider.notifier).cerrarSesion(),
                  child: const Text('Cerrar sesión'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
