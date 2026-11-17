// Contrato del backend de CU16 (`DocumentosPostulacionResponse` y compañía). Solo códigos públicos: nunca ids
// internos ni la ruta de almacenamiento.

/// Un archivo ya adjuntado a un documento requerido.
class ArchivoAdjunto {
  final String codigo;
  final String nombreArchivo;
  final String mimeType;
  final int tamanoBytes;
  final DateTime subidoEn;

  const ArchivoAdjunto({
    required this.codigo,
    required this.nombreArchivo,
    required this.mimeType,
    required this.tamanoBytes,
    required this.subidoEn,
  });

  factory ArchivoAdjunto.fromJson(Map<String, dynamic> json) => ArchivoAdjunto(
        codigo: json['codigo'] as String,
        nombreArchivo: json['nombreArchivo'] as String,
        mimeType: json['mimeType'] as String,
        tamanoBytes: (json['tamanoBytes'] as num).toInt(),
        subidoEn: DateTime.parse(json['subidoEn'] as String),
      );
}

/// Un documento que exige la convocatoria, con su estado y los archivos que lleva.
class DocumentoRequerido {
  final String tipoDocumentoCodigo;
  final String nombre;
  final String? descripcion;
  final bool obligatorio;
  final bool adjuntado;
  final List<ArchivoAdjunto> archivos;

  const DocumentoRequerido({
    required this.tipoDocumentoCodigo,
    required this.nombre,
    required this.descripcion,
    required this.obligatorio,
    required this.adjuntado,
    required this.archivos,
  });

  factory DocumentoRequerido.fromJson(Map<String, dynamic> json) => DocumentoRequerido(
        tipoDocumentoCodigo: json['tipoDocumentoCodigo'] as String,
        nombre: json['nombre'] as String,
        descripcion: json['descripcion'] as String?,
        obligatorio: json['obligatorio'] as bool,
        adjuntado: json['adjuntado'] as bool,
        archivos: [
          for (final a in json['archivos'] as List<dynamic>) ArchivoAdjunto.fromJson(a as Map<String, dynamic>),
        ],
      );
}

/// Lo que la app valida antes de subir; el backend vuelve a validar por el contenido del archivo.
class ReglasArchivo {
  final int tamanoMaximoBytes;

  /// Tipos MIME permitidos, p. ej. `application/pdf`.
  final List<String> formatosPermitidos;
  final int maximoArchivosPorTipo;

  const ReglasArchivo({
    required this.tamanoMaximoBytes,
    required this.formatosPermitidos,
    required this.maximoArchivosPorTipo,
  });

  factory ReglasArchivo.fromJson(Map<String, dynamic> json) => ReglasArchivo(
        tamanoMaximoBytes: (json['tamanoMaximoBytes'] as num).toInt(),
        formatosPermitidos: [for (final f in json['formatosPermitidos'] as List<dynamic>) f as String],
        maximoArchivosPorTipo: (json['maximoArchivosPorTipo'] as num).toInt(),
      );
}

/// Los documentos de una postulación: la lista que exige la convocatoria con su estado.
class DocumentosPostulacion {
  /// Si todavía se puede adjuntar, reemplazar o eliminar (borrador o devuelta, con la convocatoria abierta). Lo
  /// calcula el backend: la app no lo recalcula.
  final bool puedeEditar;
  final List<DocumentoRequerido> requeridos;
  final ReglasArchivo reglas;

  const DocumentosPostulacion({required this.puedeEditar, required this.requeridos, required this.reglas});

  factory DocumentosPostulacion.fromJson(Map<String, dynamic> json) => DocumentosPostulacion(
        puedeEditar: json['puedeEditar'] as bool,
        requeridos: [
          for (final r in json['requeridos'] as List<dynamic>) DocumentoRequerido.fromJson(r as Map<String, dynamic>),
        ],
        reglas: ReglasArchivo.fromJson(json['reglas'] as Map<String, dynamic>),
      );

  int get adjuntados => requeridos.where((r) => r.adjuntado).length;
}
