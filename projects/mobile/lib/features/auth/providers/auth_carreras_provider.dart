import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/carrera.dart';
import 'auth_servicios_provider.dart';

/// Catálogo público de carreras para el selector del registro.
final authCarrerasProvider = FutureProvider<List<Carrera>>((ref) {
  return ref.watch(authServiceProvider).listarCarreras();
});
