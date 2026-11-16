import 'package:flutter/material.dart';

/// Opción de un [SelectorBuscable].
class OpcionSelector {
  final String valor;
  final String etiqueta;

  const OpcionSelector(this.valor, this.etiqueta);
}

/// Desde cuántas opciones (más de) el selector ofrece buscador.
const opcionesParaBuscador = 8;

/// Altura máxima del desplegable (≈ 18 rem de la web).
const alturaMaximaDesplegable = 288.0;

/// Select de formulario. Con [opcionesParaBuscador] opciones o menos es un desplegable de altura
/// máxima [alturaMaximaDesplegable] con scroll; con más, abre una hoja con un buscador que filtra
/// la lista. Mismo criterio que el `Select` de la web.
class SelectorBuscable extends StatelessWidget {
  final String label;
  final List<OpcionSelector> opciones;
  final String? valor;
  final String? errorText;
  final String? Function(String?)? validator;
  final ValueChanged<String?> onChanged;

  const SelectorBuscable({
    super.key,
    required this.label,
    required this.opciones,
    required this.valor,
    required this.onChanged,
    this.validator,
    this.errorText,
  });

  @override
  Widget build(BuildContext context) {
    if (opciones.length <= opcionesParaBuscador) {
      return DropdownButtonFormField<String>(
        isExpanded: true,
        initialValue: valor,
        menuMaxHeight: alturaMaximaDesplegable,
        autovalidateMode: AutovalidateMode.onUnfocus,
        decoration: InputDecoration(labelText: label, errorText: errorText),
        items: [
          for (final o in opciones)
            DropdownMenuItem<String>(
              value: o.valor,
              child: Text(o.etiqueta, overflow: TextOverflow.ellipsis),
            ),
        ],
        validator: validator,
        onChanged: onChanged,
      );
    }

    return FormField<String>(
      key: ValueKey('$label-$valor'),
      initialValue: valor,
      validator: validator,
      builder: (campo) {
        final etiqueta = opciones
            .where((o) => o.valor == valor)
            .map((o) => o.etiqueta)
            .firstOrNull;
        return InkWell(
          onTap: () async {
            final elegido = await showModalBottomSheet<String>(
              context: context,
              isScrollControlled: true,
              builder: (_) => _HojaBuscador(
                titulo: label,
                opciones: opciones,
                valor: valor,
              ),
            );
            if (elegido != null) {
              campo.didChange(elegido);
              onChanged(elegido);
            }
            // Cerrar la hoja cuenta como salir del campo: se valida.
            campo.validate();
          },
          child: InputDecorator(
            isEmpty: etiqueta == null,
            decoration: InputDecoration(
              labelText: label,
              errorText: campo.errorText ?? errorText,
              suffixIcon: const Icon(Icons.arrow_drop_down),
            ),
            child: Text(etiqueta ?? '', overflow: TextOverflow.ellipsis),
          ),
        );
      },
    );
  }
}

class _HojaBuscador extends StatefulWidget {
  final String titulo;
  final List<OpcionSelector> opciones;
  final String? valor;

  const _HojaBuscador({
    required this.titulo,
    required this.opciones,
    required this.valor,
  });

  @override
  State<_HojaBuscador> createState() => _HojaBuscadorState();
}

class _HojaBuscadorState extends State<_HojaBuscador> {
  String _busqueda = '';

  @override
  Widget build(BuildContext context) {
    final filtro = _busqueda.trim().toLowerCase();
    final visibles = widget.opciones
        .where((o) => o.etiqueta.toLowerCase().contains(filtro))
        .toList();
    final teclado = MediaQuery.of(context).viewInsets.bottom;

    return Padding(
      padding: EdgeInsets.only(bottom: teclado),
      child: SizedBox(
        height: MediaQuery.of(context).size.height * 0.6,
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.all(16),
              child: TextField(
                autofocus: true,
                decoration: InputDecoration(
                  prefixIcon: const Icon(Icons.search),
                  hintText: 'Buscar ${widget.titulo.toLowerCase()}…',
                ),
                onChanged: (v) => setState(() => _busqueda = v),
              ),
            ),
            Expanded(
              child: visibles.isEmpty
                  ? const Center(child: Text('Sin resultados'))
                  : ListView.builder(
                      itemCount: visibles.length,
                      itemBuilder: (_, i) {
                        final o = visibles[i];
                        return ListTile(
                          title: Text(o.etiqueta),
                          selected: o.valor == widget.valor,
                          onTap: () => Navigator.of(context).pop(o.valor),
                        );
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }
}
