import 'package:comedor_uagrm_mobile/core/network/api_client.dart';
import 'package:comedor_uagrm_mobile/core/theme/app_theme.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/models/archivo_local.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/models/documentos_postulacion.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/models/revision_documento.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/providers/documentos_postulacion_provider.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/screens/documentos_postulacion_screen.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/screens/revisar_documento_screen.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/services/captura_documento_service.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/services/documentos_postulacion_service.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'documentos_logica_test.dart' show archivoJson, documentosJson;

/// Servicio simulado: guarda lo pedido y responde con lo que decida el test.
class ServicioDocumentosFalso implements DocumentosPostulacionService {
  DocumentosPostulacion documentos = DocumentosPostulacion.fromJson(documentosJson());
  Object? errorAlSubir;
  final List<({String tipo, String nombre})> adjuntados = [];
  final List<({String documento, String nombre})> reemplazados = [];
  final List<String> eliminados = [];

  @override
  Future<DocumentosPostulacion> listar(String postulacion) async => documentos;

  @override
  Future<ArchivoAdjunto> adjuntar(String postulacion, String tipoDocumento, ArchivoLocal archivo) async {
    if (errorAlSubir != null) throw errorAlSubir!;
    adjuntados.add((tipo: tipoDocumento, nombre: archivo.nombre));
    return ArchivoAdjunto.fromJson(archivoJson(codigo: 'doc-nuevo', nombre: archivo.nombre));
  }

  @override
  Future<ArchivoAdjunto> reemplazar(String postulacion, String documento, ArchivoLocal archivo) async {
    if (errorAlSubir != null) throw errorAlSubir!;
    reemplazados.add((documento: documento, nombre: archivo.nombre));
    return ArchivoAdjunto.fromJson(archivoJson(codigo: documento, nombre: archivo.nombre));
  }

  @override
  Future<void> eliminar(String postulacion, String documento) async => eliminados.add(documento);
}

/// Cámara, galería y archivos simulados: los plugins no corren en `flutter test`.
class CapturaFalsa implements CapturaDocumentoService {
  ArchivoLocal? pdf = const ArchivoLocal(
    ruta: '/tmp/notas.pdf',
    nombre: 'notas.pdf',
    mimeType: 'application/pdf',
    tamanoBytes: 2048,
  );
  bool camaraDenegada = false;
  int fotos = 0;

  @override
  Future<ArchivoLocal?> tomarFoto() async {
    fotos++;
    if (camaraDenegada) throw const PermisoCamaraDenegado();
    return null;
  }

  @override
  Future<ArchivoLocal?> elegirDeGaleria() async => null;

  @override
  Future<ArchivoLocal?> elegirPdf() async => pdf;

  @override
  Future<ArchivoLocal?> recortar(ArchivoLocal imagen) async => null;
}

DioException _apiError(String codigo, String mensaje) => DioException(
      requestOptions: RequestOptions(path: '/x'),
      error: ApiException(code: codigo, message: mensaje),
    );

Widget app(ServicioDocumentosFalso servicio, CapturaFalsa captura, {String ruta = '/documentos'}) {
  final rutas = GoRouter(
    initialLocation: ruta,
    routes: [
      GoRoute(path: '/', builder: (context, state) => const Scaffold(body: Text('inicio'))),
      GoRoute(
        path: '/documentos',
        builder: (context, state) => const DocumentosPostulacionScreen(codigo: 'post-1'),
      ),
      GoRoute(
        path: '/mis-postulaciones/:codigo/documentos/revisar',
        builder: (context, state) => RevisarDocumentoScreen(revision: state.extra! as RevisionDocumento),
      ),
    ],
  );
  return ProviderScope(
    overrides: [
      documentosPostulacionServiceProvider.overrideWithValue(servicio),
      capturaDocumentoServiceProvider.overrideWithValue(captura),
    ],
    child: MaterialApp.router(theme: AppTheme.light, routerConfig: rutas),
  );
}

/// Abre «Adjuntar» del documento que falta, elige «Elegir un archivo PDF» y deja la pantalla en «Revisar y adjuntar».
Future<void> irARevisar(WidgetTester tester) async {
  await tester.tap(find.text('Adjuntar'));
  await tester.pumpAndSettle();
  await tester.tap(find.text('Elegir un archivo PDF'));
  await tester.pumpAndSettle();
}

void main() {
  group('DocumentosPostulacionScreen', () {
    testWidgets('muestra los requeridos con su estado, el conteo y los archivos con su tamaño', (tester) async {
      await tester.pumpWidget(app(ServicioDocumentosFalso(), CapturaFalsa()));
      await tester.pumpAndSettle();

      expect(find.text('Adjuntos (1 de 2)'), findsOneWidget);
      expect(find.text('Carnet de identidad'), findsOneWidget);
      expect(find.text('Adjuntado'), findsOneWidget);
      expect(find.text('Falta'), findsOneWidget);
      expect(find.text('carnet.pdf'), findsOneWidget);
      expect(find.textContaining('2 KB'), findsOneWidget);
      expect(find.text('Agregar otro archivo'), findsOneWidget);
      expect(find.text('Adjuntar'), findsOneWidget);
    });

    testWidgets('con la postulación no editable solo se ve: sin adjuntar, reemplazar ni eliminar', (tester) async {
      final servicio = ServicioDocumentosFalso()
        ..documentos = DocumentosPostulacion.fromJson(documentosJson(puedeEditar: false));

      await tester.pumpWidget(app(servicio, CapturaFalsa()));
      await tester.pumpAndSettle();

      expect(find.textContaining('ya no se puede modificar'), findsOneWidget);
      expect(find.text('Adjuntar'), findsNothing);
      expect(find.byTooltip('Reemplazar carnet.pdf'), findsNothing);
      expect(find.byTooltip('Eliminar carnet.pdf'), findsNothing);
    });

    testWidgets('al llegar al máximo de archivos de un documento ya no ofrece agregar otro', (tester) async {
      final json = documentosJson();
      (json['reglas'] as Map<String, dynamic>)['maximoArchivosPorTipo'] = 1;
      final servicio = ServicioDocumentosFalso()..documentos = DocumentosPostulacion.fromJson(json);

      await tester.pumpWidget(app(servicio, CapturaFalsa()));
      await tester.pumpAndSettle();

      expect(find.text('Agregar otro archivo'), findsNothing);
      expect(find.text('Adjuntar'), findsOneWidget);
    });

    testWidgets('adjuntar un PDF: elige la fuente, lo revisa, lo sube y vuelve a la lista con aviso', (tester) async {
      final servicio = ServicioDocumentosFalso();
      await tester.pumpWidget(app(servicio, CapturaFalsa()));
      await tester.pumpAndSettle();

      await irARevisar(tester);

      expect(find.text('Revisar y adjuntar'), findsOneWidget);
      expect(find.text('Certificado de notas'), findsOneWidget);
      expect(find.textContaining('notas.pdf'), findsOneWidget);
      expect(find.text('Recortar o girar'), findsNothing); // un PDF no se recorta

      await tester.tap(find.text('Adjuntar documento'));
      await tester.pumpAndSettle();

      expect(servicio.adjuntados, [(tipo: 'tipo-notas', nombre: 'notas.pdf')]);
      expect(find.text('Documentos'), findsOneWidget);
      expect(find.text('Documento adjuntado'), findsOneWidget);
    });

    testWidgets('sin permiso de cámara avisa y vuelve a ofrecer la galería o los archivos (A1)', (tester) async {
      final captura = CapturaFalsa()..camaraDenegada = true;
      await tester.pumpWidget(app(ServicioDocumentosFalso(), captura));
      await tester.pumpAndSettle();

      await tester.tap(find.text('Adjuntar'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Tomar una foto'));
      await tester.pumpAndSettle();

      expect(captura.fotos, 1);
      expect(find.textContaining('Sin permiso de la cámara'), findsOneWidget);
      expect(find.text('Elegir de la galería'), findsOneWidget);
      expect(find.text('Elegir un archivo PDF'), findsOneWidget);
    });

    testWidgets('eliminar pide confirmación y borra ese archivo', (tester) async {
      final servicio = ServicioDocumentosFalso();
      await tester.pumpWidget(app(servicio, CapturaFalsa()));
      await tester.pumpAndSettle();

      await tester.tap(find.byTooltip('Eliminar carnet.pdf'));
      await tester.pumpAndSettle();
      expect(servicio.eliminados, isEmpty);
      await tester.tap(find.text('Eliminar'));
      await tester.pumpAndSettle();

      expect(servicio.eliminados, ['doc-1']);
    });

    testWidgets('cancelar la confirmación no elimina nada', (tester) async {
      final servicio = ServicioDocumentosFalso();
      await tester.pumpWidget(app(servicio, CapturaFalsa()));
      await tester.pumpAndSettle();

      await tester.tap(find.byTooltip('Eliminar carnet.pdf'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Cancelar'));
      await tester.pumpAndSettle();

      expect(servicio.eliminados, isEmpty);
    });

    testWidgets('reemplazar sube el archivo nuevo sobre ese documento', (tester) async {
      final servicio = ServicioDocumentosFalso();
      await tester.pumpWidget(app(servicio, CapturaFalsa()));
      await tester.pumpAndSettle();

      await tester.tap(find.byTooltip('Reemplazar carnet.pdf'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Elegir un archivo PDF'));
      await tester.pumpAndSettle();
      expect(find.text('Reemplazar documento'), findsOneWidget);
      await tester.tap(find.text('Reemplazar documento'));
      await tester.pumpAndSettle();

      expect(servicio.reemplazados, [(documento: 'doc-1', nombre: 'notas.pdf')]);
      expect(find.text('Documento reemplazado'), findsOneWidget);
    });
  });

  group('RevisarDocumentoScreen', () {
    testWidgets('un archivo más grande que el máximo muestra el motivo y no deja adjuntar', (tester) async {
      final captura = CapturaFalsa()
        ..pdf = const ArchivoLocal(
          ruta: '/tmp/grande.pdf',
          nombre: 'grande.pdf',
          mimeType: 'application/pdf',
          tamanoBytes: 6 * 1024 * 1024,
        );
      final servicio = ServicioDocumentosFalso();
      await tester.pumpWidget(app(servicio, captura));
      await tester.pumpAndSettle();

      await irARevisar(tester);

      expect(find.textContaining('supera el tamaño máximo de 5 MB'), findsOneWidget);
      await tester.tap(find.text('Adjuntar documento'));
      await tester.pumpAndSettle();
      expect(servicio.adjuntados, isEmpty);
      expect(find.text('Revisar y adjuntar'), findsOneWidget);
    });

    testWidgets('un formato no permitido muestra el motivo y no deja adjuntar', (tester) async {
      final captura = CapturaFalsa()
        ..pdf = const ArchivoLocal(
          ruta: '/tmp/nota.txt',
          nombre: 'nota.txt',
          mimeType: 'text/plain',
          tamanoBytes: 100,
        );
      await tester.pumpWidget(app(ServicioDocumentosFalso(), captura));
      await tester.pumpAndSettle();

      await irARevisar(tester);

      expect(find.textContaining('Formato no permitido. Se aceptan: PDF, JPG o PNG'), findsOneWidget);
    });

    testWidgets('si el servidor rechaza el archivo muestra el motivo y se queda para reintentar', (tester) async {
      final servicio = ServicioDocumentosFalso()
        ..errorAlSubir = _apiError('ERR_DOC_03', 'El contenido del archivo no corresponde a su formato');
      await tester.pumpWidget(app(servicio, CapturaFalsa()));
      await tester.pumpAndSettle();

      await irARevisar(tester);
      await tester.tap(find.text('Adjuntar documento'));
      await tester.pumpAndSettle();

      expect(find.text('El contenido del archivo no corresponde a su formato'), findsOneWidget);
      expect(find.text('Revisar y adjuntar'), findsOneWidget);
      expect(find.text('Adjuntar documento'), findsOneWidget);
    });

    testWidgets('sin conexión ofrece reintentar y al reintentar sube el mismo archivo (A4)', (tester) async {
      final servicio = ServicioDocumentosFalso()
        ..errorAlSubir = DioException(requestOptions: RequestOptions(path: '/x'));
      await tester.pumpWidget(app(servicio, CapturaFalsa()));
      await tester.pumpAndSettle();

      await irARevisar(tester);
      await tester.tap(find.text('Adjuntar documento'));
      await tester.pumpAndSettle();
      expect(find.textContaining('siguen guardados'), findsOneWidget);

      servicio.errorAlSubir = null;
      await tester.tap(find.text('Reintentar'));
      await tester.pumpAndSettle();

      expect(servicio.adjuntados, [(tipo: 'tipo-notas', nombre: 'notas.pdf')]);
      expect(find.text('Documentos'), findsOneWidget);
    });

    testWidgets('si la postulación ya se envió mientras tanto, avisa y vuelve a la lista', (tester) async {
      final servicio = ServicioDocumentosFalso()
        ..errorAlSubir = _apiError('ERR_DOC_06', 'ya enviada');
      await tester.pumpWidget(app(servicio, CapturaFalsa()));
      await tester.pumpAndSettle();

      await irARevisar(tester);
      await tester.tap(find.text('Adjuntar documento'));
      await tester.pumpAndSettle();

      expect(find.text('Documentos'), findsOneWidget);
      expect(find.textContaining('ya fue enviada'), findsOneWidget);
    });
  });
}
