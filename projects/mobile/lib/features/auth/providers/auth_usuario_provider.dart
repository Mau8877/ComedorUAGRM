import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/usuario.dart';
import 'auth_sesion_provider.dart';

/// Usuario de la sesión actual, o `null` si no hay sesión (o todavía se está restaurando).
final authUsuarioProvider = Provider<Usuario?>((ref) => ref.watch(authSesionProvider).valueOrNull);
