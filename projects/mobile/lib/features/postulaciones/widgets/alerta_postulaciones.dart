import 'package:flutter/material.dart';

import '../postulaciones_error_mapper.dart';

/// Alerta para los errores de «Mis postulaciones». Con [onReintentar] ofrece reintentar cuando es un error de
/// conexión o del servidor (A10).
class AlertaPostulaciones extends StatelessWidget {
  final ErrorPostulaciones error;
  final VoidCallback? onReintentar;

  const AlertaPostulaciones({super.key, required this.error, this.onReintentar});

  @override
  Widget build(BuildContext context) {
    final colores = Theme.of(context).colorScheme;

    return Semantics(
      liveRegion: true,
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(color: colores.errorContainer, borderRadius: BorderRadius.circular(12)),
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
