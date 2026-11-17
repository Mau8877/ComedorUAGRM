/// El becado encontrado por CI (CU10): lo que el Personal del Comedor ve para confirmar la identidad y el estado
/// de la beca. [horaRegistro] solo viene si la asistencia de hoy ya se registró.
class BecadoAsistencia {
  final String becaCodigo;
  final String nombres;
  final String apellidos;
  final String ci;
  final String carrera;
  final String convocatoria;
  final String estadoBeca;
  final DateTime? horaRegistro;

  const BecadoAsistencia({
    required this.becaCodigo,
    required this.nombres,
    required this.apellidos,
    required this.ci,
    required this.carrera,
    required this.convocatoria,
    required this.estadoBeca,
    this.horaRegistro,
  });

  factory BecadoAsistencia.fromJson(Map<String, dynamic> json) {
    final hora = json['horaRegistro'] as String?;
    return BecadoAsistencia(
      becaCodigo: json['becaCodigo'] as String,
      nombres: json['nombres'] as String,
      apellidos: json['apellidos'] as String,
      ci: json['ci'] as String,
      carrera: json['carrera'] as String,
      convocatoria: json['convocatoria'] as String,
      estadoBeca: json['estadoBeca'] as String,
      horaRegistro: hora == null ? null : DateTime.parse(hora),
    );
  }

  String get nombreCompleto => '$nombres $apellidos';

  /// Solo una beca vigente registra asistencia.
  bool get vigente => estadoBeca == 'VIGENTE';

  /// Ya hay asistencia de hoy: no se registra otra (A3).
  bool get yaRegistroHoy => horaRegistro != null;

  /// Copia con la hora del registro de hoy (después de registrar o de descubrir que ya existía).
  BecadoAsistencia conHora(DateTime hora) => BecadoAsistencia(
        becaCodigo: becaCodigo,
        nombres: nombres,
        apellidos: apellidos,
        ci: ci,
        carrera: carrera,
        convocatoria: convocatoria,
        estadoBeca: estadoBeca,
        horaRegistro: hora,
      );
}
