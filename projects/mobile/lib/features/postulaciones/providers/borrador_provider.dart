import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/borrador_postulacion.dart';
import 'postulaciones_servicios_provider.dart';

/// «Mi borrador» (CU-17): el borrador de la convocatoria abierta, o `null` si no hay. Se descarta al salir de la
/// pantalla; quien guarda un borrador lo invalida para que «Mis postulaciones» lo vea al volver.
final miBorradorProvider = FutureProvider.autoDispose<BorradorResumen?>(
  (ref) => ref.watch(postulacionesServiceProvider).miBorrador(),
);
