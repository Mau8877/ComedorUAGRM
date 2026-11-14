import 'package:flutter/material.dart';

/// Campo de contraseña con la opción de mostrarla u ocultarla. Los errores del servidor (ej. contraseña
/// actual incorrecta) llegan por [errorText] y desaparecen cuando la persona edita el campo.
/// ! Lo escrito no se guarda fuera del controlador ni se imprime.
class CampoContrasena extends StatefulWidget {
  final String label;
  final TextEditingController controller;
  final String? Function(String?)? validator;
  final String? errorText;
  final Iterable<String> autofillHints;
  final TextInputAction? textInputAction;
  final ValueChanged<String>? onChanged;
  final ValueChanged<String>? onFieldSubmitted;

  const CampoContrasena({
    super.key,
    required this.label,
    required this.controller,
    this.validator,
    this.errorText,
    this.autofillHints = const [AutofillHints.password],
    this.textInputAction,
    this.onChanged,
    this.onFieldSubmitted,
  });

  @override
  State<CampoContrasena> createState() => _CampoContrasenaState();
}

class _CampoContrasenaState extends State<CampoContrasena> {
  bool _oculta = true;

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: widget.controller,
      obscureText: _oculta,
      autofillHints: widget.autofillHints,
      textInputAction: widget.textInputAction,
      validator: widget.validator,
      onChanged: widget.onChanged,
      onFieldSubmitted: widget.onFieldSubmitted,
      decoration: InputDecoration(
        labelText: widget.label,
        errorText: widget.errorText,
        suffixIcon: IconButton(
          tooltip: _oculta ? 'Mostrar contraseña' : 'Ocultar contraseña',
          icon: Icon(_oculta ? Icons.visibility_outlined : Icons.visibility_off_outlined),
          onPressed: () => setState(() => _oculta = !_oculta),
        ),
      ),
    );
  }
}
