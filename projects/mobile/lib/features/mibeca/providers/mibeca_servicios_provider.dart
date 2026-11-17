import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../services/mibeca_service.dart';

/// Servicio de la API de «Mi beca» (los tests lo sustituyen con `overrideWithValue`).
final miBecaServiceProvider = Provider<MiBecaService>((ref) => MiBecaService(ApiClient.instance));
