import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/auth_provider.dart';
import '../providers/auth_usuario_provider.dart';
import '../rol_destino.dart';

/// Pantalla para quien inicia sesión con un rol que no es `POSTULANTE`: la app
/// móvil es solo para postulantes, el resto usa la plataforma web. Solo ofrece
/// cerrar sesión (que lleva al login). Muestra la URL web si `WEB_URL` está en
/// el `.env`.
class SoloPostulantesScreen extends ConsumerWidget {
  const SoloPostulantesScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final usuario = ref.watch(authUsuarioProvider);
    final cerrando = ref.watch(authProvider);
    final tema = Theme.of(context);
    final urlWeb = _urlWeb();

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.desktop_windows_outlined, size: 56, color: tema.colorScheme.primary),
                const SizedBox(height: 16),
                Text(
                  usuario == null ? 'Hola' : 'Hola, ${usuario.nombres}',
                  style: tema.textTheme.headlineSmall,
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 8),
                Text(
                  'Esta app es solo para postulantes.',
                  style: tema.textTheme.titleMedium,
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 8),
                Text(
                  usuario == null
                      ? 'Usa la plataforma web para ingresar.'
                      : 'Tu cuenta es de ${etiquetaRol(usuario.rol)}: usa la plataforma web para ingresar.',
                  style: tema.textTheme.bodyMedium,
                  textAlign: TextAlign.center,
                ),
                if (urlWeb != null) ...[
                  const SizedBox(height: 12),
                  SelectableText(
                    urlWeb,
                    style: tema.textTheme.bodyMedium?.copyWith(color: tema.colorScheme.primary),
                    textAlign: TextAlign.center,
                  ),
                ],
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

  /// `WEB_URL` es opcional, y `dotenv` puede no estar cargado (tests).
  static String? _urlWeb() {
    if (!dotenv.isInitialized) return null;
    final url = dotenv.env['WEB_URL']?.trim();
    return (url == null || url.isEmpty) ? null : url;
  }
}
