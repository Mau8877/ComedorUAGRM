import 'package:flutter/material.dart';

import '../auth_error_mapper.dart';

/// Alerta sobre el formulario para los errores que no son de un campo
/// concreto (credenciales, cuenta bloqueada o desactivada, red). Con
/// [onReintentar] ofrece reintentar (errores de conexión, A9).
class AlertaError extends StatelessWidget {
  final AuthError error;
  final VoidCallback? onReintentar;

  const AlertaError({super.key, required this.error, this.onReintentar});

  @override
  Widget build(BuildContext context) {
    final colores = Theme.of(context).colorScheme;

    return Semantics(
      liveRegion: true,
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: colores.errorContainer,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(Icons.warning_amber_rounded, color: colores.onErrorContainer),
            const SizedBox(width: 8),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(error.mensaje, style: TextStyle(color: colores.onErrorContainer)),
                  if (error.red && onReintentar != null)
                    TextButton(onPressed: onReintentar, child: const Text('Reintentar')),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
