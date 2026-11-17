/// Un archivo que el postulante capturó con la cámara o eligió de la galería o de sus archivos, todavía sin subir (CU16).
///
/// * [ruta] es el archivo temporal en el teléfono: nunca se muestra al usuario ni se manda al backend.
class ArchivoLocal {
  final String ruta;
  final String nombre;
  final String mimeType;
  final int tamanoBytes;

  const ArchivoLocal({
    required this.ruta,
    required this.nombre,
    required this.mimeType,
    required this.tamanoBytes,
  });

  bool get esImagen => mimeType.startsWith('image/');

  ArchivoLocal copyWith({String? ruta, String? nombre, String? mimeType, int? tamanoBytes}) => ArchivoLocal(
        ruta: ruta ?? this.ruta,
        nombre: nombre ?? this.nombre,
        mimeType: mimeType ?? this.mimeType,
        tamanoBytes: tamanoBytes ?? this.tamanoBytes,
      );

  /// El tipo MIME según la extensión del nombre. Es solo lo que se **declara**: el backend decide por el contenido.
  static String mimePorNombre(String nombre) {
    final punto = nombre.lastIndexOf('.');
    final extension = punto < 0 ? '' : nombre.substring(punto + 1).toLowerCase();
    return switch (extension) {
      'pdf' => 'application/pdf',
      'jpg' || 'jpeg' => 'image/jpeg',
      'png' => 'image/png',
      _ => 'application/octet-stream',
    };
  }
}
