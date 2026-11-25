import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../shared/widgets/campo_formulario.dart';
import '../envio_error_mapper.dart';
import '../formatos.dart';
import '../models/formulario_postulacion.dart';
import '../providers/borrador_provider.dart';
import '../providers/documentos_postulacion_provider.dart';
import '../providers/mis_postulaciones_provider.dart';
import '../providers/postulaciones_servicios_provider.dart';
import '../validacion_formulario.dart';
import '../widgets/formulario_oficial.dart';

/// «Nueva postulación» (CU-02): paso 1 datos del perfil, paso 2 formulario socioeconómico y paso 3 documentos (CU16) y
/// envío con confirmación. Al abrirse crea (o recupera) la postulación en edición; una devuelta se abre con sus
/// respuestas para corregirla y reenviarla (A5). Muestra el comprobante al terminar.
class PostularScreen extends ConsumerStatefulWidget {
  const PostularScreen({super.key});

  @override
  ConsumerState<PostularScreen> createState() => _PostularScreenState();
}

class _PostularScreenState extends ConsumerState<PostularScreen> {
  static const _pasos = [
    'Datos del postulante',
    'Datos socioeconómicos',
    'Documentos y envío',
  ];

  final _formKey = GlobalKey<FormState>();
  final _ingresos = TextEditingController();
  final _dependientes = TextEditingController();
  final _promedio = TextEditingController();
  final _oficial = FormularioOficialController();

  FormularioPostulacion? _formulario;
  ErrorEnvio? _errorInicio;
  ErrorEnvio? _errorEnvio;
  ComprobantePostulacion? _comprobante;
  String? _procedencia;
  String? _patrimonio;
  String? _vulnerabilidad;
  int _paso = 0;
  bool _enviando = false;

  // Borrador (CU-17): cuándo se guardó por última vez, si hay un guardado en curso y los datos mal escritos.
  DateTime? _guardadoEn;
  bool _guardando = false;
  List<String> _erroresBorrador = const [];

  @override
  void initState() {
    super.initState();
    _abrir();
  }

  @override
  void dispose() {
    _ingresos.dispose();
    _dependientes.dispose();
    _promedio.dispose();
    _oficial.dispose();
    super.dispose();
  }

  Future<void> _abrir() async {
    setState(() => _errorInicio = null);
    try {
      final formulario = await ref.read(postulacionesServiceProvider).iniciar();
      if (!mounted) return;
      final valores = ValoresFormulario.desde(formulario.respuesta);
      setState(() {
        _formulario = formulario;
        _guardadoEn = formulario.respuesta?.guardadoEn;
        _ingresos.text = valores.ingresosFamiliares;
        _dependientes.text = valores.dependientes;
        _promedio.text = valores.promedioAcademico;
        _procedencia = valores.procedencia;
        _patrimonio = valores.patrimonio;
        _vulnerabilidad = valores.estadoVulnerabilidad;
      });
      _oficial.cargar(valores.complementarios);
    } catch (e) {
      if (!mounted) return;
      setState(() => _errorInicio = mapearErrorEnvio(e));
    }
  }

  ValoresFormulario get _valores => ValoresFormulario(
    ingresosFamiliares: _ingresos.text,
    dependientes: _dependientes.text,
    procedencia: _procedencia,
    patrimonio: _patrimonio,
    estadoVulnerabilidad: _vulnerabilidad,
    promedioAcademico: _promedio.text,
    complementarios: _oficial.valores,
  );

  void _continuarDelFormulario() {
    if (_formKey.currentState!.validate()) {
      setState(() {
        _errorEnvio = null;
        _paso = 2;
      });
    }
  }

  /// «Guardar borrador» (CU-17): guarda lo que hay escrito sin exigir los campos vacíos; solo rechaza un valor mal
  /// escrito. No envía ni cambia de paso.
  Future<void> _guardarBorrador(FormularioPostulacion formulario) async {
    if (_guardando) return;
    final resultado = _valores.aBorrador();
    if (!resultado.esValido) {
      setState(() => _erroresBorrador = resultado.errores);
      return;
    }
    setState(() {
      _guardando = true;
      _erroresBorrador = const [];
      _errorEnvio = null;
    });
    try {
      final guardado = await ref
          .read(postulacionesServiceProvider)
          .guardarBorrador(formulario.postulacion, resultado.borrador!);
      ref.invalidate(miBorradorProvider);
      if (!mounted) return;
      setState(() => _guardadoEn = guardado.guardadoEn);
    } catch (e) {
      if (!mounted) return;
      setState(() => _errorEnvio = mapearErrorEnvio(e));
    } finally {
      if (mounted) setState(() => _guardando = false);
    }
  }

  Future<void> _confirmarYEnviar(FormularioPostulacion formulario) async {
    if (_enviando) return;
    final valores = _valores;
    if (!valores.esValido) {
      setState(() => _paso = 1);
      return;
    }
    final confirmado = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('¿Enviar tu postulación?'),
        content: const Text(
          'Una vez enviada no podrás editarla, salvo que la DUBSS la devuelva para corrección. '
          'Su estado será pendiente.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Confirmar envío'),
          ),
        ],
      ),
    );
    if (confirmado != true || !mounted) return;

    // Evita el doble envío aunque el segundo toque llegue antes de que se repinte el botón (A6).
    setState(() {
      _enviando = true;
      _errorEnvio = null;
    });
    try {
      final comprobante = await ref
          .read(postulacionesServiceProvider)
          .enviar(formulario.postulacion, valores.aRequest());
      ref.invalidate(misPostulacionesListProvider);
      ref.invalidate(postulacionDetailProvider(formulario.postulacion));
      if (!mounted) return;
      setState(() => _comprobante = comprobante);
    } catch (e) {
      if (!mounted) return;
      setState(() => _errorEnvio = mapearErrorEnvio(e));
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Nueva postulación'),
        leading: BackButton(
          onPressed: () =>
              context.canPop() ? context.pop() : context.go('/estudiante'),
        ),
      ),
      body: SafeArea(child: _cuerpo(context)),
    );
  }

  Widget _cuerpo(BuildContext context) {
    final error = _errorInicio;
    if (error != null) return _ErrorInicio(error: error, onReintentar: _abrir);
    final formulario = _formulario;
    if (formulario == null) {
      return const Center(child: CircularProgressIndicator());
    }
    final comprobante = _comprobante;
    if (comprobante != null) {
      return _Comprobante(
        comprobante: comprobante,
        onVer: () => context.go('/mis-postulaciones/${comprobante.codigo}'),
      );
    }

    // * SingleChildScrollView + Column (no un ListView perezoso): el formulario es largo y un campo fuera de la
    // * pantalla no se construiría, así que `Form.validate()` no lo revisaría.
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              for (var i = 0; i < _pasos.length; i++)
                Expanded(
                  child: Container(
                    height: 6,
                    margin: EdgeInsets.only(
                      right: i == _pasos.length - 1 ? 0 : 8,
                    ),
                    decoration: BoxDecoration(
                      color: i <= _paso
                          ? Theme.of(context).colorScheme.primary
                          : Theme.of(context).colorScheme.outlineVariant,
                      borderRadius: BorderRadius.circular(3),
                    ),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            'Paso ${_paso + 1} de ${_pasos.length} · ${_pasos[_paso]}',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: 12),
          if (formulario.esReenvio)
            const _Aviso(
              'Esta postulación fue devuelta para corregirla. Revisa tus datos y documentos, y vuelve a enviarla.',
            ),
          if (_errorEnvio != null) _Aviso(_errorEnvio!.mensaje, error: true),
          if (_erroresBorrador.isNotEmpty && _paso == 1)
            _Aviso(
              'No se guardó el borrador. Corrige estos datos:\n${_erroresBorrador.map((e) => "• $e").join("\n")}',
              error: true,
            ),
          if (_guardadoEn != null && _paso > 0)
            _Aviso('✓ Borrador guardado · ${formatearHora(_guardadoEn!)}'),
          if (_paso == 0) _pasoPerfil(formulario),
          if (_paso == 1) _pasoFormulario(formulario),
          if (_paso == 2) _pasoDocumentos(formulario),
        ],
      ),
    );
  }

  Widget _pasoPerfil(FormularioPostulacion f) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _Dato('Nombre', '${f.perfil.nombres} ${f.perfil.apellidos}'),
                _Dato('CI', f.perfil.ci),
                _Dato('Carrera', f.perfil.carrera),
                _Dato(
                  'Convocatoria',
                  '${f.convocatoria.nombre} · gestión ${f.convocatoria.gestion}',
                ),
                _Dato('Cierra el', formatearFecha(f.convocatoria.fechaCierre)),
              ],
            ),
          ),
        ),
        const SizedBox(height: 8),
        const Text(
          'Estos datos salen de tu perfil. Si alguno está mal, corrígelo desde tu perfil antes de enviar.',
        ),
        const SizedBox(height: 16),
        FilledButton(
          onPressed: () => setState(() => _paso = 1),
          child: const Text('Continuar'),
        ),
      ],
    );
  }

  Widget _pasoFormulario(FormularioPostulacion f) {
    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          CampoFormulario(
            controller: _ingresos,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: const InputDecoration(
              labelText: 'Ingresos familiares mensuales (Bs) *',
            ),
            validator: validarIngresos,
          ),
          const SizedBox(height: 12),
          CampoFormulario(
            controller: _dependientes,
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(
              labelText: 'N.º de dependientes *',
            ),
            validator: validarDependientes,
          ),
          const SizedBox(height: 12),
          _lista(
            'Procedencia *',
            _procedencia,
            f.opciones.procedencias,
            (v) => _procedencia = v,
          ),
          const SizedBox(height: 12),
          _lista(
            'Patrimonio *',
            _patrimonio,
            f.opciones.patrimonios,
            (v) => _patrimonio = v,
          ),
          const SizedBox(height: 12),
          _lista(
            'Estado de vulnerabilidad *',
            _vulnerabilidad,
            f.opciones.estadosVulnerabilidad,
            (v) => _vulnerabilidad = v,
          ),
          const SizedBox(height: 12),
          CampoFormulario(
            controller: _promedio,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: const InputDecoration(
              labelText: 'Promedio académico *',
              hintText: 'Ej. 72',
            ),
            validator: validarPromedio,
          ),
          FormularioOficial(controlador: _oficial, opciones: f.opciones),
          const SizedBox(height: 16),
          const Text(
            'Puedes salir y continuar después; los campos pendientes no bloquean el borrador.',
          ),
          const SizedBox(height: 12),
          OutlinedButton(
            onPressed: _guardando ? null : () => _guardarBorrador(f),
            child: Text(_guardando ? 'Guardando…' : 'Guardar borrador'),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: () => setState(() => _paso = 0),
                  child: const Text('Atrás'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: FilledButton(
                  onPressed: _continuarDelFormulario,
                  child: const Text('Continuar'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _lista(
    String etiqueta,
    String? valor,
    List<String> opciones,
    void Function(String?) alCambiar,
  ) {
    return DropdownButtonFormField<String>(
      initialValue: opciones.contains(valor) ? valor : null,
      isExpanded: true,
      decoration: InputDecoration(labelText: etiqueta),
      items: [
        for (final o in opciones) DropdownMenuItem(value: o, child: Text(o)),
      ],
      validator: validarOpcion,
      autovalidateMode: AutovalidateMode.onUserInteraction,
      onChanged: (v) => setState(() => alCambiar(v)),
    );
  }

  Widget _pasoDocumentos(FormularioPostulacion f) {
    final documentos = ref.watch(documentosPostulacionProvider(f.postulacion));
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        documentos.when(
          loading: () => const Padding(
            padding: EdgeInsets.all(16),
            child: Center(child: CircularProgressIndicator()),
          ),
          error: (e, _) => _Aviso(
            'No se pudieron cargar los documentos. Inténtalo de nuevo.',
            error: true,
          ),
          data: (datos) => Card(
            child: Column(
              children: [
                for (final d in datos.requeridos)
                  ListTile(
                    title: Text(d.nombre),
                    subtitle: Text(d.obligatorio ? 'Obligatorio' : 'Opcional'),
                    trailing: Chip(
                      label: Text(d.adjuntado ? 'Adjuntado' : 'Falta'),
                    ),
                  ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 8),
        OutlinedButton.icon(
          onPressed: () =>
              context.push('/mis-postulaciones/${f.postulacion}/documentos'),
          icon: const Icon(Icons.attach_file),
          label: const Text('Adjuntar documentos'),
        ),
        const SizedBox(height: 8),
        OutlinedButton(
          onPressed: _guardando ? null : () => _guardarBorrador(f),
          child: Text(_guardando ? 'Guardando…' : 'Guardar borrador'),
        ),
        const SizedBox(height: 16),
        Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed: () => setState(() => _paso = 1),
                child: const Text('Atrás'),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: FilledButton(
                onPressed: _enviando ? null : () => _confirmarYEnviar(f),
                child: Text(_enviando ? 'Enviando…' : 'Enviar postulación'),
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _Dato extends StatelessWidget {
  final String etiqueta;
  final String valor;

  const _Dato(this.etiqueta, this.valor);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 110,
            child: Text(etiqueta, style: Theme.of(context).textTheme.bodySmall),
          ),
          Expanded(child: Text(valor)),
        ],
      ),
    );
  }
}

class _Aviso extends StatelessWidget {
  final String mensaje;
  final bool error;

  const _Aviso(this.mensaje, {this.error = false});

  @override
  Widget build(BuildContext context) {
    final colores = Theme.of(context).colorScheme;
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: error ? colores.errorContainer : colores.secondaryContainer,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        mensaje,
        style: TextStyle(
          color: error
              ? colores.onErrorContainer
              : colores.onSecondaryContainer,
        ),
      ),
    );
  }
}

class _ErrorInicio extends StatelessWidget {
  final ErrorEnvio error;
  final VoidCallback onReintentar;

  const _ErrorInicio({required this.error, required this.onReintentar});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _Aviso(error.mensaje, error: !error.sinConvocatoria),
            if (error.red)
              FilledButton(
                onPressed: onReintentar,
                child: const Text('Reintentar'),
              ),
            TextButton(
              onPressed: () => context.go('/estudiante'),
              child: const Text('Ir a mis postulaciones'),
            ),
          ],
        ),
      ),
    );
  }
}

class _Comprobante extends StatelessWidget {
  final ComprobantePostulacion comprobante;
  final VoidCallback onVer;

  const _Comprobante({required this.comprobante, required this.onVer});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.check_circle,
              size: 56,
              color: Theme.of(context).colorScheme.primary,
            ),
            const SizedBox(height: 12),
            Text(
              comprobante.reenvio
                  ? 'Postulación reenviada'
                  : 'Postulación enviada',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 8),
            Text(
              'Recibimos tu postulación. Su estado es ${comprobante.estado}; puedes seguirla en «Mis postulaciones».',
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 16),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _Dato('Código', comprobante.codigo),
                    _Dato('Convocatoria', comprobante.convocatoria),
                    _Dato('Gestión', comprobante.gestion),
                    _Dato(
                      'Enviada el',
                      formatearFechaHora(comprobante.fechaEnvio),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: onVer,
              child: const Text('Ver mi postulación'),
            ),
          ],
        ),
      ),
    );
  }
}
