import 'finalista_postulante.dart';
import 'mi_postulacion_resumen.dart';
import 'observaciones.dart';
import 'paso_linea_tiempo.dart';

/// El detalle de una postulación propia (`MiPostulacionDetalleResponse`, CU04): el [resumen], la línea de tiempo
/// y la información asociada al estado visible. Los bloques que no corresponden llegan `null`: [observaciones]
/// (solo «devuelta»), [motivo] (solo archivo o rechazo del CU20), [finalista] (solo si lo es) y [resultado]
/// (`ADJUDICADA` o `NO_ADJUDICADA`, solo con los resultados publicados).
class MiPostulacionDetalle {
  final MiPostulacionResumen resumen;
  final List<PasoLineaTiempo> lineaDeTiempo;
  final Observaciones? observaciones;
  final String? motivo;
  final FinalistaPostulante? finalista;
  final String? resultado;
  final bool puedeCorregir;

  const MiPostulacionDetalle({
    required this.resumen,
    required this.lineaDeTiempo,
    required this.observaciones,
    required this.motivo,
    required this.finalista,
    required this.resultado,
    required this.puedeCorregir,
  });

  factory MiPostulacionDetalle.fromJson(Map<String, dynamic> json) {
    final observaciones = json['observaciones'];
    final finalista = json['finalista'];
    return MiPostulacionDetalle(
      resumen: MiPostulacionResumen.fromJson(json),
      lineaDeTiempo: (json['lineaDeTiempo'] as List<dynamic>)
          .map((paso) => PasoLineaTiempo.fromJson(paso as Map<String, dynamic>))
          .toList(),
      observaciones: observaciones == null ? null : Observaciones.fromJson(observaciones as Map<String, dynamic>),
      motivo: json['motivo'] as String?,
      finalista: finalista == null ? null : FinalistaPostulante.fromJson(finalista as Map<String, dynamic>),
      resultado: json['resultado'] as String?,
      puedeCorregir: json['puedeCorregir'] as bool,
    );
  }
}
