import 'archivo_local.dart';
import 'documentos_postulacion.dart';

/// De dónde salió el archivo que se está revisando: define qué hace «Repetir» (volver a tomar la foto, elegir otra
/// imagen o elegir otro PDF).
enum FuenteDocumento { camara, galeria, pdf }

/// Lo que la pantalla «Revisar y adjuntar» (CU16) necesita para subir un archivo: se pasa como `extra` de la ruta.
class RevisionDocumento {
  /// El código público de la postulación.
  final String postulacion;
  final DocumentoRequerido requerido;
  final ReglasArchivo reglas;
  final FuenteDocumento fuente;
  final ArchivoLocal archivo;

  /// El archivo que se reemplaza; sin él, se agrega uno nuevo al documento.
  final ArchivoAdjunto? reemplaza;

  const RevisionDocumento({
    required this.postulacion,
    required this.requerido,
    required this.reglas,
    required this.fuente,
    required this.archivo,
    this.reemplaza,
  });
}
