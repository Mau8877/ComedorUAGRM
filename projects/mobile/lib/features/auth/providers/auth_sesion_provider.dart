import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../models/sesion.dart';
import '../models/usuario.dart';
import 'auth_servicios_provider.dart';

/// Estado de sesión de la app: `loading` mientras se restaura, `data(null)` sin
/// sesión y `data(usuario)` con sesión. El router y los shells reaccionan a él.
///
/// * Al arrancar, si hay un refresh token guardado se canjea con
/// * `POST /auth/refresh`; si el backend lo rechaza (`ERR_AUTH_06`, cuenta
/// * desactivada) se borra y queda sin sesión. Si solo falla la red, el token
/// * se conserva (no hay sesión por ahora, pero no se pierde).
/// * También registra en `ApiClient` cómo renovar la sesión y qué hacer cuando
/// * ya no se puede (A8): se cierra y el router lleva al login.
class AuthSesionNotifier extends AsyncNotifier<Usuario?> {
  @override
  Future<Usuario?> build() async {
    ApiClient.onRenovar = _renovar;
    ApiClient.onSesionInvalida = () => cerrar();

    final storage = ref.read(tokenStorageProvider);
    final refresh = await storage.leerRefreshToken();
    if (refresh == null) return null;

    try {
      final sesion = await ref.read(authServiceProvider).renovar(refresh);
      await storage.guardarSesion(accessToken: sesion.accessToken, refreshToken: sesion.refreshToken);
      return sesion.usuario;
    } on DioException catch (e) {
      if (e.error is ApiException) await storage.borrarSesion();
      return null;
    }
  }

  /// Guarda los tokens de una sesión recién abierta (registro o login) y la deja activa.
  Future<void> abrir(Sesion sesion) async {
    await ref.read(tokenStorageProvider).guardarSesion(
          accessToken: sesion.accessToken,
          refreshToken: sesion.refreshToken,
        );
    state = AsyncData(sesion.usuario);
  }

  /// Actualiza los datos del usuario de la sesión (ej. el correo, tras cambiarlo en el perfil)
  /// sin tocar los tokens.
  void actualizarUsuario(Usuario usuario) {
    state = AsyncData(usuario);
  }

  /// Borra la sesión de este dispositivo (los tokens) y deja la app sin sesión.
  Future<void> cerrar() async {
    await ref.read(tokenStorageProvider).borrarSesion();
    state = const AsyncData(null);
  }

  Future<bool> _renovar() async {
    final storage = ref.read(tokenStorageProvider);
    final refresh = await storage.leerRefreshToken();
    if (refresh == null) return false;
    try {
      final sesion = await ref.read(authServiceProvider).renovar(refresh);
      await storage.guardarSesion(accessToken: sesion.accessToken, refreshToken: sesion.refreshToken);
      return true;
    } catch (_) {
      return false;
    }
  }
}

final authSesionProvider = AsyncNotifierProvider<AuthSesionNotifier, Usuario?>(AuthSesionNotifier.new);
