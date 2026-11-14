import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/auth_error_mapper.dart';
import '../../auth/auth_validators.dart';
import '../../auth/widgets/alerta_error.dart';
import '../providers/perfil_provider.dart';
import 'campo_contrasena.dart';

/// Cambiar la contraseña desde el perfil (CU15, flujo E). La sesión actual SIGUE abierta: el backend solo
/// cierra las de los demás dispositivos, así que acá no se navega ni se borra nada de `TokenStorage`. Una
/// contraseña actual incorrecta es un `400` y se marca en su campo. El widget nunca ve el refresh token:
/// lo lee `perfilProvider` del almacenamiento seguro justo antes de enviarlo.
/// ! Las contraseñas no se guardan fuera de los controladores ni se imprimen.
class CambiarContrasenaForm extends ConsumerStatefulWidget {
  const CambiarContrasenaForm({super.key});

  @override
  ConsumerState<CambiarContrasenaForm> createState() => _CambiarContrasenaFormState();
}

class _CambiarContrasenaFormState extends ConsumerState<CambiarContrasenaForm> {
  final _formKey = GlobalKey<FormState>();
  final _actual = TextEditingController();
  final _nueva = TextEditingController();
  final _confirmacion = TextEditingController();
  AuthError? _errorGeneral;
  String? _errorActual;

  @override
  void dispose() {
    _actual.dispose();
    _nueva.dispose();
    _confirmacion.dispose();
    super.dispose();
  }

  Future<void> _cambiar() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _errorGeneral = null;
      _errorActual = null;
    });

    final error = await ref.read(perfilProvider.notifier).cambiarContrasena(
          contrasenaActual: _actual.text,
          contrasenaNueva: _nueva.text,
          confirmacion: _confirmacion.text,
        );
    if (!mounted) return;
    if (error == null) {
      _formKey.currentState?.reset();
      _actual.clear();
      _nueva.clear();
      _confirmacion.clear();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Contraseña actualizada. Cerramos tu sesión en los demás dispositivos.')),
      );
      return;
    }
    setState(() {
      if (error.campo == CampoAuth.contrasenaActual) {
        _errorActual = error.mensaje;
      } else {
        _errorGeneral = error;
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final cambiando = ref.watch(perfilProvider);
    final tema = Theme.of(context);

    return Form(
      key: _formKey,
      autovalidateMode: AutovalidateMode.onUserInteraction,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text('Cambiar contraseña', style: tema.textTheme.titleLarge),
          const SizedBox(height: 4),
          Text(
            'La contraseña nueva debe tener de 6 a 72 caracteres. Cerraremos tu sesión en los demás dispositivos.',
            style: tema.textTheme.bodyMedium,
          ),
          const SizedBox(height: 16),
          if (_errorGeneral != null) ...[
            AlertaError(error: _errorGeneral!, onReintentar: cambiando ? null : _cambiar),
            const SizedBox(height: 16),
          ],
          CampoContrasena(
            label: 'Contraseña actual',
            controller: _actual,
            errorText: _errorActual,
            onChanged: (_) {
              if (_errorActual != null) setState(() => _errorActual = null);
            },
            validator: (v) => (v == null || v.isEmpty) ? 'La contraseña actual es obligatoria' : null,
          ),
          const SizedBox(height: 16),
          CampoContrasena(
            label: 'Contraseña nueva',
            controller: _nueva,
            autofillHints: const [AutofillHints.newPassword],
            validator: AuthValidators.contrasena,
          ),
          const SizedBox(height: 16),
          CampoContrasena(
            label: 'Confirmar contraseña nueva',
            controller: _confirmacion,
            autofillHints: const [AutofillHints.newPassword],
            validator: (v) => AuthValidators.confirmacion(v, _nueva.text),
            onFieldSubmitted: (_) => _cambiar(),
          ),
          const SizedBox(height: 24),
          FilledButton(
            onPressed: cambiando ? null : _cambiar,
            child: cambiando
                ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                : const Text('Cambiar contraseña'),
          ),
        ],
      ),
    );
  }
}
