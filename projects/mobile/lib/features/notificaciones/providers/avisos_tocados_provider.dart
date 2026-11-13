import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/routing/app_router.dart';
import '../../auth/providers/auth_sesion_provider.dart';
import '../destino_aviso.dart';
import '../models/mensaje_push.dart';
import 'notificaciones_servicios_provider.dart';

/// Avisos que la persona tocó en la bandeja del sistema (segundo plano o app cerrada). Vacío sin Firebase.
final avisosTocadosProvider = StreamProvider<MensajePush>((ref) => ref.watch(pushServiceProvider).avisosTocados);

/// A dónde ir cuando haya sesión: el aviso se tocó sin sesión (o mientras se restauraba) y primero va el login.
class DestinoAviso extends Notifier<String?> {
  @override
  String? build() => null;

  void definir(String ruta) => state = ruta;

  /// Devuelve el destino pendiente y lo olvida.
  String? tomar() {
    final ruta = state;
    state = null;
    return ruta;
  }
}

final destinoAvisoProvider = NotifierProvider<DestinoAviso, String?>(DestinoAviso.new);

/// Cómo se navega a una ruta (los tests lo sustituyen con `overrideWithValue`). Va por el `GoRouter` único, así
/// que pasa por el `redirect` global.
final navegarARutaProvider = Provider<void Function(String ruta)>(
  (ref) => (ruta) => ref.read(appRouterProvider).go(ruta),
);

/// Lleva al detalle de la postulación cuando se toca un aviso que apunta a una (CU04, A6).
///
/// * Con sesión navega al instante. Sin sesión (o mientras se restaura) guarda el destino y navega en cuanto la
/// * haya: pasando por el login primero. Un aviso sin `postulacionCodigo` no navega: la app solo se abre.
/// * Tocar un aviso no lo marca como leído ni cambia nada en el servidor.
final navegacionDeAvisosProvider = Provider<void>((ref) {
  void abrir(String ruta) => ref.read(navegarARutaProvider)(ruta);

  ref.listen<AsyncValue<MensajePush>>(avisosTocadosProvider, (_, siguiente) {
    final ruta = rutaDeAviso(siguiente.valueOrNull);
    if (ruta == null) return;
    if (ref.read(authSesionProvider).valueOrNull != null) {
      abrir(ruta);
    } else {
      ref.read(destinoAvisoProvider.notifier).definir(ruta);
    }
  }, fireImmediately: true);

  ref.listen(authSesionProvider, (_, siguiente) {
    if (siguiente.valueOrNull == null) return;
    final pendiente = ref.read(destinoAvisoProvider.notifier).tomar();
    if (pendiente != null) abrir(pendiente);
  });
});
