/// El bloque «Eres finalista» (`FinalistaPostulanteResponse`, CU21). [plazoEntrega] y [documentosFisicos] son
/// `null` si todavía no hubo aviso de finalista.
class FinalistaPostulante {
  /// Día límite para entregar la documentación física (sin hora).
  final DateTime? plazoEntrega;
  final bool documentacionRecibida;
  final String? documentosFisicos;

  const FinalistaPostulante({
    required this.plazoEntrega,
    required this.documentacionRecibida,
    required this.documentosFisicos,
  });

  factory FinalistaPostulante.fromJson(Map<String, dynamic> json) {
    final plazo = json['plazoEntrega'];
    return FinalistaPostulante(
      plazoEntrega: plazo == null ? null : DateTime.parse(plazo as String),
      documentacionRecibida: json['documentacionRecibida'] as bool,
      documentosFisicos: json['documentosFisicos'] as String?,
    );
  }
}
