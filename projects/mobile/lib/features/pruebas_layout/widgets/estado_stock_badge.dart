import 'package:comedoru_palette/comedoru_palette.dart';
import 'package:flutter/material.dart';

import '../models/ingrediente.dart';

/// Chip de estado -- mismo criterio visual que `StatusBadge` del frontend
/// (color de fondo tenue + texto sólido según el estado derivado del stock).
class EstadoStockBadge extends StatelessWidget {
  final EstadoStock estado;

  const EstadoStockBadge({super.key, required this.estado});

  @override
  Widget build(BuildContext context) {
    final (label, color) = switch (estado) {
      EstadoStock.disponible => ('Disponible', ComedoruPalette.lightSuccess),
      EstadoStock.bajoStock => ('Bajo stock', ComedoruPalette.lightWarning),
      EstadoStock.agotado => ('Agotado', ComedoruPalette.lightDestructive),
    };

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: TextStyle(color: color, fontSize: 12, fontWeight: FontWeight.w600),
      ),
    );
  }
}
