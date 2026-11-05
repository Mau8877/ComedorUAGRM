import 'package:flutter/material.dart';

/// Dato del perfil que se muestra pero no se edita: campo bloqueado con su etiqueta y, opcionalmente,
/// una aclaración (ej. por qué el CI no se puede modificar).
class CampoSoloLectura extends StatelessWidget {
  final String label;
  final String valor;
  final String? nota;

  const CampoSoloLectura({super.key, required this.label, required this.valor, this.nota});

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      // La llave por valor hace que el campo tome el dato nuevo si el perfil se vuelve a cargar.
      key: ValueKey('$label:$valor'),
      initialValue: valor,
      enabled: false,
      decoration: InputDecoration(labelText: label, helperText: nota),
    );
  }
}
