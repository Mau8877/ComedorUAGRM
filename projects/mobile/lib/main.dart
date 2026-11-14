import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/routing/app_router.dart';
import 'core/theme/app_theme.dart';
import 'features/notificaciones/models/mensaje_push.dart';
import 'features/notificaciones/destino_aviso.dart';
import 'features/notificaciones/providers/avisos_push_provider.dart';
import 'features/notificaciones/providers/avisos_tocados_provider.dart';
import 'features/notificaciones/providers/push_registro_provider.dart';
import 'features/notificaciones/widgets/aviso_push.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await dotenv.load();
  // Push (CU-EXTRA-01): sin google-services.json o sin Google Play Services, Firebase no arranca
  // y la app sigue funcionando, solo sin notificaciones push.
  try {
    await Firebase.initializeApp();
  } catch (e) {
    debugPrint('Firebase no disponible, la app sigue sin push: ${e.runtimeType}');
  }
  runApp(const ProviderScope(child: MyApp()));
}

class MyApp extends ConsumerWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // Mantiene registrado el teléfono mientras haya sesión (CU-EXTRA-01).
    ref.watch(pushRegistroProvider);
    // Tocar un aviso de una postulación abre su detalle (CU04, A6), pasando por el `redirect` global.
    ref.watch(navegacionDeAvisosProvider);
    // Avisos que llegan con la app abierta.
    ref.listen<AsyncValue<MensajePush>>(avisosPushProvider, (_, siguiente) {
      final mensaje = siguiente.valueOrNull;
      if (mensaje == null) return;
      final ruta = rutaDeAviso(mensaje);
      mostrarAvisoPush(
        ref.read(scaffoldMessengerKeyProvider),
        mensaje,
        alVer: ruta == null ? null : () => ref.read(navegarARutaProvider)(ruta),
      );
    });
    return MaterialApp.router(
      title: 'ComedorUAGRM',
      scaffoldMessengerKey: ref.watch(scaffoldMessengerKeyProvider),
      theme: AppTheme.light,
      darkTheme: AppTheme.dark,
      routerConfig: ref.watch(appRouterProvider),
    );
  }
}
