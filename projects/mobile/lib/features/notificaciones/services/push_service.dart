import '../models/mensaje_push.dart';

/// Lo que la app necesita de Firebase Messaging (CU-EXTRA-01): permiso, token del teléfono, su
/// renovación y los avisos que llegan con la app abierta.
///
/// * Es una capa delgada para que nada fuera de `firebase_push_service.dart` importe Firebase y
/// * los tests la sustituyan por una falsa. Si Firebase no está disponible (sin
/// * `google-services.json` o sin Google Play Services) ninguna operación lanza: devuelven
/// * «sin push».
abstract class PushService {
  /// Pide el permiso de notificaciones (Android 13+). `true` si se concedió.
  Future<bool> pedirPermiso();

  /// Token de Firebase de este teléfono, o `null` si no hay push.
  Future<String?> obtenerToken();

  /// Tokens nuevos cuando Firebase renueva el del teléfono.
  Stream<String> get tokenRenovado;

  /// Avisos que llegan con la app en primer plano (en segundo plano los muestra el sistema).
  Stream<MensajePush> get mensajes;
}
