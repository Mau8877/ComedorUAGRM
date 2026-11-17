/// La beca del usuario (CU24): su estado, la convocatoria y gestión, el total de asistencias y, si está suspendida o
/// dada de baja, el motivo. Solo lectura; el backend no manda ids internos.
class MiBeca {
  final String codigo;

  /// `VIGENTE`, `SUSPENDIDA` o `BAJA`, tal como lo manda el backend.
  final String estado;
  final String convocatoria;
  final String gestion;

  /// `null` en una beca de reemplazo (no tiene adjudicación propia).
  final DateTime? fechaAdjudicacion;
  final DateTime fechaInicio;
  final String? motivo;
  final int totalAsistencias;

  const MiBeca({
    required this.codigo,
    required this.estado,
    required this.convocatoria,
    required this.gestion,
    required this.fechaInicio,
    required this.totalAsistencias,
    this.fechaAdjudicacion,
    this.motivo,
  });

  factory MiBeca.fromJson(Map<String, dynamic> json) {
    final adjudicacion = json['fechaAdjudicacion'] as String?;
    return MiBeca(
      codigo: json['codigo'] as String,
      estado: json['estado'] as String,
      convocatoria: json['convocatoria'] as String,
      gestion: json['gestion'] as String,
      fechaAdjudicacion: adjudicacion == null ? null : DateTime.parse(adjudicacion),
      fechaInicio: DateTime.parse(json['fechaInicio'] as String),
      motivo: json['motivo'] as String?,
      totalAsistencias: (json['totalAsistencias'] as num).toInt(),
    );
  }

  bool get vigente => estado == 'VIGENTE';
}
