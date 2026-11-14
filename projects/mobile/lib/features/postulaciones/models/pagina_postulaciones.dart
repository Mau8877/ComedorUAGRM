import 'mi_postulacion_resumen.dart';

/// El bloque `meta` de un listado paginado del backend.
class MetaPagina {
  final int page;
  final int pageSize;
  final int totalItems;
  final int totalPages;

  const MetaPagina({required this.page, required this.pageSize, required this.totalItems, required this.totalPages});

  factory MetaPagina.fromJson(Map<String, dynamic> json) => MetaPagina(
        page: (json['page'] as num).toInt(),
        pageSize: (json['pageSize'] as num).toInt(),
        totalItems: (json['totalItems'] as num).toInt(),
        totalPages: (json['totalPages'] as num).toInt(),
      );
}

/// Una página de «Mis postulaciones»: los elementos y su `meta`. El `ApiClient` entrega `{data, meta}`.
class PaginaPostulaciones {
  final List<MiPostulacionResumen> items;
  final MetaPagina meta;

  const PaginaPostulaciones({required this.items, required this.meta});

  factory PaginaPostulaciones.fromJson(Map<String, dynamic> json) => PaginaPostulaciones(
        items: (json['data'] as List<dynamic>)
            .map((item) => MiPostulacionResumen.fromJson(item as Map<String, dynamic>))
            .toList(),
        meta: MetaPagina.fromJson(json['meta'] as Map<String, dynamic>),
      );
}
