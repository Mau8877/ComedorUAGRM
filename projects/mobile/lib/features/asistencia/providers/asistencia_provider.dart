import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../asistencia_error_mapper.dart';
import '../models/becado_asistencia.dart';
import 'asistencia_servicios_provider.dart';

/// Estado de la pantalla «Registrar asistencia» (CU10).
class EstadoAsistencia {
  /// El becado encontrado, o `null` si todavía no se buscó o se pasó al siguiente.
  final BecadoAsistencia? becado;

  /// Hay una búsqueda o un registro en curso (la pantalla deshabilita los botones).
  final bool ocupado;

  /// El último error ya traducido, o `null`.
  final ErrorAsistencia? error;

  /// La asistencia se acaba de registrar con esta pulsación (para el aviso de confirmación, paso 9).
  final bool recienRegistrada;

  const EstadoAsistencia({this.becado, this.ocupado = false, this.error, this.recienRegistrada = false});
}

/// Acciones de «Registrar asistencia»: buscar por CI (pasos 3 a 5), registrar (pasos 6 a 9) y pasar al siguiente.
///
/// * Registrar es idempotente: si ya había asistencia de hoy (A3) el backend devuelve la hora del primer registro y
/// * el estado queda como «ya registrada», sin error. Una segunda llamada mientras hay una en curso se ignora.
/// ! El CI y el nombre del becado no pasan por ningún log.
class AsistenciaNotifier extends AutoDisposeNotifier<EstadoAsistencia> {
  @override
  EstadoAsistencia build() => const EstadoAsistencia();

  Future<void> buscar(String ci) async {
    if (state.ocupado) return;
    state = const EstadoAsistencia(ocupado: true);
    try {
      final becado = await ref.read(asistenciaServiceProvider).buscarBecado(ci.trim());
      state = EstadoAsistencia(becado: becado);
    } catch (e) {
      state = EstadoAsistencia(error: mapearErrorAsistencia(e));
    }
  }

  Future<void> registrar() async {
    final becado = state.becado;
    if (state.ocupado || becado == null || !becado.vigente || becado.yaRegistroHoy) return;
    state = EstadoAsistencia(becado: becado, ocupado: true);
    try {
      final resultado = await ref.read(asistenciaServiceProvider).registrar(becado.becaCodigo);
      state = EstadoAsistencia(
        becado: becado.conHora(resultado.horaRegistro),
        recienRegistrada: !resultado.yaRegistrada,
      );
    } catch (e) {
      state = EstadoAsistencia(becado: becado, error: mapearErrorAsistencia(e));
    }
  }

  /// Deja la pantalla lista para el siguiente becado (paso 9).
  void siguiente() => state = const EstadoAsistencia();
}

final asistenciaProvider = AutoDisposeNotifierProvider<AsistenciaNotifier, EstadoAsistencia>(AsistenciaNotifier.new);
