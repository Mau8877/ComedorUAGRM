import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../enlaces_cu02.dart';
import '../formatos.dart';
import '../models/borrador_postulacion.dart';

/// «Mi borrador» (CU-17): la postulación a medias de la convocatoria abierta, con la fecha del último guardado y lo
/// que falta para poder enviarla. «Continuar» abre `/postular`, que recupera el borrador con sus respuestas.
class TarjetaBorrador extends StatelessWidget {
  final BorradorResumen borrador;

  const TarjetaBorrador({super.key, required this.borrador});

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    final guardadoEn = borrador.guardadoEn;
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Expanded(child: Text(borrador.convocatoria, style: texto.titleMedium)),
                const Chip(label: Text('Borrador')),
              ],
            ),
            const SizedBox(height: 4),
            Text(
              guardadoEn == null
                  ? 'Todavía no guardaste ningún dato.'
                  : 'Última modificación: ${formatearFechaHora(guardadoEn)}',
              style: texto.bodySmall,
            ),
            const SizedBox(height: 8),
            Text(borrador.resumenAvance),
            const SizedBox(height: 8),
            Text(
              'Tu borrador se conserva hasta el cierre de la convocatoria (${formatearFecha(borrador.fechaCierre)}). '
              'Un borrador no cuenta como postulación enviada.',
              style: texto.bodySmall,
            ),
            const SizedBox(height: 12),
            FilledButton(onPressed: () => context.push(rutaPostular), child: const Text('Continuar')),
          ],
        ),
      ),
    );
  }
}
