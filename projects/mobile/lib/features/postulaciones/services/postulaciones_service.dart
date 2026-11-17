import 'package:dio/dio.dart';

import '../models/borrador_postulacion.dart';
import '../models/formulario_postulacion.dart';
import '../models/mi_postulacion_detalle.dart';
import '../models/pagina_postulaciones.dart';

/// Llamadas a la API de «Mis postulaciones» (CU04, lectura) y del envío de la postulación (CU-02). Recibe el [Dio] compartido de
/// `ApiClient.instance`, no crea el suyo. Los errores de negocio salen como `ApiException` dentro de un
/// [DioException] (ya lo hace el interceptor).
class PostulacionesService {
  final Dio _dio;

  PostulacionesService(this._dio);

  /// Una página de las postulaciones enviadas del usuario (la más reciente primero por defecto). Lo vacío no viaja.
  Future<PaginaPostulaciones> listarMias({
    String? gestionAcademica,
    int page = 1,
    int pageSize = 20,
    String? sort,
  }) async {
    final response = await _dio.get<Map<String, dynamic>>('/postulaciones/mias', queryParameters: {
      'page': page,
      'pageSize': pageSize,
      'filter[gestionAcademica]': ?gestionAcademica,
      'sort': ?sort,
    });
    return PaginaPostulaciones.fromJson(response.data!);
  }

  /// El detalle de una postulación propia; una ajena o inexistente da `ERR_POS_01` (404).
  Future<MiPostulacionDetalle> obtener(String codigo) async {
    final response = await _dio.get<Map<String, dynamic>>('/postulaciones/$codigo');
    return MiPostulacionDetalle.fromJson(response.data!);
  }

  /// «Nueva postulación» (CU-02): crea el borrador de la convocatoria abierta o devuelve el que ya existe (incluida
  /// una devuelta a corregir). Es idempotente. Sin convocatoria abierta da `ERR_ENV_01` (409).
  Future<FormularioPostulacion> iniciar() async {
    final response = await _dio.post<Map<String, dynamic>>('/postulaciones');
    return FormularioPostulacion.fromJson(response.data!);
  }

  /// Envía las respuestas del formulario; si la postulación ya se había enviado, devuelve el mismo comprobante.
  Future<ComprobantePostulacion> enviar(String codigo, EnviarPostulacionRequest respuestas) async {
    final response = await _dio.post<Map<String, dynamic>>('/postulaciones/$codigo/enviar', data: respuestas.toJson());
    return ComprobantePostulacion.fromJson(response.data!);
  }

  /// Guarda el avance del formulario (CU-17) sin exigir lo obligatorio; no cambia el estado de la postulación.
  Future<BorradorGuardado> guardarBorrador(String codigo, GuardarBorradorRequest borrador) async {
    final response = await _dio.put<Map<String, dynamic>>('/postulaciones/$codigo/borrador', data: borrador.toJson());
    return BorradorGuardado.fromJson(response.data!);
  }

  /// «Mi borrador» (CU-17): el borrador de la convocatoria abierta, o `null` si no hay (o la convocatoria ya cerró).
  Future<BorradorResumen?> miBorrador() async {
    final response = await _dio.get<Map<String, dynamic>?>('/postulaciones/mi-borrador');
    final datos = response.data;
    return datos == null ? null : BorradorResumen.fromJson(datos);
  }
}
