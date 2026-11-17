import 'package:dio/dio.dart';

import '../models/archivo_local.dart';
import '../models/documentos_postulacion.dart';

/// Llamadas a la API de los documentos de una postulación (CU16). Recibe el [Dio] compartido de
/// `ApiClient.instance`, no crea el suyo. Los errores de negocio salen como `ApiException` dentro de un
/// [DioException] (ya lo hace el interceptor).
///
/// * Las subidas son `multipart/form-data`: el archivo viaja como una parte y [Dio] arma el encabezado con su
///   separador. Si la sesión vence a mitad de la subida, el `ApiClient` renueva y reintenta con una copia del cuerpo.
class DocumentosPostulacionService {
  final Dio _dio;

  DocumentosPostulacionService(this._dio);

  String _base(String postulacion) => '/postulaciones/$postulacion/documentos';

  Future<MultipartFile> _parte(ArchivoLocal archivo) => MultipartFile.fromFile(
        archivo.ruta,
        filename: archivo.nombre,
        contentType: DioMediaType.parse(archivo.mimeType),
      );

  /// Los documentos que exige la convocatoria de la postulación, con su estado y sus archivos.
  Future<DocumentosPostulacion> listar(String postulacion) async {
    final response = await _dio.get<Map<String, dynamic>>(_base(postulacion));
    return DocumentosPostulacion.fromJson(response.data!);
  }

  Future<ArchivoAdjunto> adjuntar(String postulacion, String tipoDocumento, ArchivoLocal archivo) async {
    final cuerpo = FormData.fromMap({'tipoDocumento': tipoDocumento, 'archivo': await _parte(archivo)});
    final response = await _dio.post<Map<String, dynamic>>(_base(postulacion), data: cuerpo);
    return ArchivoAdjunto.fromJson(response.data!);
  }

  Future<ArchivoAdjunto> reemplazar(String postulacion, String documento, ArchivoLocal archivo) async {
    final cuerpo = FormData.fromMap({'archivo': await _parte(archivo)});
    final response = await _dio.put<Map<String, dynamic>>('${_base(postulacion)}/$documento', data: cuerpo);
    return ArchivoAdjunto.fromJson(response.data!);
  }

  Future<void> eliminar(String postulacion, String documento) async {
    await _dio.delete<dynamic>('${_base(postulacion)}/$documento');
  }
}
