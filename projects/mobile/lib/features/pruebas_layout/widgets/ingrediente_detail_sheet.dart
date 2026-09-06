import 'package:flutter/material.dart';

import '../models/ingrediente.dart';
import 'estado_stock_badge.dart';

/// Detalle -- equivalente mobile de `IngredienteDetailModal.tsx`: los
/// campos que solo se muestran acá (no en la lista ni en el form).
class IngredienteDetailSheet extends StatelessWidget {
  final Ingrediente ingrediente;
  final VoidCallback onEditar;

  const IngredienteDetailSheet({super.key, required this.ingrediente, required this.onEditar});

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;

    return Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              ClipRRect(
                borderRadius: BorderRadius.circular(999),
                child: Image.network(
                  ingrediente.foto,
                  width: 56,
                  height: 56,
                  fit: BoxFit.cover,
                  errorBuilder: (context, error, stackTrace) => const CircleAvatar(radius: 28),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(ingrediente.nombre, style: textTheme.headlineSmall),
                    Text(ingrediente.categoria, style: textTheme.bodySmall),
                  ],
                ),
              ),
              EstadoStockBadge(estado: ingrediente.estadoStock),
            ],
          ),
          const SizedBox(height: 16),
          _DetailRow(label: 'Stock', value: '${ingrediente.stock} ${ingrediente.unidad}'),
          _DetailRow(
            label: 'Stock mínimo',
            value: '${ingrediente.stockMinimo} ${ingrediente.unidad}',
          ),
          _DetailRow(
            label: 'Precio unitario',
            value: 'Bs ${ingrediente.precioUnitario.toStringAsFixed(2)}',
          ),
          if (ingrediente.proveedor != null)
            _DetailRow(label: 'Proveedor', value: ingrediente.proveedor!),
          if (ingrediente.ubicacionAlmacen != null)
            _DetailRow(label: 'Ubicación', value: ingrediente.ubicacionAlmacen!),
          if (ingrediente.fechaIngreso != null)
            _DetailRow(label: 'Fecha de ingreso', value: ingrediente.fechaIngreso!),
          if (ingrediente.fechaVencimiento != null)
            _DetailRow(label: 'Fecha de vencimiento', value: ingrediente.fechaVencimiento!),
          if (ingrediente.descripcion != null) ...[
            const SizedBox(height: 12),
            Text(ingrediente.descripcion!, style: textTheme.bodyMedium),
          ],
          const SizedBox(height: 20),
          FilledButton.tonal(
            onPressed: () {
              Navigator.of(context).pop();
              onEditar();
            },
            child: const Text('Editar'),
          ),
        ],
      ),
    );
  }
}

class _DetailRow extends StatelessWidget {
  final String label;
  final String value;

  const _DetailRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: Theme.of(context).textTheme.bodySmall),
          Text(value, style: Theme.of(context).textTheme.bodyMedium),
        ],
      ),
    );
  }
}
