/// Aviso push que llega con la app abierta (CU-EXTRA-01).
///
/// * `codigoNotificacion` y `evento` salen de `data` del mensaje; sirven para enlazar con la
/// * bandeja cuando exista (hoy tocar el aviso solo abre la app).
class MensajePush {
  final String titulo;
  final String cuerpo;
  final String? codigoNotificacion;
  final String? evento;

  const MensajePush({
    required this.titulo,
    required this.cuerpo,
    this.codigoNotificacion,
    this.evento,
  });
}
