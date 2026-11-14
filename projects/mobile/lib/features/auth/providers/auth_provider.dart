import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../notificaciones/providers/push_registro_provider.dart';
import '../auth_error_mapper.dart';
import '../models/registro_request.dart';
import 'auth_servicios_provider.dart';
import 'auth_sesion_provider.dart';

/// Acciones de CU01: registrar (flujo A), iniciar sesión (B) y cerrar sesión (C).
///
/// * `registrar` e `iniciarSesion` devuelven `null` si salió bien (la sesión ya
/// * quedó abierta y el router lleva al módulo) o un [AuthError] ya traducido.
/// * El estado es `true` mientras hay una acción en curso: las pantallas
/// * deshabilitan el botón con eso (A7) y una segunda llamada se ignora. Es un
/// * `Notifier<bool>` síncrono (y no un `AsyncNotifier`) para que arranque en
/// * `false` y no `loading`.
/// * `cerrarSesion` solo cierra ESTE dispositivo y nunca falla (A10).
/// ! Ni contraseñas ni tokens pasan por un log.
class AuthAcciones extends Notifier<bool> {
  @override
  bool build() => false;

  Future<AuthError?> registrar(RegistroRequest request) {
    return _ejecutar(() async {
      final sesion = await ref.read(authServiceProvider).registrar(request);
      await ref.read(authSesionProvider.notifier).abrir(sesion);
    });
  }

  Future<AuthError?> iniciarSesion(String correo, String contrasena) {
    return _ejecutar(() async {
      final sesion = await ref.read(authServiceProvider).iniciarSesion(correo.trim(), contrasena);
      await ref.read(authSesionProvider.notifier).abrir(sesion);
    });
  }

  /// Flujo A del CU15: pide el enlace de recuperación. Devuelve `null` si el backend respondió bien
  /// (la pantalla muestra siempre el mismo mensaje neutro) o un [AuthError] (red, límite de peticiones).
  Future<AuthError?> solicitarRecuperacion(String correo) {
    return _ejecutar(() => ref.read(authServiceProvider).solicitarRecuperacion(correo.trim()));
  }

  Future<void> cerrarSesion() async {
    state = true;
    // CU-EXTRA-01: la baja del teléfono necesita la sesión, así que va antes de borrarla.
    // Nunca falla (sin conexión se cierra igual).
    await ref.read(pushRegistroProvider.notifier).darDeBaja();
    try {
      final refresh = await ref.read(tokenStorageProvider).leerRefreshToken();
      if (refresh != null) {
        await ref.read(authServiceProvider).cerrarSesion(refresh);
      }
    } catch (_) {
      // A10: sesión ya cerrada o sin conexión, se sigue igual.
    }
    await ref.read(authSesionProvider.notifier).cerrar();
    state = false;
  }

  Future<AuthError?> _ejecutar(Future<void> Function() accion) async {
    if (state) return const AuthError('Ya hay una solicitud en curso.');
    state = true;
    try {
      await accion();
      return null;
    } catch (e) {
      return mapearError(e);
    } finally {
      state = false;
    }
  }
}

final authProvider = NotifierProvider<AuthAcciones, bool>(AuthAcciones.new);
