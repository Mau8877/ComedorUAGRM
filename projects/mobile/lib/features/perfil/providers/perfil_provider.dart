import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/auth_error_mapper.dart';
import '../../auth/models/usuario.dart';
import '../../auth/providers/auth_servicios_provider.dart';
import '../../auth/providers/auth_sesion_provider.dart';
import '../models/actualizar_perfil_request.dart';
import '../models/perfil.dart';
import 'perfil_servicios_provider.dart';

/// Perfil del usuario autenticado (CU15, flujo C). Se descarta al salir de la pantalla.
final perfilDetailProvider = FutureProvider.autoDispose<Perfil>((ref) {
  return ref.watch(perfilServiceProvider).obtener();
});

/// Acciones del perfil: actualizar los datos de contacto (flujo D) y cambiar la contraseña (flujo E).
///
/// * Ambas devuelven `null` si salió bien o un [AuthError] ya traducido. El estado es `true` mientras
/// * hay una acción en curso (las pantallas deshabilitan el botón con eso y una segunda llamada se
/// * ignora).
/// * `cambiarContrasena` NO toca la sesión local: el backend solo cierra las de los demás
/// * dispositivos. Un `400` por contraseña actual incorrecta nunca cierra la sesión.
/// ! Ni contraseñas ni el refresh token pasan por un log; el refresh token se lee del
/// ! almacenamiento seguro justo antes de enviarse y no se conserva.
class PerfilAcciones extends Notifier<bool> {
  @override
  bool build() => false;

  Future<AuthError?> actualizar(ActualizarPerfilRequest request) {
    return _ejecutar(() async {
      final perfil = await ref.read(perfilServiceProvider).actualizar(request);
      ref.invalidate(perfilDetailProvider);
      // El correo también vive en el usuario de la sesión (menú de cuenta): se mantiene al día.
      ref.read(authSesionProvider.notifier).actualizarUsuario(Usuario(
            codigo: perfil.codigo,
            nombres: perfil.nombres,
            apellidos: perfil.apellidos,
            correo: perfil.correo,
            rol: perfil.rol,
          ));
    });
  }

  Future<AuthError?> cambiarContrasena({
    required String contrasenaActual,
    required String contrasenaNueva,
    required String confirmacion,
  }) {
    return _ejecutar(() async {
      final refreshToken = await ref.read(tokenStorageProvider).leerRefreshToken();
      await ref.read(perfilServiceProvider).cambiarContrasena(
            contrasenaActual: contrasenaActual,
            contrasenaNueva: contrasenaNueva,
            confirmacion: confirmacion,
            refreshToken: refreshToken,
          );
    });
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

final perfilProvider = NotifierProvider<PerfilAcciones, bool>(PerfilAcciones.new);
