import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// `TextFormField` que muestra su error solo al salir del campo (perder el foco) o al enviar el
/// formulario, nunca mientras se escribe por primera vez; una vez mostrado, se revalida al editar
/// para que desaparezca apenas el valor es válido. Mismo criterio que los formularios de la web.
class CampoFormulario extends StatefulWidget {
  final TextEditingController? controller;
  final InputDecoration decoration;
  final String? Function(String?)? validator;
  final TextInputType? keyboardType;
  final TextInputAction? textInputAction;
  final TextCapitalization textCapitalization;
  final bool obscureText;
  final Iterable<String>? autofillHints;
  final List<TextInputFormatter>? inputFormatters;
  final ValueChanged<String>? onChanged;
  final ValueChanged<String>? onFieldSubmitted;
  final int maxLines;

  const CampoFormulario({
    super.key,
    this.controller,
    this.decoration = const InputDecoration(),
    this.validator,
    this.keyboardType,
    this.textInputAction,
    this.textCapitalization = TextCapitalization.none,
    this.obscureText = false,
    this.autofillHints,
    this.inputFormatters,
    this.onChanged,
    this.onFieldSubmitted,
    this.maxLines = 1,
  });

  @override
  State<CampoFormulario> createState() => _CampoFormularioState();
}

class _CampoFormularioState extends State<CampoFormulario> {
  final _clave = GlobalKey<FormFieldState<String>>();

  void _alCambiar(String valor) {
    final estado = _clave.currentState;
    if (estado != null && estado.hasError) estado.validate();
    widget.onChanged?.call(valor);
  }

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      key: _clave,
      controller: widget.controller,
      decoration: widget.decoration,
      validator: widget.validator,
      keyboardType: widget.keyboardType,
      textInputAction: widget.textInputAction,
      textCapitalization: widget.textCapitalization,
      obscureText: widget.obscureText,
      maxLines: widget.obscureText ? 1 : widget.maxLines,
      autofillHints: widget.autofillHints,
      inputFormatters: widget.inputFormatters,
      autovalidateMode: AutovalidateMode.onUnfocus,
      onChanged: _alCambiar,
      onFieldSubmitted: widget.onFieldSubmitted,
    );
  }
}
