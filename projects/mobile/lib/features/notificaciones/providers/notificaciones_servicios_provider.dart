import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../services/dispositivos_service.dart';
import '../services/firebase_push_service.dart';
import '../services/push_service.dart';

/// Acceso a Firebase Messaging (los tests lo sustituyen con `overrideWithValue`).
final pushServiceProvider = Provider<PushService>((ref) => FirebasePushService());

final dispositivosServiceProvider = Provider<DispositivosService>((ref) => DispositivosService(ApiClient.instance));
