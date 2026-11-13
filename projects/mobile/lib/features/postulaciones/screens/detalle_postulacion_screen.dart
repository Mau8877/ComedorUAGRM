import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../bloques_detalle.dart';
import '../formatos.dart';
import '../models/mi_postulacion_detalle.dart';
import '../postulaciones_error_mapper.dart';
import '../providers/mis_postulaciones_provider.dart';
import '../widgets/alerta_postulaciones.dart';
import '../widgets/bloques_informacion.dart';
import '../widgets/estado_postulacion_chip.dart';
import '../widgets/linea_de_tiempo.dart';

/// El detalle de una postulación (CU04): su estado visible (tal cual lo manda el backend), la línea de tiempo con
/// los pasos que ocurrieron y los que faltan, y solo los bloques que corresponden al estado. Una postulación
/// inexistente o ajena se muestra como «no encontrada».
class DetallePostulacionScreen extends ConsumerWidget {
  final String codigo;

  const DetallePostulacionScreen({super.key, required this.codigo});

  void _volver(BuildContext context) {
    if (context.canPop()) {
      context.pop();
    } else {
      context.go('/estudiante');
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final detalle = ref.watch(postulacionDetailProvider(codigo));

    return Scaffold(
      appBar: AppBar(
        title: const Text('Detalle de postulación'),
        leading: BackButton(onPressed: () => _volver(context)),
      ),
      body: detalle.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) {
          final error = mapearErrorPostulaciones(e);
          return Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: error.noEncontrada
                  ? Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text('Postulación no encontrada', style: Theme.of(context).textTheme.titleMedium),
                        const SizedBox(height: 8),
                        Text(error.mensaje, textAlign: TextAlign.center),
                        const SizedBox(height: 16),
                        OutlinedButton(
                          onPressed: () => context.go('/estudiante'),
                          child: const Text('Ir a mis postulaciones'),
                        ),
                      ],
                    )
                  : AlertaPostulaciones(
                      error: error,
                      onReintentar: () => ref.invalidate(postulacionDetailProvider(codigo)),
                    ),
            ),
          );
        },
        data: (datos) => _Contenido(detalle: datos),
      ),
    );
  }
}

class _Contenido extends StatelessWidget {
  final MiPostulacionDetalle detalle;

  const _Contenido({required this.detalle});

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    final resumen = detalle.resumen;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(resumen.convocatoria.nombre, style: texto.titleLarge),
                    const SizedBox(height: 4),
                    Text(
                      'Gestión ${resumen.gestionAcademica.nombre} · Enviada el ${formatearFecha(resumen.fechaEnvio)}',
                      style: texto.bodySmall,
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              EstadoPostulacionChip(estado: resumen.estado),
            ],
          ),
          const SizedBox(height: 24),
          Text('Recorrido de tu postulación', style: texto.titleMedium),
          const SizedBox(height: 12),
          LineaDeTiempo(pasos: detalle.lineaDeTiempo),
          for (final bloque in bloquesDelDetalle(detalle))
            switch (bloque) {
              BloqueDetalle.observaciones =>
                BloqueObservaciones(observaciones: detalle.observaciones!, puedeCorregir: detalle.puedeCorregir),
              BloqueDetalle.motivo => BloqueMotivo(motivo: detalle.motivo!, estado: resumen.estado),
              BloqueDetalle.finalista => BloqueFinalista(finalista: detalle.finalista!),
              BloqueDetalle.resultado => BloqueResultado(resultado: detalle.resultado!),
            },
        ],
      ),
    );
  }
}
