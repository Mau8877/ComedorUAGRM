import 'package:flutter/material.dart';

import '../models/ingrediente.dart';

/// Form de crear/editar -- `Form` + `TextFormField` + `validator` nativo de
/// Flutter (ver FORMULARIOS_MOBILE.md, sin librerías externas). Mismas
/// reglas de negocio que `ingredienteSchema.ts` del frontend (mismos
/// mensajes en español, ver FORMULARIOS_MOBILE.md#consistencia-de-reglas-de-negocio-con-el-frontend-web).
class IngredienteFormSheet extends StatefulWidget {
  final Ingrediente? ingrediente;
  final ValueChanged<Ingrediente> onSubmit;

  const IngredienteFormSheet({super.key, this.ingrediente, required this.onSubmit});

  @override
  State<IngredienteFormSheet> createState() => _IngredienteFormSheetState();
}

class _IngredienteFormSheetState extends State<IngredienteFormSheet> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _nombreController;
  late final TextEditingController _fotoController;
  late final TextEditingController _stockController;
  late final TextEditingController _stockMinimoController;
  late final TextEditingController _precioController;
  late String _categoria;
  late String _unidad;
  bool _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    final ingrediente = widget.ingrediente;
    _nombreController = TextEditingController(text: ingrediente?.nombre ?? '');
    _fotoController = TextEditingController(text: ingrediente?.foto ?? '');
    _stockController = TextEditingController(text: ingrediente?.stock.toString() ?? '');
    _stockMinimoController = TextEditingController(text: ingrediente?.stockMinimo.toString() ?? '');
    _precioController = TextEditingController(text: ingrediente?.precioUnitario.toString() ?? '');
    _categoria = ingrediente?.categoria ?? categoriasIngrediente.first;
    _unidad = ingrediente?.unidad ?? unidadesIngrediente.first;
  }

  @override
  void dispose() {
    _nombreController.dispose();
    _fotoController.dispose();
    _stockController.dispose();
    _stockMinimoController.dispose();
    _precioController.dispose();
    super.dispose();
  }

  String? _numeroNoNegativo(String? valor, String mensaje) {
    if (valor == null || valor.isEmpty) return mensaje;
    final numero = num.tryParse(valor);
    if (numero == null || numero < 0) return mensaje;
    return null;
  }

  void _handleSubmit() {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isSubmitting = true);
    // Simula la latencia de una mutation real -- acá no hay backend, solo
    // el timeout (mismo criterio que PruebaIngredientes.tsx del frontend).
    Future.delayed(const Duration(milliseconds: 800), () {
      if (!mounted) return;
      final base =
          widget.ingrediente ??
          Ingrediente(
            id: 'ing-${DateTime.now().millisecondsSinceEpoch}',
            nombre: '',
            foto: '',
            categoria: _categoria,
            unidad: _unidad,
            stock: 0,
            stockMinimo: 0,
            precioUnitario: 0,
          );
      widget.onSubmit(
        base.copyWith(
          nombre: _nombreController.text,
          foto: _fotoController.text,
          categoria: _categoria,
          unidad: _unidad,
          stock: int.parse(_stockController.text),
          stockMinimo: int.parse(_stockMinimoController.text),
          precioUnitario: double.parse(_precioController.text),
        ),
      );
      Navigator.of(context).pop();
    });
  }

  @override
  Widget build(BuildContext context) {
    final esEdicion = widget.ingrediente != null;

    return Padding(
      padding: EdgeInsets.only(
        left: 20,
        right: 20,
        top: 20,
        bottom: MediaQuery.of(context).viewInsets.bottom + 20,
      ),
      child: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              esEdicion ? 'Editar ingrediente' : 'Nuevo ingrediente',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _nombreController,
              decoration: const InputDecoration(labelText: 'Nombre'),
              validator: (valor) =>
                  (valor == null || valor.isEmpty) ? 'El nombre es obligatorio' : null,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _fotoController,
              decoration: const InputDecoration(labelText: 'Foto (URL)'),
              validator: (valor) {
                if (valor == null || valor.isEmpty) return 'La foto es obligatoria';
                if (Uri.tryParse(valor)?.isAbsolute != true) return 'Debe ser una URL válida';
                return null;
              },
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              initialValue: _categoria,
              decoration: const InputDecoration(labelText: 'Categoría'),
              items: [
                for (final categoria in categoriasIngrediente)
                  DropdownMenuItem(value: categoria, child: Text(categoria)),
              ],
              onChanged: (valor) => setState(() => _categoria = valor!),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              initialValue: _unidad,
              decoration: const InputDecoration(labelText: 'Unidad'),
              items: [
                for (final unidad in unidadesIngrediente)
                  DropdownMenuItem(value: unidad, child: Text(unidad)),
              ],
              onChanged: (valor) => setState(() => _unidad = valor!),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: TextFormField(
                    controller: _stockController,
                    decoration: const InputDecoration(labelText: 'Stock'),
                    keyboardType: TextInputType.number,
                    validator: (valor) => _numeroNoNegativo(valor, 'El stock no puede ser negativo'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: TextFormField(
                    controller: _stockMinimoController,
                    decoration: const InputDecoration(labelText: 'Stock mínimo'),
                    keyboardType: TextInputType.number,
                    validator: (valor) =>
                        _numeroNoNegativo(valor, 'El stock mínimo no puede ser negativo'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _precioController,
              decoration: const InputDecoration(labelText: 'Precio unitario (Bs)'),
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              validator: (valor) {
                if (valor == null || valor.isEmpty) return 'El precio es obligatorio';
                final numero = num.tryParse(valor);
                if (numero == null || numero <= 0) return 'El precio debe ser mayor a 0';
                return null;
              },
            ),
            const SizedBox(height: 20),
            FilledButton(
              onPressed: _isSubmitting ? null : _handleSubmit,
              child: _isSubmitting
                  ? const SizedBox(
                      height: 16,
                      width: 16,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : Text(esEdicion ? 'Guardar cambios' : 'Crear ingrediente'),
            ),
          ],
        ),
      ),
    );
  }
}
