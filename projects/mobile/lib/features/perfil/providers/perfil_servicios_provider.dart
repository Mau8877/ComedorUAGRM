import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../services/perfil_service.dart';

/// Servicio de la API del perfil (los tests lo sustituyen con `overrideWithValue`).
final perfilServiceProvider = Provider<PerfilService>((ref) => PerfilService(ApiClient.instance));
