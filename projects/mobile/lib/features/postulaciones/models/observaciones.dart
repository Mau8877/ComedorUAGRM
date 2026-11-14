/// Las observaciones de una devolución (CU19) y cuándo se hicieron (`ObservacionesResponse`).
class Observaciones {
  final String texto;
  final DateTime fecha;

  const Observaciones({required this.texto, required this.fecha});

  factory Observaciones.fromJson(Map<String, dynamic> json) =>
      Observaciones(texto: json['texto'] as String, fecha: DateTime.parse(json['fecha'] as String));
}
