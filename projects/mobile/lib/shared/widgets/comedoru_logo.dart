import 'package:flutter/material.dart';

/// Marca de ComedorU (cubiertos y plato en blanco, U en rojo) (de `packages/comedoru-icons`), para el AppBar
/// de marca (`header`, fijo en azul marino en claro y oscuro).
class ComedoruLogo extends StatelessWidget {
  const ComedoruLogo({super.key, this.size = 32});

  final double size;

  @override
  Widget build(BuildContext context) {
    return Image.asset(
      'assets/images/comedoru-mark-white.png',
      width: size,
      height: size,
      semanticLabel: 'ComedorU',
    );
  }
}
