import 'package:go_router/go_router.dart';

import '../../features/health/screens/health_check_screen.dart';
import '../../features/pruebas_layout/screens/demo_home_screen.dart';
import '../../features/pruebas_layout/screens/ingredientes_screen.dart';
import '../../layouts/admin/admin_shell.dart';
import '../../layouts/estudiante/estudiante_shell.dart';

/// Router único de la app -- toda la navegación pasa por acá, ninguna
/// pantalla arma su propio `GoRoute` suelto (ver NAVEGACION_MOBILE.md).
final appRouter = GoRouter(
  // TODO: reemplazar por la validación real de sesión una vez que exista
  // TODO: el provider de auth (ver features/auth/, mismo criterio que
  // TODO: _authenticated.tsx del lado del frontend). Por ahora no hay
  // TODO: ningún token que chequear, así que no hay redirect real todavía.
  redirect: (context, state) {
    return null;
  },
  routes: [
    GoRoute(
      path: '/',
      builder: (context, state) => const DemoHomeScreen(),
    ),
    GoRoute(
      path: '/admin',
      builder: (context, state) => const AdminShell(),
    ),
    GoRoute(
      path: '/estudiante',
      builder: (context, state) => const EstudianteShell(),
    ),
    GoRoute(
      path: '/ingredientes',
      builder: (context, state) => const IngredientesScreen(),
    ),
    GoRoute(
      path: '/diagnostico',
      builder: (context, state) => const HealthCheckScreen(),
    ),
  ],
);
