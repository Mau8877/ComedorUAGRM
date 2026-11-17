/// El resultado de registrar la asistencia (CU10). [yaRegistrada] es `true` si ese día ya existía un registro: no se
/// creó otro y [horaRegistro] es la hora del primero (idempotencia).
class AsistenciaRegistrada {
  final String codigo;
  final String becaCodigo;
  final DateTime horaRegistro;
  final bool yaRegistrada;

  const AsistenciaRegistrada({
    required this.codigo,
    required this.becaCodigo,
    required this.horaRegistro,
    required this.yaRegistrada,
  });

  factory AsistenciaRegistrada.fromJson(Map<String, dynamic> json) => AsistenciaRegistrada(
        codigo: json['codigo'] as String,
        becaCodigo: json['becaCodigo'] as String,
        horaRegistro: DateTime.parse(json['horaRegistro'] as String),
        yaRegistrada: json['yaRegistrada'] as bool,
      );
}
