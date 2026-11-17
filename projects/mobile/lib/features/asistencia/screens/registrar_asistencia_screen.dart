import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/widgets/menu_usuario.dart';
import '../formatos_asistencia.dart';
import '../models/becado_asistencia.dart';
import '../providers/asistencia_provider.dart';

/// «Registrar asistencia» (CU10, solo `PERSONAL_COMEDOR`): se ingresa el CI del becado, se muestran sus datos y el
/// estado de su beca y se confirma el registro de hoy. Una sola asistencia por becado y día: si ya estaba
/// registrada se avisa la hora y no se crea otra.
class RegistrarAsistenciaScreen extends ConsumerStatefulWidget {
  const RegistrarAsistenciaScreen({super.key});

  @override
  ConsumerState<RegistrarAsistenciaScreen> createState() => _RegistrarAsistenciaScreenState();
}

class _RegistrarAsistenciaScreenState extends ConsumerState<RegistrarAsistenciaScreen> {
  final _ci = TextEditingController();
  final _formKey = GlobalKey<FormState>();

  @override
  void dispose() {
    _ci.dispose();
    super.dispose();
  }

  void _buscar() {
    if (!_formKey.currentState!.validate()) return;
    FocusScope.of(context).unfocus();
    ref.read(asistenciaProvider.notifier).buscar(_ci.text);
  }

  void _siguiente() {
    _ci.clear();
    ref.read(asistenciaProvider.notifier).siguiente();
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(asistenciaProvider);
    final tema = Theme.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text('Comedor · Asistencia'), actions: const [MenuUsuario()]),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Text('Registrar asistencia', style: tema.textTheme.headlineSmall),
            const SizedBox(height: 4),
            Text('Hoy, ${formatearFechaAsistencia(DateTime.now())}', style: tema.textTheme.bodyMedium),
            const SizedBox(height: 16),
            Form(
              key: _formKey,
              child: TextFormField(
                controller: _ci,
                enabled: !estado.ocupado,
                keyboardType: TextInputType.text,
                textInputAction: TextInputAction.search,
                inputFormatters: [LengthLimitingTextInputFormatter(20)],
                decoration: const InputDecoration(labelText: 'CI del becado', hintText: 'Ingrese CI...'),
                validator: (valor) => (valor == null || valor.trim().isEmpty) ? 'El CI es obligatorio' : null,
                onFieldSubmitted: (_) => _buscar(),
              ),
            ),
            const SizedBox(height: 12),
            Align(
              alignment: Alignment.centerLeft,
              child: FilledButton(
                onPressed: estado.ocupado ? null : _buscar,
                child: estado.ocupado && estado.becado == null
                    ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                    : const Text('Buscar'),
              ),
            ),
            if (estado.error != null) ...[
              const SizedBox(height: 16),
              _Aviso(texto: estado.error!.mensaje, error: true),
            ],
            if (estado.becado != null) ...[
              const SizedBox(height: 16),
              _TarjetaBecado(
                becado: estado.becado!,
                ocupado: estado.ocupado,
                recienRegistrada: estado.recienRegistrada,
                onRegistrar: () => ref.read(asistenciaProvider.notifier).registrar(),
                onSiguiente: _siguiente,
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _TarjetaBecado extends StatelessWidget {
  final BecadoAsistencia becado;
  final bool ocupado;
  final bool recienRegistrada;
  final VoidCallback onRegistrar;
  final VoidCallback onSiguiente;

  const _TarjetaBecado({
    required this.becado,
    required this.ocupado,
    required this.recienRegistrada,
    required this.onRegistrar,
    required this.onSiguiente,
  });

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final hora = becado.horaRegistro;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(becado.nombreCompleto, style: tema.textTheme.titleMedium),
            const SizedBox(height: 2),
            Text('CI ${becado.ci} · ${becado.carrera}', style: tema.textTheme.bodyMedium),
            Text(becado.convocatoria, style: tema.textTheme.bodySmall),
            const SizedBox(height: 8),
            Chip(
              label: Text(_etiquetaEstado(becado.estadoBeca)),
              backgroundColor: becado.vigente ? tema.colorScheme.secondaryContainer : tema.colorScheme.errorContainer,
            ),
            const SizedBox(height: 12),
            if (!becado.vigente)
              _Aviso(texto: 'La beca está ${_etiquetaEstado(becado.estadoBeca).toLowerCase()}: no se puede registrar la asistencia.', error: true)
            else if (hora != null)
              _Aviso(
                texto: recienRegistrada
                    ? 'Asistencia registrada a las ${formatearHoraAsistencia(hora)}.'
                    : 'Asistencia ya registrada hoy a las ${formatearHoraAsistencia(hora)}.',
                error: false,
              ),
            const SizedBox(height: 12),
            Row(
              children: [
                if (becado.vigente && hora == null)
                  FilledButton.icon(
                    onPressed: ocupado ? null : onRegistrar,
                    icon: ocupado
                        ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                        : const Icon(Icons.check),
                    label: const Text('Registrar asistencia'),
                  )
                else
                  OutlinedButton(onPressed: onSiguiente, child: const Text('Siguiente becado')),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

String _etiquetaEstado(String estado) {
  switch (estado) {
    case 'VIGENTE':
      return 'Beca vigente';
    case 'SUSPENDIDA':
      return 'Beca suspendida';
    case 'BAJA':
      return 'Beca dada de baja';
    default:
      return estado;
  }
}

/// Aviso en línea: rojo para un error o una beca que no registra, amarillo/neutro para lo informativo.
class _Aviso extends StatelessWidget {
  final String texto;
  final bool error;

  const _Aviso({required this.texto, required this.error});

  @override
  Widget build(BuildContext context) {
    final colores = Theme.of(context).colorScheme;
    return Semantics(
      liveRegion: true,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: error ? colores.errorContainer : colores.tertiaryContainer,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Text(texto, style: TextStyle(color: error ? colores.onErrorContainer : colores.onTertiaryContainer)),
      ),
    );
  }
}
