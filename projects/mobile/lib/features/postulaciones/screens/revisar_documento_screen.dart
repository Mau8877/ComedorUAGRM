import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../documentos_error_mapper.dart';
import '../formatos_documentos.dart';
import '../models/archivo_local.dart';
import '../models/revision_documento.dart';
import '../providers/documentos_postulacion_provider.dart';
import '../services/captura_documento_service.dart';
import '../widgets/alerta_documentos.dart';

/// «Revisar y adjuntar» (CU16, mockup img 141, pantalla 2): vista previa del archivo, recorte y giro si es una imagen,
/// «Repetir» para volver a elegirlo y «Adjuntar documento» para subirlo. Valida formato y tamaño con las reglas del
/// backend antes de subir. Si la subida falla, el archivo se conserva y se puede reintentar sin perder los demás
/// documentos (A4).
class RevisarDocumentoScreen extends ConsumerStatefulWidget {
  final RevisionDocumento revision;

  const RevisarDocumentoScreen({super.key, required this.revision});

  @override
  ConsumerState<RevisarDocumentoScreen> createState() => _RevisarDocumentoScreenState();
}

class _RevisarDocumentoScreenState extends ConsumerState<RevisarDocumentoScreen> {
  late ArchivoLocal _archivo = widget.revision.archivo;
  ErrorDocumentos? _error;
  bool _subiendo = false;

  RevisionDocumento get _revision => widget.revision;

  String? get _problemaLocal => validarArchivoLocal(
        mimeType: _archivo.mimeType,
        tamanoBytes: _archivo.tamanoBytes,
        maximoBytes: _revision.reglas.tamanoMaximoBytes,
        formatos: _revision.reglas.formatosPermitidos,
      );

  Future<void> _recortar() async {
    final recortada = await ref.read(capturaDocumentoServiceProvider).recortar(_archivo);
    if (recortada != null && mounted) {
      setState(() {
        _archivo = recortada;
        _error = null;
      });
    }
  }

  Future<void> _repetir() async {
    final captura = ref.read(capturaDocumentoServiceProvider);
    try {
      final nuevo = switch (_revision.fuente) {
        FuenteDocumento.camara => await captura.tomarFoto(),
        FuenteDocumento.galeria => await captura.elegirDeGaleria(),
        FuenteDocumento.pdf => await captura.elegirPdf(),
      };
      if (nuevo != null && mounted) {
        setState(() {
          _archivo = nuevo;
          _error = null;
        });
      }
    } on PermisoCamaraDenegado {
      if (!mounted) return;
      setState(() => _error = const ErrorDocumentos('Sin permiso de la cámara. Vuelve atrás y elige desde tu galería.'));
    }
  }

  Future<void> _adjuntar() async {
    if (_subiendo || _problemaLocal != null) return;
    setState(() {
      _subiendo = true;
      _error = null;
    });
    final acciones = ref.read(documentosPostulacionAccionesProvider(_revision.postulacion));
    final mensajero = ScaffoldMessenger.of(context);
    final router = GoRouter.of(context);
    try {
      final reemplaza = _revision.reemplaza;
      if (reemplaza != null) {
        await acciones.reemplazar(reemplaza.codigo, _archivo);
      } else {
        await acciones.adjuntar(_revision.requerido.tipoDocumentoCodigo, _archivo);
      }
      mensajero.showSnackBar(SnackBar(
        content: Text(reemplaza != null ? 'Documento reemplazado' : 'Documento adjuntado'),
      ));
      if (router.canPop()) router.pop();
    } catch (e) {
      final error = mapearErrorDocumentos(e);
      if (!mounted) return;
      if (error.refrescar || error.noEditable) {
        // La postulación cambió: se avisa y se vuelve a la lista, que ya se actualizó.
        mensajero.showSnackBar(SnackBar(content: Text(error.mensaje)));
        if (router.canPop()) router.pop();
        return;
      }
      setState(() {
        _subiendo = false;
        _error = error;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    final problema = _problemaLocal;

    return Scaffold(
      appBar: AppBar(title: const Text('Revisar y adjuntar')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text(_revision.requerido.nombre, style: texto.titleMedium),
          const SizedBox(height: 12),
          _VistaPrevia(archivo: _archivo),
          const SizedBox(height: 4),
          Text('${_archivo.nombre} · ${formatearTamano(_archivo.tamanoBytes)}', style: texto.bodySmall),
          const SizedBox(height: 12),
          Row(
            children: [
              if (_archivo.esImagen) ...[
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: _subiendo ? null : _recortar,
                    icon: const Icon(Icons.crop_rotate),
                    label: const Text('Recortar o girar'),
                  ),
                ),
                const SizedBox(width: 8),
              ],
              Expanded(
                child: OutlinedButton(
                  onPressed: _subiendo ? null : _repetir,
                  child: Text(_revision.fuente == FuenteDocumento.camara ? 'Repetir foto' : 'Elegir otro'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          if (problema != null) AlertaDocumentos(error: ErrorDocumentos(problema)),
          if (_error != null) AlertaDocumentos(error: _error!, onReintentar: _subiendo ? null : _adjuntar),
          const SizedBox(height: 16),
          FilledButton(
            onPressed: _subiendo || problema != null ? null : _adjuntar,
            child: _subiendo
                ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                : Text(_revision.reemplaza != null ? 'Reemplazar documento' : 'Adjuntar documento'),
          ),
        ],
      ),
    );
  }
}

class _VistaPrevia extends StatelessWidget {
  final ArchivoLocal archivo;

  const _VistaPrevia({required this.archivo});

  @override
  Widget build(BuildContext context) {
    final colores = Theme.of(context).colorScheme;

    return Container(
      height: 260,
      decoration: BoxDecoration(color: colores.surfaceContainerHighest, borderRadius: BorderRadius.circular(12)),
      clipBehavior: Clip.antiAlias,
      child: archivo.esImagen
          ? Image.file(
              File(archivo.ruta),
              fit: BoxFit.contain,
              semanticLabel: 'Vista previa de ${archivo.nombre}',
              errorBuilder: (_, _, _) => const Center(child: Icon(Icons.broken_image_outlined, size: 48)),
            )
          : const Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.picture_as_pdf_outlined, size: 56),
                  SizedBox(height: 8),
                  Text('Vista previa no disponible para PDF'),
                ],
              ),
            ),
    );
  }
}
