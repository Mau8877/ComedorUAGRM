/// Si un paso de la línea de tiempo ya ocurrió o todavía falta.
enum SituacionPaso {
  completado,
  pendiente;

  static SituacionPaso deValor(String valor) => switch (valor) {
        'COMPLETADO' => SituacionPaso.completado,
        'PENDIENTE' => SituacionPaso.pendiente,
        _ => throw FormatException('Situación de paso desconocida: $valor'),
      };
}

/// Un paso de la línea de tiempo (`PasoLineaTiempoResponse`). Los completados salen del historial real, con su
/// fecha; los pendientes no tienen fecha. [estado] es un estado de la postulación o `resultado` (el paso final).
/// Nunca trae quién hizo el cambio.
class PasoLineaTiempo {
  final String estado;
  final DateTime? fecha;
  final SituacionPaso situacion;

  const PasoLineaTiempo({required this.estado, required this.fecha, required this.situacion});

  factory PasoLineaTiempo.fromJson(Map<String, dynamic> json) {
    final fecha = json['fecha'];
    return PasoLineaTiempo(
      estado: json['estado'] as String,
      fecha: fecha == null ? null : DateTime.parse(fecha as String),
      situacion: SituacionPaso.deValor(json['situacion'] as String),
    );
  }

  /// El texto del paso: el mismo estado cambia de sentido si ya ocurrió o si todavía falta (el reenvío).
  String get etiqueta {
    if (situacion == SituacionPaso.pendiente && estado == 'pendiente') {
      return 'Reenvío de la postulación corregida';
    }
    return switch (estado) {
      'pendiente' => 'Postulación enviada',
      'calificada' => 'Calificada',
      'devuelta' => 'Devuelta para corrección',
      'archivada' => 'Archivada',
      'adjudicada' => 'Adjudicada',
      'no_adjudicada' => 'No adjudicada',
      'resultado' => 'Resultado de la adjudicación',
      _ => estado,
    };
  }
}
