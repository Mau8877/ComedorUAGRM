import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../shared/widgets/comedoru_logo.dart';
import '../auth_error_mapper.dart';
import '../auth_validators.dart';
import '../providers/auth_provider.dart';
import '../providers/auth_usuario_provider.dart';
import '../rol_destino.dart';
import '../widgets/alerta_error.dart';

/// Inicio de sesión (CU01, flujo B). Los errores van en una alerta sobre el
/// formulario y no en un campo: así no se revela cuál dato falló (A3).
class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _correo = TextEditingController();
  final _contrasena = TextEditingController();
  AuthError? _error;

  @override
  void dispose() {
    _correo.dispose();
    _contrasena.dispose();
    super.dispose();
  }

  Future<void> _enviar() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _error = null);

    final error = await ref.read(authProvider.notifier).iniciarSesion(_correo.text, _contrasena.text);
    if (!mounted) return;
    if (error != null) {
      setState(() => _error = error);
      return;
    }
    final usuario = ref.read(authUsuarioProvider);
    if (usuario != null) context.go(rutaInicioPorRol(usuario.rol));
  }

  @override
  Widget build(BuildContext context) {
    final enviando = ref.watch(authProvider);
    final tema = Theme.of(context);

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Form(
                key: _formKey,
                autovalidateMode: AutovalidateMode.onUserInteraction,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Center(child: ComedoruLogo(size: 72)),
                    const SizedBox(height: 16),
                    Text('Iniciar sesión', style: tema.textTheme.headlineSmall, textAlign: TextAlign.center),
                    const SizedBox(height: 4),
                    Text(
                      'Ingresa con tu correo y tu contraseña.',
                      style: tema.textTheme.bodyMedium,
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 24),
                    if (_error != null) ...[
                      AlertaError(error: _error!, onReintentar: enviando ? null : _enviar),
                      const SizedBox(height: 16),
                    ],
                    TextFormField(
                      controller: _correo,
                      keyboardType: TextInputType.emailAddress,
                      autofillHints: const [AutofillHints.email],
                      decoration: const InputDecoration(labelText: 'Correo electrónico'),
                      validator: AuthValidators.correo,
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _contrasena,
                      obscureText: true,
                      autofillHints: const [AutofillHints.password],
                      decoration: const InputDecoration(labelText: 'Contraseña'),
                      validator: AuthValidators.contrasenaLogin,
                      onFieldSubmitted: (_) => _enviar(),
                    ),
                    const SizedBox(height: 24),
                    FilledButton(
                      onPressed: enviando ? null : _enviar,
                      child: enviando
                          ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                          : const Text('Iniciar sesión'),
                    ),
                    const SizedBox(height: 12),
                    TextButton(
                      onPressed: enviando ? null : () => context.go('/registro'),
                      child: const Text('¿No tienes cuenta? Crear cuenta'),
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
}
