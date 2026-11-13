import 'package:firebase_messaging/firebase_messaging.dart';

import '../models/mensaje_push.dart';
import 'push_service.dart';

/// [PushService] sobre `firebase_messaging`. Único archivo de la app que lo importa.
///
/// ! Los tokens no se escriben en ningún log.
class FirebasePushService implements PushService {
  /// `FirebaseMessaging.instance` lanza si `Firebase.initializeApp()` falló (ver `main.dart`).
  FirebaseMessaging? get _messaging {
    try {
      return FirebaseMessaging.instance;
    } catch (_) {
      return null;
    }
  }

  @override
  Future<bool> pedirPermiso() async {
    try {
      final ajustes = await _messaging?.requestPermission();
      final estado = ajustes?.authorizationStatus;
      return estado == AuthorizationStatus.authorized || estado == AuthorizationStatus.provisional;
    } catch (_) {
      return false;
    }
  }

  // * `getToken()` y `onTokenRefresh` figuran como deprecados en firebase_messaging 17, pero su
  // * reemplazo (`register()` / `onRegistered`) entrega el Firebase Installation ID, que es otro
  // * identificador y no se verificó que el Admin SDK del backend lo acepte como destino. Se
  // * mantiene el token de registro clásico, que es el que `CanalPushFcm` envía. Revisar al
  // * migrar el backend a FIDs.
  @override
  Future<String?> obtenerToken() async {
    try {
      // ignore: deprecated_member_use
      return await _messaging?.getToken();
    } catch (_) {
      return null;
    }
  }

  @override
  Stream<String> get tokenRenovado {
    final messaging = _messaging;
    // ignore: deprecated_member_use
    return messaging == null ? const Stream.empty() : messaging.onTokenRefresh;
  }

  @override
  Stream<MensajePush> get mensajes {
    if (_messaging == null) return const Stream.empty();
    return FirebaseMessaging.onMessage.map(_aMensaje);
  }

  @override
  Stream<MensajePush> get avisosTocados async* {
    final messaging = _messaging;
    if (messaging == null) return;
    try {
      // La app estaba cerrada y se abrió tocando un aviso.
      final inicial = await messaging.getInitialMessage();
      if (inicial != null) yield _aMensaje(inicial);
    } catch (_) {
      // Sin Firebase: no hay aviso inicial.
    }
    // La app estaba en segundo plano y se trajo al frente tocando un aviso.
    yield* FirebaseMessaging.onMessageOpenedApp.map(_aMensaje);
  }

  static MensajePush _aMensaje(RemoteMessage m) => MensajePush(
        titulo: m.notification?.title ?? '',
        cuerpo: m.notification?.body ?? '',
        codigoNotificacion: m.data['codigoNotificacion'] as String?,
        evento: m.data['evento'] as String?,
        postulacionCodigo: m.data['postulacionCodigo'] as String?,
      );
}
