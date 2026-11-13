import 'estado_postulacion.dart';

/// Un código público con su nombre: la convocatoria o la gestión académica de una postulación.
class CodigoNombre {
  final String codigo;
  final String nombre;

  const CodigoNombre({required this.codigo, required this.nombre});

  factory CodigoNombre.fromJson(Map<String, dynamic> json) =>
      CodigoNombre(codigo: json['codigo'] as String, nombre: json['nombre'] as String);
}

/// Una postulación enviada en «Mis postulaciones» (`MiPostulacionResumenResponse`, CU04). Solo códigos
/// públicos: nunca ids internos. [estado] es el visible, tal cual lo manda el backend.
class MiPostulacionResumen {
  final String codigo;
  final CodigoNombre convocatoria;
  final CodigoNombre gestionAcademica;
  final DateTime fechaEnvio;
  final EstadoPostulacion estado;
  final int numeroEnvios;

  const MiPostulacionResumen({
    required this.codigo,
    required this.convocatoria,
    required this.gestionAcademica,
    required this.fechaEnvio,
    required this.estado,
    required this.numeroEnvios,
  });

  factory MiPostulacionResumen.fromJson(Map<String, dynamic> json) => MiPostulacionResumen(
        codigo: json['codigo'] as String,
        convocatoria: CodigoNombre.fromJson(json['convocatoria'] as Map<String, dynamic>),
        gestionAcademica: CodigoNombre.fromJson(json['gestionAcademica'] as Map<String, dynamic>),
        fechaEnvio: DateTime.parse(json['fechaEnvio'] as String),
        estado: EstadoPostulacion.deValor(json['estado'] as String),
        numeroEnvios: (json['numeroEnvios'] as num).toInt(),
      );
}
