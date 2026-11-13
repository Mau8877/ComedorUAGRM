import 'package:comedoru_palette/comedoru_palette.dart';
import 'package:flutter/material.dart';

import '../models/estado_postulacion.dart';

/// El estado visible de una postulación como un chip con un tono del tema (success, warning, info, error, muted):
/// nunca hex sueltos. La app muestra el estado tal cual lo manda el backend, no lo recalcula.
class EstadoPostulacionChip extends StatelessWidget {
  final EstadoPostulacion estado;

  const EstadoPostulacionChip({super.key, required this.estado});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final oscuro = tema.brightness == Brightness.dark;
    final tono = _tono(estado, tema.colorScheme, oscuro);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(color: tono.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(999)),
      child: Text(
        estado.etiqueta,
        style: tema.textTheme.labelMedium?.copyWith(color: tono, fontWeight: FontWeight.w600),
      ),
    );
  }

  static Color _tono(EstadoPostulacion estado, ColorScheme colores, bool oscuro) => switch (estado) {
        EstadoPostulacion.pendiente || EstadoPostulacion.calificada =>
          oscuro ? ComedoruPalette.darkInfo : ComedoruPalette.lightInfo,
        EstadoPostulacion.devuelta => oscuro ? ComedoruPalette.darkWarning : ComedoruPalette.lightWarning,
        EstadoPostulacion.adjudicada => oscuro ? ComedoruPalette.darkSuccess : ComedoruPalette.lightSuccess,
        EstadoPostulacion.noAdjudicada => colores.error,
        EstadoPostulacion.archivada => colores.onSurface.withValues(alpha: 0.6),
      };
}
