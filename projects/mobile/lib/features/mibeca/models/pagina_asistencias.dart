/// Una asistencia del historial (CU24): el día (de calendario, hora de Bolivia) y la hora en que se registró.
class AsistenciaBecado {
  /// Solo la fecha, sin hora ni zona (`AAAA-MM-DD`).
  final DateTime fecha;
  final DateTime horaRegistro;

  const AsistenciaBecado({required this.fecha, required this.horaRegistro});

  factory AsistenciaBecado.fromJson(Map<String, dynamic> json) => AsistenciaBecado(
        fecha: DateTime.parse(json['fecha'] as String),
        horaRegistro: DateTime.parse(json['horaRegistro'] as String),
      );
}

/// El bloque `meta` de un listado paginado del backend.
class MetaAsistencias {
  final int page;
  final int pageSize;
  final int totalItems;
  final int totalPages;

  const MetaAsistencias({
    required this.page,
    required this.pageSize,
    required this.totalItems,
    required this.totalPages,
  });

  factory MetaAsistencias.fromJson(Map<String, dynamic> json) => MetaAsistencias(
        page: (json['page'] as num).toInt(),
        pageSize: (json['pageSize'] as num).toInt(),
        totalItems: (json['totalItems'] as num).toInt(),
        totalPages: (json['totalPages'] as num).toInt(),
      );
}

/// Una página del historial: los elementos y su `meta`. El `ApiClient` entrega `{data, meta}`.
class PaginaAsistencias {
  final List<AsistenciaBecado> items;
  final MetaAsistencias meta;

  const PaginaAsistencias({required this.items, required this.meta});

  factory PaginaAsistencias.fromJson(Map<String, dynamic> json) => PaginaAsistencias(
        items: (json['data'] as List<dynamic>)
            .map((item) => AsistenciaBecado.fromJson(item as Map<String, dynamic>))
            .toList(),
        meta: MetaAsistencias.fromJson(json['meta'] as Map<String, dynamic>),
      );
}
