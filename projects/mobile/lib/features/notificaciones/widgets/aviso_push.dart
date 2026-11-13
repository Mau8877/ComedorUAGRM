import 'package:flutter/material.dart';

import '../models/mensaje_push.dart';

/// Muestra un aviso push recibido con la app abierta como un `SnackBar` flotante, sobre la
/// pantalla actual y sin interrumpir lo que la persona hacía (CU-EXTRA-01, CA12).
///
/// * Los colores salen del tema (`SnackBar` los toma de `ColorScheme`); no hay hex sueltos.
/// * Con [alVer] el aviso ofrece «Ver», que abre aquello a lo que apunta (hoy, el detalle de una postulación, CU04).
void mostrarAvisoPush(GlobalKey<ScaffoldMessengerState> llave, MensajePush mensaje, {VoidCallback? alVer}) {
  final messenger = llave.currentState;
  if (messenger == null) return;
  messenger
    ..hideCurrentSnackBar()
    ..showSnackBar(
      SnackBar(
        behavior: SnackBarBehavior.floating,
        duration: const Duration(seconds: 6),
        showCloseIcon: true,
        action: alVer == null ? null : SnackBarAction(label: 'Ver', onPressed: alVer),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (mensaje.titulo.isNotEmpty) Text(mensaje.titulo, style: const TextStyle(fontWeight: FontWeight.w600)),
            if (mensaje.cuerpo.isNotEmpty) Text(mensaje.cuerpo),
          ],
        ),
      ),
    );
}
