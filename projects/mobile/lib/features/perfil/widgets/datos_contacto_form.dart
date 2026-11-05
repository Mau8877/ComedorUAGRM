import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/auth_error_mapper.dart';
import '../../auth/auth_validators.dart';
import '../../auth/widgets/alerta_error.dart';
import '../models/actualizar_perfil_request.dart';
import '../models/perfil.dart';
import '../providers/perfil_provider.dart';
import 'campo_contrasena.dart';

/// Datos de contacto (CU15, flujo D): el correo y, si es Postulante, el teléfono. Cambiar el correo exige
/// la contraseña actual (A7); manda solo lo que cambió. El CI y los demás datos no son campos de este formulario.
/// ! La contraseña actual no se guarda fuera del controlador ni se imprime.
class DatosContactoForm extends ConsumerStatefulWidget {
  final Perfil perfil;

  const DatosContactoForm({super.key, required this.perfil});

  @override
  ConsumerState<DatosContactoForm> createState() => _DatosContactoFormState();
}

class _DatosContactoFormState extends ConsumerState<DatosContactoForm> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _correo;
  late final TextEditingController _telefono;
  final _contrasenaActual = TextEditingController();
  AuthError? _errorGeneral;
  final Map<CampoAuth, String> _erroresServidor = {};

  Perfil get _perfil => widget.perfil;

  @override
  void initState() {
    super.initState();
    _correo = TextEditingController(text: _perfil.correo);
    _telefono = TextEditingController(text: _perfil.postulante?.telefono ?? '');
    for (final c in [_correo, _telefono, _contrasenaActual]) {
      c.addListener(_alCambiar);
    }
  }

  @override
  void didUpdateWidget(DatosContactoForm anterior) {
    super.didUpdateWidget(anterior);
    // El perfil se volvió a cargar (tras guardar): el formulario toma lo guardado.
    if (anterior.perfil.correo != _perfil.correo || anterior.perfil.postulante?.telefono != _perfil.postulante?.telefono) {
      _correo.text = _perfil.correo;
      _telefono.text = _perfil.postulante?.telefono ?? '';
      _contrasenaActual.clear();
    }
  }

  @override
  void dispose() {
    _correo.dispose();
    _telefono.dispose();
    _contrasenaActual.dispose();
    super.dispose();
  }

  void _alCambiar() => setState(() {});

  ActualizarPerfilRequest get _cambios => ActualizarPerfilRequest.desdeCambios(
        perfil: _perfil,
        correo: _correo.text,
        telefono: _telefono.text,
        contrasenaActual: _contrasenaActual.text,
      );

  bool get _correoCambio => _cambios.correo != null;

  void _limpiarError(CampoAuth campo) {
    if (_erroresServidor.containsKey(campo)) setState(() => _erroresServidor.remove(campo));
  }

  Future<void> _guardar() async {
    if (!_formKey.currentState!.validate()) return;
    final request = _cambios;
    if (request.vacio) return;
    setState(() {
      _errorGeneral = null;
      _erroresServidor.clear();
    });

    final error = await ref.read(perfilProvider.notifier).actualizar(request);
    if (!mounted) return;
    if (error == null) {
      _contrasenaActual.clear();
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Datos actualizados')));
      return;
    }
    setState(() {
      // El dato en conflicto (A5) o la contraseña incorrecta (A6) se marcan en su campo; lo escrito se conserva.
      if (error.campo == CampoAuth.correo || error.campo == CampoAuth.contrasenaActual) {
        _erroresServidor[error.campo!] = error.mensaje;
      } else {
        _errorGeneral = error;
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final guardando = ref.watch(perfilProvider);
    final tema = Theme.of(context);
    final correoCambio = _correoCambio;
    final sinCambios = _cambios.vacio;

    return Form(
      key: _formKey,
      autovalidateMode: AutovalidateMode.onUserInteraction,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text('Datos de contacto', style: tema.textTheme.titleLarge),
          const SizedBox(height: 4),
          Text('Las notificaciones se envían a este correo y teléfono.', style: tema.textTheme.bodyMedium),
          const SizedBox(height: 16),
          if (_errorGeneral != null) ...[
            AlertaError(error: _errorGeneral!, onReintentar: guardando ? null : _guardar),
            const SizedBox(height: 16),
          ],
          TextFormField(
            controller: _correo,
            keyboardType: TextInputType.emailAddress,
            autofillHints: const [AutofillHints.email],
            decoration: InputDecoration(labelText: 'Correo electrónico', errorText: _erroresServidor[CampoAuth.correo]),
            validator: AuthValidators.correo,
            onChanged: (_) => _limpiarError(CampoAuth.correo),
          ),
          if (_perfil.esPostulante) ...[
            const SizedBox(height: 16),
            TextFormField(
              controller: _telefono,
              keyboardType: TextInputType.phone,
              autofillHints: const [AutofillHints.telephoneNumber],
              decoration: const InputDecoration(labelText: 'Teléfono'),
              validator: AuthValidators.telefono,
            ),
          ],
          // La contraseña actual aparece solo si el correo cambió (A7); si vuelve al original, desaparece.
          if (correoCambio) ...[
            const SizedBox(height: 16),
            CampoContrasena(
              label: 'Contraseña actual',
              controller: _contrasenaActual,
              errorText: _erroresServidor[CampoAuth.contrasenaActual],
              onChanged: (_) => _limpiarError(CampoAuth.contrasenaActual),
              validator: (v) => (v == null || v.isEmpty) ? 'Ingresa tu contraseña actual para cambiar el correo' : null,
            ),
            const SizedBox(height: 4),
            Text('Por seguridad, confirma tu contraseña para cambiar el correo.', style: tema.textTheme.bodySmall),
          ],
          const SizedBox(height: 16),
          FilledButton(
            onPressed: (guardando || sinCambios) ? null : _guardar,
            child: guardando
                ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                : const Text('Guardar cambios'),
          ),
        ],
      ),
    );
  }
}
