import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/mi_postulacion_detalle.dart';
import '../models/mi_postulacion_resumen.dart';
import '../models/pagina_postulaciones.dart';
import 'postulaciones_servicios_provider.dart';

/// Cuántas postulaciones se piden por página (el mismo valor por defecto del backend; sin selector en el móvil).
const tamanoPaginaPostulaciones = 20;

/// Estado de pantalla de «Mis postulaciones»: la gestión académica elegida en el filtro (`null` = todas).
/// Cambiarla recarga la lista desde la página 1.
class MisPostulacionesUi extends Notifier<String?> {
  @override
  String? build() => null;

  void elegirGestion(String? gestion) => state = gestion;
}

final misPostulacionesUiProvider = NotifierProvider<MisPostulacionesUi, String?>(MisPostulacionesUi.new);

/// Lo acumulado de la lista: las páginas ya cargadas, el `meta` de la última y si se está pidiendo otra.
class ListaMisPostulaciones {
  final List<MiPostulacionResumen> items;
  final MetaPagina meta;
  final bool cargandoMas;

  /// La última página pedida falló: se muestra «Reintentar» al final y lo ya cargado sigue ahí.
  final bool errorSiguiente;

  const ListaMisPostulaciones({
    required this.items,
    required this.meta,
    this.cargandoMas = false,
    this.errorSiguiente = false,
  });

  bool get hayMas => meta.page < meta.totalPages;

  ListaMisPostulaciones copyWith({
    List<MiPostulacionResumen>? items,
    MetaPagina? meta,
    bool? cargandoMas,
    bool? errorSiguiente,
  }) =>
      ListaMisPostulaciones(
        items: items ?? this.items,
        meta: meta ?? this.meta,
        cargandoMas: cargandoMas ?? this.cargandoMas,
        errorSiguiente: errorSiguiente ?? this.errorSiguiente,
      );
}

/// Las postulaciones enviadas del usuario con paginación acumulada (scroll infinito, CU04). Escucha el filtro:
/// al cambiarlo se descarta lo acumulado y se pide la página 1. Se descarta al salir de la pantalla.
///
/// * Solo lectura: no escribe nada ni marca avisos como leídos.
class MisPostulacionesList extends AutoDisposeAsyncNotifier<ListaMisPostulaciones> {
  @override
  Future<ListaMisPostulaciones> build() async {
    final gestion = ref.watch(misPostulacionesUiProvider);
    final pagina = await ref
        .read(postulacionesServiceProvider)
        .listarMias(gestionAcademica: gestion, page: 1, pageSize: tamanoPaginaPostulaciones);
    return ListaMisPostulaciones(items: pagina.items, meta: pagina.meta);
  }

  /// Pide la página siguiente y la suma a lo cargado. Se ignora si no hay más o si ya se está pidiendo; un fallo
  /// deja lo cargado y marca [ListaMisPostulaciones.errorSiguiente].
  Future<void> cargarSiguiente() async {
    final actual = state.valueOrNull;
    if (actual == null || !actual.hayMas || actual.cargandoMas) return;
    final gestion = ref.read(misPostulacionesUiProvider);
    state = AsyncData(actual.copyWith(cargandoMas: true, errorSiguiente: false));
    try {
      final pagina = await ref.read(postulacionesServiceProvider).listarMias(
            gestionAcademica: gestion,
            page: actual.meta.page + 1,
            pageSize: tamanoPaginaPostulaciones,
          );
      // Si mientras tanto cambió el filtro, la lista ya se reinició: este resultado es de la anterior.
      if (ref.read(misPostulacionesUiProvider) != gestion) return;
      state = AsyncData(actual.copyWith(
        items: [...actual.items, ...pagina.items],
        meta: pagina.meta,
        cargandoMas: false,
      ));
    } catch (_) {
      if (ref.read(misPostulacionesUiProvider) != gestion) return;
      state = AsyncData(actual.copyWith(cargandoMas: false, errorSiguiente: true));
    }
  }
}

final misPostulacionesListProvider =
    AsyncNotifierProvider.autoDispose<MisPostulacionesList, ListaMisPostulaciones>(MisPostulacionesList.new);

/// El detalle de una postulación propia, por su código público. Se descarta al salir de la pantalla.
final postulacionDetailProvider = FutureProvider.autoDispose.family<MiPostulacionDetalle, String>((ref, codigo) {
  return ref.watch(postulacionesServiceProvider).obtener(codigo);
});
