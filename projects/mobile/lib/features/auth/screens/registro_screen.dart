import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../auth_error_mapper.dart';
import '../auth_validators.dart';
import '../models/registro_request.dart';
import '../providers/auth_carreras_provider.dart';
import '../providers/auth_provider.dart';
import '../providers/auth_usuario_provider.dart';
import '../rol_destino.dart';
import '../widgets/alerta_error.dart';

/// Registro de un Postulante (CU01, flujo A).
///
/// * No hay selector de rol ni forma de crear cuentas del personal (CA16): el
/// * backend crea siempre un Postulante, y el personal lo crea el
/// * Administrador DUBSS en el CU14.
/// * Los conflictos del backend (CI, correo, registro universitario) se
/// * marcan en su campo y se limpian al editarlo (A1).
class RegistroScreen extends ConsumerStatefulWidget {
  const RegistroScreen({super.key});

  @override
  ConsumerState<RegistroScreen> createState() => _RegistroScreenState();
}

class _RegistroScreenState extends ConsumerState<RegistroScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nombres = TextEditingController();
  final _apellidos = TextEditingController();
  final _ci = TextEditingController();
  final _registro = TextEditingController();
  final _telefono = TextEditingController();
  final _correo = TextEditingController();
  final _contrasena = TextEditingController();
  final _confirmacion = TextEditingController();

  String? _carreraCodigo;
  AuthError? _errorGeneral;
  final Map<CampoAuth, String> _erroresServidor = {};

  @override
  void dispose() {
    for (final c in [_nombres, _apellidos, _ci, _registro, _telefono, _correo, _contrasena, _confirmacion]) {
      c.dispose();
    }
    super.dispose();
  }

  void _limpiarServidor(CampoAuth campo) {
    if (_erroresServidor.containsKey(campo)) {
      setState(() => _erroresServidor.remove(campo));
    }
  }

  Future<void> _enviar() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _errorGeneral = null;
      _erroresServidor.clear();
    });

    final error = await ref.read(authProvider.notifier).registrar(
          RegistroRequest(
            nombres: _nombres.text.trim(),
            apellidos: _apellidos.text.trim(),
            ci: _ci.text.trim(),
            registroUniversitario: _registro.text,
            carreraCodigo: _carreraCodigo!,
            telefono: _telefono.text.trim(),
            correo: _correo.text.trim(),
            contrasena: _contrasena.text,
            confirmacionContrasena: _confirmacion.text,
          ),
        );
    if (!mounted) return;
    if (error == null) {
      final usuario = ref.read(authUsuarioProvider);
      if (usuario != null) context.go(rutaInicioPorRol(usuario.rol));
      return;
    }
    setState(() {
      if (error.campo != null) {
        _erroresServidor[error.campo!] = error.mensaje;
      } else {
        _errorGeneral = error;
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final enviando = ref.watch(authProvider);
    final carreras = ref.watch(authCarrerasProvider);
    final tema = Theme.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text('Crear cuenta')),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 480),
              child: Form(
                key: _formKey,
                autovalidateMode: AutovalidateMode.onUserInteraction,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Text('Regístrate como postulante a la Beca Comedor.', style: tema.textTheme.bodyMedium),
                    const SizedBox(height: 16),
                    if (_errorGeneral != null) ...[
                      AlertaError(error: _errorGeneral!, onReintentar: enviando ? null : _enviar),
                      const SizedBox(height: 16),
                    ],
                    TextFormField(
                      controller: _nombres,
                      textCapitalization: TextCapitalization.words,
                      decoration: const InputDecoration(labelText: 'Nombres'),
                      validator: AuthValidators.nombres,
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _apellidos,
                      textCapitalization: TextCapitalization.words,
                      decoration: const InputDecoration(labelText: 'Apellidos'),
                      validator: AuthValidators.apellidos,
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _ci,
                      decoration: InputDecoration(labelText: 'CI', errorText: _erroresServidor[CampoAuth.ci]),
                      validator: AuthValidators.ci,
                      onChanged: (_) => _limpiarServidor(CampoAuth.ci),
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _registro,
                      keyboardType: TextInputType.number,
                      decoration: InputDecoration(
                        labelText: 'Registro universitario',
                        errorText: _erroresServidor[CampoAuth.registroUniversitario],
                      ),
                      validator: AuthValidators.registroUniversitario,
                      onChanged: (_) => _limpiarServidor(CampoAuth.registroUniversitario),
                    ),
                    const SizedBox(height: 16),
                    _selectorCarrera(carreras),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _telefono,
                      keyboardType: TextInputType.phone,
                      decoration: const InputDecoration(labelText: 'Teléfono'),
                      validator: AuthValidators.telefono,
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _correo,
                      keyboardType: TextInputType.emailAddress,
                      decoration: InputDecoration(
                        labelText: 'Correo electrónico',
                        errorText: _erroresServidor[CampoAuth.correo],
                      ),
                      validator: AuthValidators.correo,
                      onChanged: (_) => _limpiarServidor(CampoAuth.correo),
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _contrasena,
                      obscureText: true,
                      decoration: const InputDecoration(labelText: 'Contraseña'),
                      validator: AuthValidators.contrasena,
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _confirmacion,
                      obscureText: true,
                      decoration: const InputDecoration(labelText: 'Confirmar contraseña'),
                      validator: (v) => AuthValidators.confirmacion(v, _contrasena.text),
                    ),
                    const SizedBox(height: 24),
                    FilledButton(
                      onPressed: enviando ? null : _enviar,
                      child: enviando
                          ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                          : const Text('Crear cuenta'),
                    ),
                    const SizedBox(height: 12),
                    TextButton(
                      onPressed: enviando ? null : () => context.go('/login'),
                      child: const Text('¿Ya tienes cuenta? Inicia sesión'),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _selectorCarrera(AsyncValue carreras) {
    return carreras.when(
      loading: () => const InputDecorator(
        decoration: InputDecoration(labelText: 'Carrera'),
        child: Text('Cargando carreras…'),
      ),
      error: (_, _) => InputDecorator(
        decoration: const InputDecoration(labelText: 'Carrera', errorText: 'No se pudieron cargar las carreras.'),
        child: Align(
          alignment: Alignment.centerLeft,
          child: TextButton(
            onPressed: () => ref.invalidate(authCarrerasProvider),
            child: const Text('Reintentar'),
          ),
        ),
      ),
      data: (lista) => DropdownButtonFormField<String>(
        isExpanded: true,
        initialValue: _carreraCodigo,
        decoration: InputDecoration(
          labelText: 'Carrera',
          errorText: _erroresServidor[CampoAuth.carrera],
        ),
        items: [
          for (final carrera in lista)
            DropdownMenuItem<String>(
              value: carrera.codigo,
              child: Text(carrera.nombre, overflow: TextOverflow.ellipsis),
            ),
        ],
        validator: AuthValidators.carrera,
        onChanged: (valor) {
          _limpiarServidor(CampoAuth.carrera);
          setState(() => _carreraCodigo = valor);
        },
      ),
    );
  }
}
