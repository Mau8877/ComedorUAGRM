import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/storage/token_storage.dart';
import '../services/auth_service.dart';

/// Almacenamiento seguro de la sesión (los tests lo sustituyen con `overrideWithValue`).
final tokenStorageProvider = Provider<TokenStorage>((ref) => TokenStorage.instance);

final authServiceProvider = Provider<AuthService>((ref) => AuthService(ApiClient.instance));
