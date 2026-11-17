import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../services/asistencia_service.dart';

/// Servicio de la API de asistencia (los tests lo sustituyen con `overrideWithValue`).
final asistenciaServiceProvider = Provider<AsistenciaService>((ref) => AsistenciaService(ApiClient.instance));
