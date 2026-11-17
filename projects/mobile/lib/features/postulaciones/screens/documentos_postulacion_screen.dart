import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../documentos_error_mapper.dart';
import '../formatos.dart';
import '../formatos_documentos.dart';
import '../models/archivo_local.dart';
import '../models/documentos_postulacion.dart';
import '../models/revision_documento.dart';
import '../providers/documentos_postulacion_provider.dart';
import '../services/captura_documento_service.dart';
import '../widgets/alerta_documentos.dart';

/// Los documentos de una postulación (CU16): los que exige la convocatoria, cuáles ya se adjuntaron y cuáles faltan.
/// Mientras la postulación se pueda modificar (borrador o devuelta, convocatoria abierta) permite adjuntar un archivo
/// (cámara, galería o PDF), reemplazarlo y eliminarlo; si no, solo mostrar. Elegir un archivo lleva a «Revisar y
/// adjuntar» ([RevisarDocumentoScreen]).
class DocumentosPostulacionScreen extends ConsumerWidget {
  /// El código público de la postulación, de la ruta.
  final String codigo;

  const DocumentosPostulacionScreen({super.key, required this.codigo});

  void _volver(BuildContext context) {
    if (context.canPop()) {
      context.pop();
    } else {
      context.go('/mis-postulaciones/$codigo');
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final documentos = ref.watch(documentosPostulacionProvider(codigo));

    return Scaffold(
      appBar: AppBar(
        title: const Text('Documentos'),
        leading: BackButton(onPressed: () => _volver(context)),
      ),
      body: documentos.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) {
          final error = mapearErrorDocumentos(e);
          return Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: AlertaDocumentos(
                error: error.refrescar ? const ErrorDocumentos('La postulación no existe o no es tuya') : error,
                onReintentar: () => ref.invalidate(documentosPostulacionProvider(codigo)),
              ),
            ),
          );
        },
        data: (datos) => _Contenido(codigo: codigo, documentos: datos),
      ),
    );
  }
}

enum _Accion { reemplazar, eliminar }

class _Contenido extends ConsumerWidget {
  final String codigo;
  final DocumentosPostulacion documentos;

  const _Contenido({required this.codigo, required this.documentos});

  Future<void> _elegirArchivo(
    BuildContext context,
    WidgetRef ref,
    DocumentoRequerido requerido, {
    ArchivoAdjunto? reemplaza,
  }) async {
    final fuente = await showModalBottomSheet<FuenteDocumento>(
      context: context,
      builder: (_) => const _HojaDeFuentes(),
    );
    if (fuente == null || !context.mounted) return;

    final captura = ref.read(capturaDocumentoServiceProvider);
    ArchivoLocal? archivo;
    try {
      archivo = switch (fuente) {
        FuenteDocumento.camara => await captura.tomarFoto(),
        FuenteDocumento.galeria => await captura.elegirDeGaleria(),
        FuenteDocumento.pdf => await captura.elegirPdf(),
      };
    } on PermisoCamaraDenegado {
      // A1: sin permiso de cámara se ofrece elegir el documento de la galería o de los archivos.
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Sin permiso de la cámara. Elige el documento desde tu galería o tus archivos.')),
      );
      return _elegirArchivo(context, ref, requerido, reemplaza: reemplaza);
    }
    if (archivo == null || !context.mounted) return;

    await context.push<void>(
      '/mis-postulaciones/$codigo/documentos/revisar',
      extra: RevisionDocumento(
        postulacion: codigo,
        requerido: requerido,
        reglas: documentos.reglas,
        fuente: fuente,
        archivo: archivo,
        reemplaza: reemplaza,
      ),
    );
  }

  Future<void> _eliminar(BuildContext context, WidgetRef ref, DocumentoRequerido requerido, ArchivoAdjunto archivo) async {
    final confirmado = await showDialog<bool>(
      context: context,
      builder: (dialogo) => AlertDialog(
        title: const Text('Eliminar documento'),
        content: Text('Se eliminará «${archivo.nombreArchivo}» de ${requerido.nombre}. Esta acción no se puede deshacer.'),
        actions: [
          TextButton(onPressed: () => Navigator.of(dialogo).pop(false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.of(dialogo).pop(true), child: const Text('Eliminar')),
        ],
      ),
    );
    if (confirmado != true || !context.mounted) return;

    final mensajero = ScaffoldMessenger.of(context);
    try {
      await ref.read(documentosPostulacionAccionesProvider(codigo)).eliminar(archivo.codigo);
      mensajero.showSnackBar(SnackBar(content: Text('Se eliminó ${requerido.nombre}')));
    } catch (e) {
      mensajero.showSnackBar(SnackBar(content: Text(mapearErrorDocumentos(e).mensaje)));
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final texto = Theme.of(context).textTheme;
    final reglas = documentos.reglas;

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text('Adjuntos (${documentos.adjuntados} de ${documentos.requeridos.length})', style: texto.titleMedium),
        const SizedBox(height: 4),
        Text(
          'Hasta ${reglas.maximoArchivosPorTipo} archivos por documento · '
          '${nombresDeFormatos(reglas.formatosPermitidos)}, hasta ${formatearTamano(reglas.tamanoMaximoBytes)}',
          style: texto.bodySmall,
        ),
        if (!documentos.puedeEditar) ...[
          const SizedBox(height: 12),
          Semantics(
            liveRegion: true,
            child: Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.surfaceContainerHighest,
                borderRadius: BorderRadius.circular(12),
              ),
              child: const Text('Esta postulación ya no se puede modificar: solo puedes ver tus documentos.'),
            ),
          ),
        ],
        if (documentos.requeridos.isEmpty)
          const Padding(padding: EdgeInsets.all(24), child: Center(child: Text('La convocatoria no exige documentos.'))),
        for (final requerido in documentos.requeridos)
          Padding(
            padding: const EdgeInsets.only(top: 12),
            child: _TarjetaDocumento(
              requerido: requerido,
              puedeEditar: documentos.puedeEditar,
              puedeAgregar: requerido.archivos.length < reglas.maximoArchivosPorTipo,
              onAdjuntar: () => _elegirArchivo(context, ref, requerido),
              onAccion: (archivo, accion) => switch (accion) {
                _Accion.reemplazar => _elegirArchivo(context, ref, requerido, reemplaza: archivo),
                _Accion.eliminar => _eliminar(context, ref, requerido, archivo),
              },
            ),
          ),
      ],
    );
  }
}

class _TarjetaDocumento extends StatelessWidget {
  final DocumentoRequerido requerido;
  final bool puedeEditar;
  final bool puedeAgregar;
  final VoidCallback onAdjuntar;
  final void Function(ArchivoAdjunto archivo, _Accion accion) onAccion;

  const _TarjetaDocumento({
    required this.requerido,
    required this.puedeEditar,
    required this.puedeAgregar,
    required this.onAdjuntar,
    required this.onAccion,
  });

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    final colores = Theme.of(context).colorScheme;
    final falta = !requerido.adjuntado && requerido.obligatorio;

    return Card(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: falta ? colores.error : colores.outlineVariant),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(requerido.nombre, style: texto.titleSmall),
                      if (requerido.descripcion != null) Text(requerido.descripcion!, style: texto.bodySmall),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                _EstadoDocumento(requerido: requerido),
              ],
            ),
            for (final archivo in requerido.archivos)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Row(
                  children: [
                    const Icon(Icons.description_outlined, size: 20),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(archivo.nombreArchivo, overflow: TextOverflow.ellipsis),
                          Text(
                            '${formatearTamano(archivo.tamanoBytes)} · ${formatearFecha(archivo.subidoEn)}',
                            style: texto.bodySmall,
                          ),
                        ],
                      ),
                    ),
                    if (puedeEditar) ...[
                      IconButton(
                        tooltip: 'Reemplazar ${archivo.nombreArchivo}',
                        icon: const Icon(Icons.sync),
                        onPressed: () => onAccion(archivo, _Accion.reemplazar),
                      ),
                      IconButton(
                        tooltip: 'Eliminar ${archivo.nombreArchivo}',
                        icon: const Icon(Icons.delete_outline),
                        onPressed: () => onAccion(archivo, _Accion.eliminar),
                      ),
                    ],
                  ],
                ),
              ),
            if (puedeEditar && puedeAgregar)
              Padding(
                padding: const EdgeInsets.only(top: 12),
                child: OutlinedButton.icon(
                  onPressed: onAdjuntar,
                  icon: const Icon(Icons.add),
                  label: Text(requerido.adjuntado ? 'Agregar otro archivo' : 'Adjuntar'),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _EstadoDocumento extends StatelessWidget {
  final DocumentoRequerido requerido;

  const _EstadoDocumento({required this.requerido});

  @override
  Widget build(BuildContext context) {
    final colores = Theme.of(context).colorScheme;
    final (etiqueta, fondo, color) = requerido.adjuntado
        ? ('Adjuntado', colores.primaryContainer, colores.onPrimaryContainer)
        : requerido.obligatorio
            ? ('Falta', colores.errorContainer, colores.onErrorContainer)
            : ('Opcional', colores.surfaceContainerHighest, colores.onSurfaceVariant);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(color: fondo, borderRadius: BorderRadius.circular(12)),
      child: Text(etiqueta, style: TextStyle(color: color, fontSize: 12, fontWeight: FontWeight.w600)),
    );
  }
}

class _HojaDeFuentes extends StatelessWidget {
  const _HojaDeFuentes();

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          ListTile(
            leading: const Icon(Icons.photo_camera_outlined),
            title: const Text('Tomar una foto'),
            onTap: () => Navigator.of(context).pop(FuenteDocumento.camara),
          ),
          ListTile(
            leading: const Icon(Icons.photo_library_outlined),
            title: const Text('Elegir de la galería'),
            onTap: () => Navigator.of(context).pop(FuenteDocumento.galeria),
          ),
          ListTile(
            leading: const Icon(Icons.picture_as_pdf_outlined),
            title: const Text('Elegir un archivo PDF'),
            onTap: () => Navigator.of(context).pop(FuenteDocumento.pdf),
          ),
        ],
      ),
    );
  }
}
