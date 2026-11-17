import 'dart:io';

import 'package:comedor_uagrm_mobile/core/network/api_client.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/documentos_error_mapper.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/formatos_documentos.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/models/archivo_local.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/models/documentos_postulacion.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/services/documentos_postulacion_service.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../support/fake_adapter.dart';

Map<String, dynamic> archivoJson({String codigo = 'doc-1', String nombre = 'carnet.pdf'}) => {
      'codigo': codigo,
      'nombreArchivo': nombre,
      'mimeType': 'application/pdf',
      'tamanoBytes': 2048,
      'subidoEn': '2026-07-16T15:00:00Z',
    };

Map<String, dynamic> documentosJson({bool puedeEditar = true}) => {
      'puedeEditar': puedeEditar,
      'requeridos': [
        {
          'tipoDocumentoCodigo': 'tipo-carnet',
          'nombre': 'Carnet de identidad',
          'descripcion': 'Ambas caras',
          'obligatorio': true,
          'adjuntado': true,
          'archivos': [archivoJson()],
        },
        {
          'tipoDocumentoCodigo': 'tipo-notas',
          'nombre': 'Certificado de notas',
          'descripcion': null,
          'obligatorio': true,
          'adjuntado': false,
          'archivos': <dynamic>[],
        },
      ],
      'reglas': {
        'tamanoMaximoBytes': 5242880,
        'formatosPermitidos': ['application/pdf', 'image/jpeg', 'image/png'],
        'maximoArchivosPorTipo': 5,
      },
    };

DioException _apiError(String codigo, String mensaje) => DioException(
      requestOptions: RequestOptions(path: '/x'),
      error: ApiException(code: codigo, message: mensaje),
    );

void main() {
  group('modelos', () {
    test('DocumentosPostulacion.fromJson lee los requeridos, sus archivos y las reglas', () {
      final d = DocumentosPostulacion.fromJson(documentosJson());

      expect(d.puedeEditar, isTrue);
      expect(d.requeridos, hasLength(2));
      expect(d.requeridos[0].nombre, 'Carnet de identidad');
      expect(d.requeridos[0].archivos.single.tamanoBytes, 2048);
      expect(d.requeridos[1].descripcion, isNull);
      expect(d.requeridos[1].adjuntado, isFalse);
      expect(d.adjuntados, 1);
      expect(d.reglas.maximoArchivosPorTipo, 5);
      expect(d.reglas.formatosPermitidos, contains('image/jpeg'));
    });

    test('ArchivoLocal.mimePorNombre decide por la extensión, sin importar mayúsculas', () {
      expect(ArchivoLocal.mimePorNombre('Carnet.PDF'), 'application/pdf');
      expect(ArchivoLocal.mimePorNombre('foto.jpeg'), 'image/jpeg');
      expect(ArchivoLocal.mimePorNombre('foto.JPG'), 'image/jpeg');
      expect(ArchivoLocal.mimePorNombre('scan.png'), 'image/png');
      expect(ArchivoLocal.mimePorNombre('nota.txt'), 'application/octet-stream');
      expect(ArchivoLocal.mimePorNombre('sinextension'), 'application/octet-stream');
    });
  });

  group('formatos_documentos', () {
    test('formatearTamano muestra KB o MB', () {
      expect(formatearTamano(800 * 1024), '800 KB');
      expect(formatearTamano(5 * 1024 * 1024), '5 MB');
      expect(formatearTamano((1.2 * 1024 * 1024).round()), '1.2 MB');
      expect(formatearTamano(10), '1 KB');
    });

    test('nombresDeFormatos une con coma y «o»', () {
      expect(nombresDeFormatos(['application/pdf']), 'PDF');
      expect(nombresDeFormatos(['image/png', 'application/pdf']), 'PDF o PNG');
      expect(nombresDeFormatos(['image/png', 'image/jpeg', 'application/pdf']), 'PDF, JPG o PNG');
    });

    test('validarArchivoLocal acepta lo permitido y explica el rechazo', () {
      const formatos = ['application/pdf', 'image/jpeg'];
      String? validar(String mime, int tamano) =>
          validarArchivoLocal(mimeType: mime, tamanoBytes: tamano, maximoBytes: 1024, formatos: formatos);

      expect(validar('application/pdf', 100), isNull);
      expect(validar('application/pdf', 1024), isNull);
      expect(validar('application/pdf', 1025), contains('supera el tamaño máximo de 1 KB'));
      expect(validar('application/pdf', 0), 'El archivo está vacío');
      expect(validar('image/png', 100), 'Formato no permitido. Se aceptan: PDF o JPG');
    });
  });

  group('mapearErrorDocumentos', () {
    test('documento o postulación inexistente pide refrescar', () {
      expect(mapearErrorDocumentos(_apiError('ERR_DOC_04', 'x')).refrescar, isTrue);
      expect(mapearErrorDocumentos(_apiError('ERR_DOC_09', 'x')).refrescar, isTrue);
    });

    test('postulación enviada o convocatoria cerrada no es editable', () {
      expect(mapearErrorDocumentos(_apiError('ERR_DOC_06', 'x')).noEditable, isTrue);
      expect(mapearErrorDocumentos(_apiError('ERR_DOC_07', 'x')).noEditable, isTrue);
    });

    test('formato, tamaño y máximo traen el mensaje en español del backend', () {
      expect(mapearErrorDocumentos(_apiError('ERR_DOC_03', 'El formato no está permitido')).mensaje,
          'El formato no está permitido');
      expect(mapearErrorDocumentos(_apiError('ERR_DOC_08', 'Ya adjuntaste el máximo')).mensaje,
          'Ya adjuntaste el máximo');
      expect(mapearErrorDocumentos(_apiError('ERR_SYS_06', 'Supera el máximo')).mensaje, 'Supera el máximo');
    });

    test('sin conexión permite reintentar y avisa que los otros documentos siguen guardados', () {
      final e = mapearErrorDocumentos(DioException(requestOptions: RequestOptions(path: '/x')));

      expect(e.red, isTrue);
      expect(e.mensaje, contains('siguen guardados'));
    });

    test('un código desconocido da el mensaje genérico sin datos sensibles', () {
      expect(mapearErrorDocumentos(_apiError('ERR_RARO_01', 'detalle interno')).mensaje, isNot(contains('interno')));
    });
  });

  group('DocumentosPostulacionService', () {
    late Directory carpeta;
    late File archivo;

    setUp(() async {
      carpeta = await Directory.systemTemp.createTemp('docs_test');
      archivo = File('${carpeta.path}/carnet.pdf')..writeAsBytesSync([0x25, 0x50, 0x44, 0x46, 0x2d, 0x31]);
    });

    tearDown(() async {
      // En Windows el adaptador simulado deja abierto el archivo; la limpieza del temporal es de mejor esfuerzo.
      try {
        await carpeta.delete(recursive: true);
      } on FileSystemException {
        // el sistema operativo borra el temporal más tarde
      }
    });

    ArchivoLocal local() => ArchivoLocal(
          ruta: archivo.path,
          nombre: 'carnet.pdf',
          mimeType: 'application/pdf',
          tamanoBytes: archivo.lengthSync(),
        );

    Dio clienteCon(FakeAdapter adapter) {
      final dio = Dio(BaseOptions(baseUrl: 'http://api.test/api/v1'));
      dio.httpClientAdapter = adapter;
      return dio;
    }

    // El servicio recibe `data` ya desempaquetado por el interceptor del ApiClient; acá se simula con un adaptador que
    // devuelve el JSON directo.
    FakeAdapter responde(Object? cuerpo, {int status = 200}) =>
        FakeAdapter((_) => respuestaJson(status, cuerpo as Map<String, dynamic>));

    test('listar hace GET /postulaciones/{codigo}/documentos', () async {
      final adapter = responde(documentosJson());

      final d = await DocumentosPostulacionService(clienteCon(adapter)).listar('post-1');

      expect(adapter.peticiones.single.method, 'GET');
      expect(adapter.peticiones.single.path, '/postulaciones/post-1/documentos');
      expect(d.requeridos, hasLength(2));
    });

    test('adjuntar sube un multipart con tipoDocumento y el archivo', () async {
      final adapter = responde(archivoJson());

      final r = await DocumentosPostulacionService(clienteCon(adapter)).adjuntar('post-1', 'tipo-notas', local());

      final p = adapter.peticiones.single;
      expect(p.method, 'POST');
      expect(p.path, '/postulaciones/post-1/documentos');
      final cuerpo = p.data as FormData;
      expect(cuerpo.fields.single.key, 'tipoDocumento');
      expect(cuerpo.fields.single.value, 'tipo-notas');
      expect(cuerpo.files.single.key, 'archivo');
      expect(cuerpo.files.single.value.filename, 'carnet.pdf');
      expect(cuerpo.files.single.value.contentType.toString(), 'application/pdf');
      expect(r.codigo, 'doc-1');
    });

    test('reemplazar hace PUT con solo el archivo', () async {
      final adapter = responde(archivoJson());

      await DocumentosPostulacionService(clienteCon(adapter)).reemplazar('post-1', 'doc-1', local());

      final p = adapter.peticiones.single;
      expect(p.method, 'PUT');
      expect(p.path, '/postulaciones/post-1/documentos/doc-1');
      expect((p.data as FormData).fields, isEmpty);
    });

    test('eliminar hace DELETE sobre ese documento', () async {
      final adapter = responde(<String, dynamic>{});

      await DocumentosPostulacionService(clienteCon(adapter)).eliminar('post-1', 'doc-1');

      expect(adapter.peticiones.single.method, 'DELETE');
      expect(adapter.peticiones.single.path, '/postulaciones/post-1/documentos/doc-1');
    });
  });
}
