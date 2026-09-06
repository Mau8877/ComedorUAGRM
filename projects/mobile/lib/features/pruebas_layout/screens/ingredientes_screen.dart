import 'package:flutter/material.dart';

import '../mocks/ingredientes_mock.dart';
import '../models/ingrediente.dart';
import '../widgets/estado_stock_badge.dart';
import '../widgets/ingrediente_detail_sheet.dart';
import '../widgets/ingrediente_form_sheet.dart';

/// Pantalla de prueba `/ingredientes` -- equivalente mobile de
/// `PruebaIngredientes.tsx`: búsqueda + filtro por categoría, lista con
/// foto/estado derivado del stock, y crear/editar/eliminar mockeados (solo
/// mutan la lista local, no hay backend detrás). Sirve para verificar cómo
/// se ve y se siente el flujo completo antes de que exista la feature real.
class IngredientesScreen extends StatefulWidget {
  const IngredientesScreen({super.key});

  @override
  State<IngredientesScreen> createState() => _IngredientesScreenState();
}

class _IngredientesScreenState extends State<IngredientesScreen> {
  late List<Ingrediente> _ingredientes;
  String _busqueda = '';
  String? _categoriaFiltro;

  @override
  void initState() {
    super.initState();
    _ingredientes = List.of(mockIngredientes);
  }

  List<Ingrediente> get _filtrados {
    return _ingredientes.where((ingrediente) {
      final coincideBusqueda =
          _busqueda.isEmpty ||
          ingrediente.nombre.toLowerCase().contains(_busqueda.toLowerCase());
      final coincideCategoria =
          _categoriaFiltro == null || ingrediente.categoria == _categoriaFiltro;
      return coincideBusqueda && coincideCategoria;
    }).toList();
  }

  void _abrirForm({Ingrediente? ingrediente}) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (context) => IngredienteFormSheet(
        ingrediente: ingrediente,
        onSubmit: (nuevo) {
          setState(() {
            final indice = _ingredientes.indexWhere(
              (item) => item.id == nuevo.id,
            );
            if (indice >= 0) {
              _ingredientes[indice] = nuevo;
            } else {
              _ingredientes.insert(0, nuevo);
            }
          });
        },
      ),
    );
  }

  void _abrirDetalle(Ingrediente ingrediente) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (context) => IngredienteDetailSheet(
        ingrediente: ingrediente,
        onEditar: () => _abrirForm(ingrediente: ingrediente),
      ),
    );
  }

  void _confirmarEliminar(Ingrediente ingrediente) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('¿Eliminar ingrediente?'),
        content: Text(
          'Se eliminará permanentemente "${ingrediente.nombre}" del sistema.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: Theme.of(context).colorScheme.error,
            ),
            onPressed: () {
              setState(
                () => _ingredientes.removeWhere(
                  (item) => item.id == ingrediente.id,
                ),
              );
              Navigator.of(context).pop();
            },
            child: const Text('Eliminar'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final ingredientes = _filtrados;

    return Scaffold(
      appBar: AppBar(title: const Text('Ingredientes')),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _abrirForm(),
        child: const Icon(Icons.add),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: TextField(
              decoration: const InputDecoration(
                hintText: 'Buscar ingrediente...',
                prefixIcon: Icon(Icons.search),
                isDense: true,
              ),
              onChanged: (valor) => setState(() => _busqueda = valor),
            ),
          ),
          SizedBox(
            height: 40,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 16),
              children: [
                Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: ChoiceChip(
                    label: const Text('Todas'),
                    selected: _categoriaFiltro == null,
                    onSelected: (_) => setState(() => _categoriaFiltro = null),
                  ),
                ),
                for (final categoria in categoriasIngrediente)
                  Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: ChoiceChip(
                      label: Text(categoria),
                      selected: _categoriaFiltro == categoria,
                      onSelected: (_) =>
                          setState(() => _categoriaFiltro = categoria),
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 8),
          Expanded(
            child: ingredientes.isEmpty
                ? const Center(child: Text('Sin resultados'))
                : ListView.separated(
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 88),
                    itemCount: ingredientes.length,
                    separatorBuilder: (context, index) =>
                        const SizedBox(height: 8),
                    itemBuilder: (context, index) {
                      final ingrediente = ingredientes[index];
                      return Card(
                        margin: EdgeInsets.zero,
                        child: ListTile(
                          onTap: () => _abrirDetalle(ingrediente),
                          leading: ClipRRect(
                            borderRadius: BorderRadius.circular(999),
                            child: Image.network(
                              ingrediente.foto,
                              width: 40,
                              height: 40,
                              fit: BoxFit.cover,
                              errorBuilder: (context, error, stackTrace) =>
                                  const CircleAvatar(radius: 20),
                            ),
                          ),
                          title: Text(ingrediente.nombre),
                          subtitle: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                '${ingrediente.categoria} · ${ingrediente.stock} ${ingrediente.unidad} · Bs ${ingrediente.precioUnitario.toStringAsFixed(2)}',
                              ),
                              const SizedBox(height: 4),
                              EstadoStockBadge(estado: ingrediente.estadoStock),
                            ],
                          ),
                          trailing: SizedBox(
                            width: 40,
                            child: PopupMenuButton<String>(
                              padding: EdgeInsets.zero,
                              onSelected: (accion) {
                                if (accion == 'editar') {
                                  _abrirForm(ingrediente: ingrediente);
                                }
                                if (accion == 'eliminar') {
                                  _confirmarEliminar(ingrediente);
                                }
                              },
                              itemBuilder: (context) => [
                                const PopupMenuItem(
                                  value: 'editar',
                                  child: ListTile(
                                    leading: Icon(Icons.edit_outlined),
                                    title: Text('Editar'),
                                    contentPadding: EdgeInsets.zero,
                                  ),
                                ),
                                PopupMenuItem(
                                  value: 'eliminar',
                                  child: ListTile(
                                    leading: Icon(
                                      Icons.delete_outline,
                                      color: Theme.of(
                                        context,
                                      ).colorScheme.error,
                                    ),
                                    title: const Text('Eliminar'),
                                    contentPadding: EdgeInsets.zero,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }
}
