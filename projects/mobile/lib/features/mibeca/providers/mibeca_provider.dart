import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../formatos_mibeca.dart';
import '../models/mi_beca.dart';
import '../models/pagina_asistencias.dart';
import 'mibeca_servicios_provider.dart';

/// Cuántas asistencias se piden por página (el mismo valor por defecto del backend; sin selector en el móvil).
const tamanoPaginaAsistencias = 20;

/// La beca del usuario. Se descarta al salir de la pantalla. Un error (incluido `ERR_MBE_01`) lo maneja la vista.
final miBecaProvider = FutureProvider.autoDispose<MiBeca>((ref) => ref.watch(miBecaServiceProvider).obtener());

/// Estado de pantalla del historial: el rango de fechas elegido (`null` = sin filtro). Cambiarlo recarga el
/// historial desde la página 1.
class MiBecaRangoUi extends Notifier<DateTimeRange?> {
  @override
  DateTimeRange? build() => null;

  void elegir(DateTimeRange? rango) => state = rango;
}

final miBecaRangoUiProvider = NotifierProvider<MiBecaRangoUi, DateTimeRange?>(MiBecaRangoUi.new);

/// Lo acumulado del historial: las páginas ya cargadas, el `meta` de la última y si se está pidiendo otra.
class ListaAsistencias {
  final List<AsistenciaBecado> items;
  final MetaAsistencias meta;
  final bool cargandoMas;

  /// La última página pedida falló: se muestra «Reintentar» al final y lo ya cargado sigue ahí.
  final bool errorSiguiente;

  const ListaAsistencias({
    required this.items,
    required this.meta,
    this.cargandoMas = false,
    this.errorSiguiente = false,
  });

  bool get hayMas => meta.page < meta.totalPages;

  ListaAsistencias copyWith({
    List<AsistenciaBecado>? items,
    MetaAsistencias? meta,
    bool? cargandoMas,
    bool? errorSiguiente,
  }) =>
      ListaAsistencias(
        items: items ?? this.items,
        meta: meta ?? this.meta,
        cargandoMas: cargandoMas ?? this.cargandoMas,
        errorSiguiente: errorSiguiente ?? this.errorSiguiente,
      );
}

/// El historial de asistencia del becado con paginación acumulada (scroll infinito, CU24). Escucha el rango: al
/// cambiarlo se descarta lo acumulado y se pide la página 1. Se descarta al salir de la pantalla.
///
/// * Solo lectura: no escribe nada.
class MiBecaAsistencias extends AutoDisposeAsyncNotifier<ListaAsistencias> {
  @override
  Future<ListaAsistencias> build() async {
    final rango = ref.watch(miBecaRangoUiProvider);
    final pagina = await ref.read(miBecaServiceProvider).listarAsistencias(
          desde: rango == null ? null : diaParaApi(rango.start),
          hasta: rango == null ? null : diaParaApi(rango.end),
          page: 1,
          pageSize: tamanoPaginaAsistencias,
        );
    return ListaAsistencias(items: pagina.items, meta: pagina.meta);
  }

  /// Pide la página siguiente y la suma a lo cargado. Se ignora si no hay más o si ya se está pidiendo; un fallo
  /// deja lo cargado y marca [ListaAsistencias.errorSiguiente].
  Future<void> cargarSiguiente() async {
    final actual = state.valueOrNull;
    if (actual == null || !actual.hayMas || actual.cargandoMas) return;
    final rango = ref.read(miBecaRangoUiProvider);
    state = AsyncData(actual.copyWith(cargandoMas: true, errorSiguiente: false));
    try {
      final pagina = await ref.read(miBecaServiceProvider).listarAsistencias(
            desde: rango == null ? null : diaParaApi(rango.start),
            hasta: rango == null ? null : diaParaApi(rango.end),
            page: actual.meta.page + 1,
            pageSize: tamanoPaginaAsistencias,
          );
      // Si mientras tanto cambió el rango, el historial ya se reinició: este resultado es del anterior.
      if (ref.read(miBecaRangoUiProvider) != rango) return;
      state = AsyncData(actual.copyWith(
        items: [...actual.items, ...pagina.items],
        meta: pagina.meta,
        cargandoMas: false,
      ));
    } catch (_) {
      if (ref.read(miBecaRangoUiProvider) != rango) return;
      state = AsyncData(actual.copyWith(cargandoMas: false, errorSiguiente: true));
    }
  }
}

final miBecaAsistenciasProvider =
    AsyncNotifierProvider.autoDispose<MiBecaAsistencias, ListaAsistencias>(MiBecaAsistencias.new);
