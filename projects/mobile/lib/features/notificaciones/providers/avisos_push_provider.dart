import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/mensaje_push.dart';
import 'notificaciones_servicios_provider.dart';

/// Avisos push que llegan con la app abierta (en segundo plano o cerrada los muestra el sistema).
/// Vacío si Firebase no está disponible.
final avisosPushProvider = StreamProvider<MensajePush>((ref) => ref.watch(pushServiceProvider).mensajes);

/// Llave del `ScaffoldMessenger` raíz: permite mostrar un aviso sobre cualquier pantalla sin
/// necesitar su `BuildContext`.
final scaffoldMessengerKeyProvider = Provider<GlobalKey<ScaffoldMessengerState>>((ref) => GlobalKey<ScaffoldMessengerState>());
