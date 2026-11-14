import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../shared/widgets/comedoru_logo.dart';
import '../auth_error_mapper.dart';
import '../auth_validators.dart';
import '../providers/auth_provider.dart';
import '../widgets/alerta_error.dart';

/// Mensaje fijo tras pedir el enlace (CU15, A1/CA01/CA02/CA18): el mismo para cualquier respuesta,
/// sin repetir el correo ni distinguir si la cuenta existe, está desactivada o falló el correo.
/// El enlace del correo abre el portal web (decisión del CU), donde se define la contraseña nueva.
const mensajeRecuperacionEnviada =
    'Si la cuenta existe, enviamos un enlace al correo indicado. Vence en 60 minutos. '
    'Ábrelo desde tu correo: se abre en el navegador. Revisa también la carpeta de spam.';

/// Solicitud de recuperación de contraseña (CU15, flujo A). La app solo pide el enlace; la contraseña
/// nueva se define en la web con ese enlace.
class RecuperarContrasenaScreen extends ConsumerStatefulWidget {
  const RecuperarContrasenaScreen({super.key});

  @override
  ConsumerState<RecuperarContrasenaScreen> createState() => _RecuperarContrasenaScreenState();
}

class _RecuperarContrasenaScreenState extends ConsumerState<RecuperarContrasenaScreen> {
  final _formKey = GlobalKey<FormState>();
  final _correo = TextEditingController();
  AuthError? _error;
  bool _enviado = false;

  @override
  void dispose() {
    _correo.dispose();
    super.dispose();
  }

  Future<void> _enviar() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _error = null);

    final error = await ref.read(authProvider.notifier).solicitarRecuperacion(_correo.text);
    if (!mounted) return;
    setState(() {
      _error = error;
      _enviado = error == null;
    });
  }

  void _pedirDeNuevo() {
    _correo.clear();
    setState(() => _enviado = false);
  }

  @override
  Widget build(BuildContext context) {
    final enviando = ref.watch(authProvider);
    final tema = Theme.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text('Recuperar contraseña')),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Center(child: ComedoruLogo(size: 72)),
                  const SizedBox(height: 16),
                  Text('Recuperar contraseña', style: tema.textTheme.headlineSmall, textAlign: TextAlign.center),
                  const SizedBox(height: 4),
                  Text(
                    'Te enviaremos un enlace para definir una contraseña nueva.',
                    style: tema.textTheme.bodyMedium,
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 24),
                  if (_enviado) ..._confirmacion(tema) else ..._formulario(enviando),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  List<Widget> _confirmacion(ThemeData tema) {
    return [
      Semantics(
        liveRegion: true,
        child: Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(color: tema.colorScheme.surfaceContainerHighest, borderRadius: BorderRadius.circular(12)),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(Icons.mark_email_read_outlined, color: tema.colorScheme.primary),
              const SizedBox(width: 8),
              const Expanded(child: Text(mensajeRecuperacionEnviada)),
            ],
          ),
        ),
      ),
      const SizedBox(height: 16),
      OutlinedButton(onPressed: _pedirDeNuevo, child: const Text('Enviar de nuevo')),
      const SizedBox(height: 12),
      TextButton(onPressed: () => context.go('/login'), child: const Text('Volver a iniciar sesión')),
    ];
  }

  List<Widget> _formulario(bool enviando) {
    return [
      if (_error != null) ...[
        AlertaError(error: _error!, onReintentar: enviando ? null : _enviar),
        const SizedBox(height: 16),
      ],
      Form(
        key: _formKey,
        autovalidateMode: AutovalidateMode.onUserInteraction,
        child: TextFormField(
          controller: _correo,
          keyboardType: TextInputType.emailAddress,
          autofillHints: const [AutofillHints.email],
          decoration: const InputDecoration(labelText: 'Correo electrónico'),
          validator: AuthValidators.correo,
          onFieldSubmitted: (_) => _enviar(),
        ),
      ),
      const SizedBox(height: 24),
      FilledButton(
        onPressed: enviando ? null : _enviar,
        child: enviando
            ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
            : const Text('Enviar enlace'),
      ),
      const SizedBox(height: 12),
      TextButton(
        onPressed: enviando ? null : () => context.go('/login'),
        child: const Text('Volver a iniciar sesión'),
      ),
    ];
  }
}
