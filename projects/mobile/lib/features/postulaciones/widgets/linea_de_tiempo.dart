import 'package:comedoru_palette/comedoru_palette.dart';
import 'package:flutter/material.dart';

import '../formatos.dart';
import '../models/paso_linea_tiempo.dart';

/// La línea de tiempo de una postulación (CU04): los pasos que ya ocurrieron, con su fecha y hora, resaltados, y
/// los que faltan, atenuados. Sale del historial real que arma el backend; no muestra quién hizo cada cambio.
class LineaDeTiempo extends StatelessWidget {
  final List<PasoLineaTiempo> pasos;

  const LineaDeTiempo({super.key, required this.pasos});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final exito = tema.brightness == Brightness.dark ? ComedoruPalette.darkSuccess : ComedoruPalette.lightSuccess;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        for (final paso in pasos)
          Opacity(
            key: ValueKey('paso-${paso.estado}-${paso.situacion.name}'),
            opacity: paso.situacion == SituacionPaso.completado ? 1 : 0.55,
            child: Padding(
              padding: const EdgeInsets.only(bottom: 14),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(
                    paso.situacion == SituacionPaso.completado ? Icons.check_circle : Icons.radio_button_unchecked,
                    size: 22,
                    color: paso.situacion == SituacionPaso.completado ? exito : tema.colorScheme.onSurface,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(paso.etiqueta, style: tema.textTheme.titleSmall),
                        Text(
                          paso.fecha == null ? 'Pendiente' : formatearFechaHora(paso.fecha!),
                          style: tema.textTheme.bodySmall,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
      ],
    );
  }
}
