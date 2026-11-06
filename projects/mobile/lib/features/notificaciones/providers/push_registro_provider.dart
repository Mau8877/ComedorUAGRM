import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/providers/auth_sesion_provider.dart';
import 'notificaciones_servicios_provider.dart';

/// Mantiene registrado en el backend el token de este teléfono mientras haya sesión (CU-EXTRA-01).
///
/// * Cuando la sesión pasa a abierta (login, registro o sesión restaurada) pide el permiso, obtiene
/// * el token y lo registra; si Firebase lo renueva, lo vuelve a registrar. Sin permiso o sin
/// * Firebase no hace nada y la app sigue normal (A1). Android ya limita cuántas veces muestra el
/// * diálogo de permiso, por eso no se guarda una marca propia de «ya se pidió».
/// * El estado es el último token registrado (`null` = ninguno).
/// * La baja NO ocurre sola al perder la sesión: necesita la sesión para autenticarse, así que
/// * `AuthAcciones.cerrarSesion` llama a [darDeBaja] antes de borrarla.
/// ! Un error de red nunca se propaga: se reintenta en el próximo inicio de sesión o renovación.
class PushRegistro extends Notifier<String?> {
  StreamSubscription<String>? _renovacion;
  bool _vivo = true;

  @override
  String? build() {
    _vivo = true;
    ref.onDispose(() {
      _vivo = false;
      _renovacion?.cancel();
    });
    ref.listen(authSesionProvider, (anterior, siguiente) {
      final hay = siguiente.valueOrNull != null;
      final habia = anterior?.valueOrNull != null;
      if (hay && !habia) {
        _activar();
      } else if (!hay && habia) {
        _renovacion?.cancel();
        _renovacion = null;
        state = null;
      }
    }, fireImmediately: true);
    return null;
  }

  Future<void> _activar() async {
    try {
      final push = ref.read(pushServiceProvider);
      if (!await push.pedirPermiso()) return;
      final token = await push.obtenerToken();
      if (token == null || !_vivo) return;
      await _registrar(token);
      await _renovacion?.cancel();
      _renovacion = push.tokenRenovado.listen(_registrar);
    } catch (_) {
      // Sin red o sin Firebase: la app sigue sin push.
    }
  }

  Future<void> _registrar(String token) async {
    try {
      await ref.read(dispositivosServiceProvider).registrar(token);
      if (_vivo) state = token;
    } catch (_) {
      // Se reintenta en el próximo inicio de sesión o cuando Firebase renueve el token.
    }
  }

  /// Da de baja el token registrado. Nunca falla (como el resto del cierre de sesión, CU01 A10).
  Future<void> darDeBaja() async {
    final token = state;
    await _renovacion?.cancel();
    _renovacion = null;
    if (token == null) return;
    try {
      await ref.read(dispositivosServiceProvider).darDeBaja(token);
    } catch (_) {
      // Sin conexión o sesión ya vencida: el cierre local continúa igual.
    }
    if (_vivo) state = null;
  }
}

final pushRegistroProvider = NotifierProvider<PushRegistro, String?>(PushRegistro.new);
