import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../services/postulaciones_service.dart';

/// Servicio de la API de «Mis postulaciones» (los tests lo sustituyen con `overrideWithValue`).
final postulacionesServiceProvider =
    Provider<PostulacionesService>((ref) => PostulacionesService(ApiClient.instance));
