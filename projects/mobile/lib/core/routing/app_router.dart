import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/models/usuario.dart';
import '../../features/auth/providers/auth_sesion_provider.dart';
import '../../features/auth/rol_destino.dart';
import '../../features/auth/screens/cargando_screen.dart';
import '../../features/auth/screens/login_screen.dart';
import '../../features/auth/screens/recuperar_contrasena_screen.dart';
import '../../features/auth/screens/registro_screen.dart';
import '../../features/auth/screens/sin_modulo_screen.dart';
import '../../features/health/screens/health_check_screen.dart';
import '../../features/perfil/screens/perfil_screen.dart';
import '../../features/postulaciones/screens/detalle_postulacion_screen.dart';
import '../../features/pruebas_layout/screens/demo_home_screen.dart';
import '../../features/pruebas_layout/screens/ingredientes_screen.dart';
import '../../layouts/admin/admin_shell.dart';
import '../../layouts/estudiante/estudiante_shell.dart';

const _rutasPublicas = {'/login', '/registro', '/recuperar-contrasena'};

/// Decide si hay que redirigir (devuelve la ruta destino) o dejar pasar
/// (`null`). Función pura para poder probar todos los casos sin montar la app.
///
/// * Mientras la sesión se restaura (`loading`) todo va a `/cargando`, para
/// * que no parpadee una pantalla privada ni el login. Resuelta: sin sesión
/// * solo se ven `/login`, `/registro` y `/recuperar-contrasena` (A8, CA15, CA18); con sesión, las
/// * pantallas de acceso, `/` y `/cargando` llevan al módulo del rol.
String? redirectDeSesion({required AsyncValue<Usuario?> sesion, required String ubicacion}) {
  if (sesion.isLoading && !sesion.hasValue) {
    return ubicacion == '/cargando' ? null : '/cargando';
  }

  final usuario = sesion.valueOrNull;
  if (usuario == null) {
    return _rutasPublicas.contains(ubicacion) ? null : '/login';
  }

  if (_rutasPublicas.contains(ubicacion) || ubicacion == '/' || ubicacion == '/cargando') {
    return rutaInicioPorRol(usuario.rol);
  }
  return null;
}

/// Router único de la app -- toda la navegación pasa por acá, ninguna
/// pantalla arma su propio `GoRoute` suelto (ver NAVEGACION_MOBILE.md). El
/// chequeo de sesión se resuelve una sola vez, en el `redirect` raíz, y el
/// router se re-evalúa cada vez que cambia el estado de sesión.
final appRouterProvider = Provider<GoRouter>((ref) {
  final cambioDeSesion = ValueNotifier<int>(0);
  ref.listen(authSesionProvider, (_, _) => cambioDeSesion.value++);
  ref.onDispose(cambioDeSesion.dispose);

  final router = GoRouter(
    initialLocation: '/',
    refreshListenable: cambioDeSesion,
    redirect: (context, state) => redirectDeSesion(
      sesion: ref.read(authSesionProvider),
      ubicacion: state.matchedLocation,
    ),
    routes: [
      GoRoute(path: '/', builder: (context, state) => const DemoHomeScreen()),
      GoRoute(path: '/cargando', builder: (context, state) => const CargandoScreen()),
      GoRoute(path: '/login', builder: (context, state) => const LoginScreen()),
      GoRoute(path: '/registro', builder: (context, state) => const RegistroScreen()),
      GoRoute(path: '/recuperar-contrasena', builder: (context, state) => const RecuperarContrasenaScreen()),
      GoRoute(path: '/perfil', builder: (context, state) => const PerfilScreen()),
      GoRoute(path: '/sin-modulo', builder: (context, state) => const SinModuloScreen()),
      GoRoute(path: '/admin', builder: (context, state) => const AdminShell()),
      GoRoute(path: '/estudiante', builder: (context, state) => const EstudianteShell()),
      // Detalle de una postulación (CU04): la lista vive dentro del shell del Estudiante. El `redirect` global
      // ya exige sesión: sin ella, primero el login.
      GoRoute(
        path: '/mis-postulaciones/:codigo',
        builder: (context, state) => DetallePostulacionScreen(codigo: state.pathParameters['codigo']!),
      ),
      GoRoute(path: '/ingredientes', builder: (context, state) => const IngredientesScreen()),
      GoRoute(path: '/diagnostico', builder: (context, state) => const HealthCheckScreen()),
    ],
  );
  ref.onDispose(router.dispose);
  return router;
});
