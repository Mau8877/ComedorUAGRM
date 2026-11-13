import 'package:comedoru_palette/comedoru_palette.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../enlaces_cu02.dart';
import '../formatos.dart';
import '../models/estado_postulacion.dart';
import '../models/finalista_postulante.dart';
import '../models/observaciones.dart';

/// Tarjeta con un título y un contenido, del tono pedido (el fondo es el tono con transparencia).
class _Bloque extends StatelessWidget {
  final String titulo;
  final Color? tono;
  final List<Widget> hijos;

  const _Bloque({required this.titulo, required this.hijos, this.tono});

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(top: 16),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: tono?.withValues(alpha: 0.12) ?? Theme.of(context).colorScheme.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: (tono ?? Theme.of(context).dividerColor).withValues(alpha: 0.35)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(titulo, style: texto.titleMedium?.copyWith(color: tono)),
          const SizedBox(height: 8),
          ...hijos,
        ],
      ),
    );
  }
}

/// Las observaciones de la devolución (CU19) y su fecha. «Corregir y reenviar» solo aparece si el backend dice que
/// se puede corregir **y** existe la pantalla del CU-02; mientras no exista se oculta.
class BloqueObservaciones extends StatelessWidget {
  final Observaciones observaciones;
  final bool puedeCorregir;

  const BloqueObservaciones({super.key, required this.observaciones, required this.puedeCorregir});

  @override
  Widget build(BuildContext context) {
    final oscuro = Theme.of(context).brightness == Brightness.dark;
    return _Bloque(
      titulo: 'Observaciones de la DUBSS',
      tono: oscuro ? ComedoruPalette.darkWarning : ComedoruPalette.lightWarning,
      hijos: [
        Text(observaciones.texto),
        const SizedBox(height: 4),
        Text(formatearFechaHora(observaciones.fecha), style: Theme.of(context).textTheme.bodySmall),
        if (puedeCorregir && rutaCorregir != null) ...[
          const SizedBox(height: 8),
          FilledButton(onPressed: () => context.push(rutaCorregir!), child: const Text('Corregir y reenviar')),
        ],
      ],
    );
  }
}

/// El motivo del archivo o del rechazo de la postulación (CU20). Nunca el de la decisión final del CU05.
class BloqueMotivo extends StatelessWidget {
  final String motivo;
  final EstadoPostulacion estado;

  const BloqueMotivo({super.key, required this.motivo, required this.estado});

  @override
  Widget build(BuildContext context) {
    return _Bloque(
      titulo: estado == EstadoPostulacion.archivada ? 'Motivo del archivo' : 'Motivo del rechazo',
      hijos: [Text(motivo)],
    );
  }
}

/// El bloque «Eres finalista» (CU21): el plazo para entregar la documentación física, los documentos a presentar
/// (del último aviso) y si la documentación ya fue recibida.
class BloqueFinalista extends StatelessWidget {
  final FinalistaPostulante finalista;

  const BloqueFinalista({super.key, required this.finalista});

  @override
  Widget build(BuildContext context) {
    final oscuro = Theme.of(context).brightness == Brightness.dark;
    final etiqueta = Theme.of(context).textTheme.bodySmall;
    return _Bloque(
      titulo: 'Eres finalista',
      tono: oscuro ? ComedoruPalette.darkInfo : ComedoruPalette.lightInfo,
      hijos: [
        Text('Plazo de entrega', style: etiqueta),
        Text(finalista.plazoEntrega == null ? 'Todavía sin plazo' : formatearDia(finalista.plazoEntrega!)),
        const SizedBox(height: 8),
        Text('Documentos a presentar', style: etiqueta),
        Text(finalista.documentosFisicos ?? 'La DUBSS aún no los indicó'),
        const SizedBox(height: 8),
        Text('Documentación', style: etiqueta),
        Text(finalista.documentacionRecibida ? 'Recibida' : 'Pendiente de entrega'),
      ],
    );
  }
}

/// El resultado de la postulación una vez publicados los resultados (CU22): adjudicada o no adjudicada, sin motivo
/// (el de la decisión final lo ve solo la DUBSS). El botón «Ver resultados de la convocatoria» se oculta mientras
/// no exista esa pantalla en la app.
class BloqueResultado extends StatelessWidget {
  final String resultado;

  const BloqueResultado({super.key, required this.resultado});

  @override
  Widget build(BuildContext context) {
    final adjudicada = resultado == 'ADJUDICADA';
    final oscuro = Theme.of(context).brightness == Brightness.dark;
    return _Bloque(
      titulo: adjudicada ? 'Adjudicada' : 'No adjudicada',
      tono: adjudicada ? (oscuro ? ComedoruPalette.darkSuccess : ComedoruPalette.lightSuccess) : null,
      hijos: [
        Text(adjudicada
            ? '¡Felicidades! Obtuviste la Beca Comedor con esta postulación.'
            : 'Tu postulación no fue adjudicada. Gracias por participar.'),
      ],
    );
  }
}
