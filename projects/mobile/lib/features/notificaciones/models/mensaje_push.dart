/// Aviso push que llega con la app abierta (CU-EXTRA-01).
///
/// * `codigoNotificacion` y `evento` salen de `data` del mensaje; sirven para enlazar con la
/// * bandeja cuando exista. `postulacionCodigo` viene solo en los avisos de una postulación (CU04): tocar el
/// * aviso abre el detalle de esa postulación; sin él, tocarlo solo abre la app.
class MensajePush {
  final String titulo;
  final String cuerpo;
  final String? codigoNotificacion;
  final String? evento;

  /// Código público de la postulación a la que apunta el aviso, si la tiene.
  final String? postulacionCodigo;

  const MensajePush({
    required this.titulo,
    required this.cuerpo,
    this.codigoNotificacion,
    this.evento,
    this.postulacionCodigo,
  });
}
