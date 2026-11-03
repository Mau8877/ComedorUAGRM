import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/auth_provider.dart';
import '../providers/auth_usuario_provider.dart';
import '../rol_destino.dart';

/// Menú del usuario para el `AppBar` de los shells: muestra su nombre y rol
/// y la opción «Cerrar sesión» (solo cierra este dispositivo). Tras cerrar,
/// el router lleva a `/login` y la tecla atrás no vuelve a una pantalla privada.
class MenuUsuario extends ConsumerWidget {
  const MenuUsuario({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final usuario = ref.watch(authUsuarioProvider);
    final cerrando = ref.watch(authProvider);
    if (usuario == null) return const SizedBox.shrink();

    return PopupMenuButton<String>(
      tooltip: 'Cuenta',
      icon: const Icon(Icons.account_circle_outlined),
      enabled: !cerrando,
      onSelected: (_) => ref.read(authProvider.notifier).cerrarSesion(),
      itemBuilder: (context) => [
        PopupMenuItem<String>(
          enabled: false,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(usuario.nombreCompleto, style: const TextStyle(fontWeight: FontWeight.w600)),
              Text(etiquetaRol(usuario.rol), style: Theme.of(context).textTheme.bodySmall),
            ],
          ),
        ),
        const PopupMenuDivider(),
        const PopupMenuItem<String>(
          value: 'cerrar',
          child: ListTile(
            leading: Icon(Icons.logout),
            title: Text('Cerrar sesión'),
            contentPadding: EdgeInsets.zero,
          ),
        ),
      ],
    );
  }
}
