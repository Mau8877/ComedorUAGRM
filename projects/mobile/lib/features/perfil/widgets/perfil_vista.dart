import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/auth_error_mapper.dart';
import '../../auth/widgets/alerta_error.dart';
import '../providers/perfil_provider.dart';
import 'cambiar_contrasena_form.dart';
import 'datos_contacto_form.dart';
import 'mis_datos.dart';

/// Contenido de «Mi perfil» (CU15): Mis datos (solo lectura), datos de contacto y cambiar contraseña.
/// Lo usan la pantalla `/perfil` y la sección «Perfil» del shell del Estudiante.
class PerfilVista extends ConsumerWidget {
  const PerfilVista({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final perfil = ref.watch(perfilDetailProvider);

    return perfil.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: AlertaError(
            error: mapearError(e),
            onReintentar: () => ref.invalidate(perfilDetailProvider),
          ),
        ),
      ),
      data: (datos) => SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 520),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                MisDatos(perfil: datos),
                const SizedBox(height: 24),
                const Divider(),
                const SizedBox(height: 24),
                DatosContactoForm(perfil: datos),
                const SizedBox(height: 24),
                const Divider(),
                const SizedBox(height: 24),
                const CambiarContrasenaForm(),
                const SizedBox(height: 24),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
