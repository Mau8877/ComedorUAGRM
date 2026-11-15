/// «5 MB», «1.2 MB», «800 KB»: el tamaño de un archivo para mostrarlo (los adjuntos y los límites de las reglas).
String formatearTamano(int bytes) {
  if (bytes >= 1024 * 1024) {
    final mb = bytes / (1024 * 1024);
    return '${mb == mb.truncateToDouble() ? mb.toInt() : mb.toStringAsFixed(1)} MB';
  }
  final kb = (bytes / 1024).round();
  return '${kb < 1 ? 1 : kb} KB';
}

const _nombres = {
  'application/pdf': 'PDF',
  'image/jpeg': 'JPG',
  'image/png': 'PNG',
};

/// «PDF, JPG o PNG»: los formatos permitidos para los mensajes.
String nombresDeFormatos(List<String> formatos) {
  final nombres = ([...formatos]..sort()).map((f) => _nombres[f] ?? f).toList();
  if (nombres.length <= 1) return nombres.join();
  return '${nombres.sublist(0, nombres.length - 1).join(', ')} o ${nombres.last}';
}

/// El motivo por el que un archivo no cumple las reglas, o `null` si las cumple. Es solo feedback inmediato: el backend
/// vuelve a validar por el contenido del archivo y es el que decide.
String? validarArchivoLocal({required String mimeType, required int tamanoBytes, required int maximoBytes, required List<String> formatos}) {
  if (tamanoBytes <= 0) return 'El archivo está vacío';
  if (tamanoBytes > maximoBytes) {
    return 'El archivo supera el tamaño máximo de ${formatearTamano(maximoBytes)}';
  }
  if (!formatos.contains(mimeType)) {
    return 'Formato no permitido. Se aceptan: ${nombresDeFormatos(formatos)}';
  }
  return null;
}
