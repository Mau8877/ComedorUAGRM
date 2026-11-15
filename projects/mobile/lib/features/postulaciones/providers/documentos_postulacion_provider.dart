import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../models/archivo_local.dart';
import '../models/documentos_postulacion.dart';
import '../services/captura_documento_service.dart';
import '../services/documentos_postulacion_service.dart';

/// Servicio de la API de documentos de la postulación (los tests lo sustituyen con `overrideWithValue`).
final documentosPostulacionServiceProvider =
    Provider<DocumentosPostulacionService>((ref) => DocumentosPostulacionService(ApiClient.instance));

/// Cámara, galería, archivos y recorte del teléfono (los tests lo sustituyen: los plugins no corren en `flutter test`).
final capturaDocumentoServiceProvider = Provider<CapturaDocumentoService>((ref) => CapturaDocumentoService());

/// Los documentos que exige la convocatoria de una postulación y su estado (CU16). Se descarta al salir de la pantalla.
///
/// * El parámetro es el código público de la postulación.
final documentosPostulacionProvider = FutureProvider.autoDispose.family<DocumentosPostulacion, String>(
  (ref, postulacion) => ref.watch(documentosPostulacionServiceProvider).listar(postulacion),
);

/// Adjuntar, reemplazar y eliminar documentos de una postulación. Después de cada cambio vuelve a pedir la lista.
class DocumentosPostulacionAcciones {
  final Ref _ref;
  final String postulacion;

  DocumentosPostulacionAcciones(this._ref, this.postulacion);

  DocumentosPostulacionService get _servicio => _ref.read(documentosPostulacionServiceProvider);

  void _refrescar() => _ref.invalidate(documentosPostulacionProvider(postulacion));

  Future<void> adjuntar(String tipoDocumento, ArchivoLocal archivo) async {
    try {
      await _servicio.adjuntar(postulacion, tipoDocumento, archivo);
    } finally {
      // También al fallar: si la postulación cambió (ya se envió, la convocatoria cerró), la lista debe reflejarlo.
      _refrescar();
    }
  }

  Future<void> reemplazar(String documento, ArchivoLocal archivo) async {
    try {
      await _servicio.reemplazar(postulacion, documento, archivo);
    } finally {
      _refrescar();
    }
  }

  Future<void> eliminar(String documento) async {
    try {
      await _servicio.eliminar(postulacion, documento);
    } finally {
      _refrescar();
    }
  }
}

final documentosPostulacionAccionesProvider = Provider.autoDispose.family<DocumentosPostulacionAcciones, String>(
  (ref, postulacion) => DocumentosPostulacionAcciones(ref, postulacion),
);
