import 'package:comedor_uagrm_mobile/core/network/api_client.dart';
import 'package:comedor_uagrm_mobile/core/storage/token_storage.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/models/estado_postulacion.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/models/mi_postulacion_detalle.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/models/mi_postulacion_resumen.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/models/paso_linea_tiempo.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/postulaciones_error_mapper.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/providers/mis_postulaciones_provider.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/providers/postulaciones_servicios_provider.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/services/postulaciones_service.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../support/fake_adapter.dart';

Map<String, dynamic> resumenJson(int n, {String estado = 'pendiente'}) => {
      'codigo': 'post-$n',
      'convocatoria': {'codigo': 'conv-$n', 'nombre': 'Beca $n'},
      'gestionAcademica': {'codigo': 'gestion-$n', 'nombre': '2026-$n'},
      'fechaEnvio': '2026-11-13T15:30:00Z',
      'estado': estado,
      'numeroEnvios': 1,
    };

Map<String, dynamic> detalleJson({String estado = 'pendiente', Map<String, dynamic> extra = const {}}) => {
      ...resumenJson(1, estado: estado),
      'lineaDeTiempo': [
        {'estado': 'pendiente', 'fecha': '2026-11-13T15:30:00Z', 'situacion': 'COMPLETADO'},
        {'estado': 'resultado', 'fecha': null, 'situacion': 'PENDIENTE'},
      ],
      'observaciones': null,
      'motivo': null,
      'finalista': null,
      'resultado': null,
      'puedeCorregir': false,
      ...extra,
    };

Map<String, dynamic> metaJson({int page = 1, int totalItems = 1, int totalPages = 1}) =>
    {'page': page, 'pageSize': 20, 'totalItems': totalItems, 'totalPages': totalPages};

PostulacionesService servicioCon(FakeAdapter adapter) {
  FlutterSecureStorage.setMockInitialValues({});
  final dio = ApiClient.construir('http://api.test/api/v1', TokenStorage())..httpClientAdapter = adapter;
  return PostulacionesService(dio);
}

void main() {
  group('modelos', () {
    test('MiPostulacionResumen se parsea con su convocatoria, gestión, fecha y estado visible', () {
      final r = MiPostulacionResumen.fromJson(resumenJson(2, estado: 'no_adjudicada'));

      expect(r.codigo, 'post-2');
      expect(r.convocatoria.nombre, 'Beca 2');
      expect(r.gestionAcademica.codigo, 'gestion-2');
      expect(r.fechaEnvio, DateTime.utc(2026, 11, 13, 15, 30));
      expect(r.estado, EstadoPostulacion.noAdjudicada);
      expect(r.numeroEnvios, 1);
    });

    test('un estado desconocido es un error de contrato; los seis visibles se reconocen', () {
      expect(() => EstadoPostulacion.deValor('borrador'), throwsFormatException);
      for (final estado in EstadoPostulacion.values) {
        expect(EstadoPostulacion.deValor(estado.valor), estado);
      }
    });

    test('MiPostulacionDetalle sin bloques opcionales los deja en null', () {
      final d = MiPostulacionDetalle.fromJson(detalleJson());

      expect(d.resumen.estado, EstadoPostulacion.pendiente);
      expect(d.observaciones, isNull);
      expect(d.motivo, isNull);
      expect(d.finalista, isNull);
      expect(d.resultado, isNull);
      expect(d.puedeCorregir, isFalse);
      expect(d.lineaDeTiempo, hasLength(2));
      expect(d.lineaDeTiempo[0].situacion, SituacionPaso.completado);
      expect(d.lineaDeTiempo[1].situacion, SituacionPaso.pendiente);
      expect(d.lineaDeTiempo[1].fecha, isNull);
    });

    test('MiPostulacionDetalle con todos los bloques los parsea', () {
      final d = MiPostulacionDetalle.fromJson(detalleJson(estado: 'devuelta', extra: {
        'observaciones': {'texto': 'Falta el CI', 'fecha': '2026-11-15T14:00:00Z'},
        'motivo': 'Motivo del archivo',
        'finalista': {'plazoEntrega': '2026-11-20', 'documentacionRecibida': true, 'documentosFisicos': 'CI y notas'},
        'resultado': 'ADJUDICADA',
        'puedeCorregir': true,
      }));

      expect(d.observaciones!.texto, 'Falta el CI');
      expect(d.observaciones!.fecha, DateTime.utc(2026, 11, 15, 14));
      expect(d.motivo, 'Motivo del archivo');
      expect(d.finalista!.plazoEntrega, DateTime(2026, 11, 20));
      expect(d.finalista!.documentacionRecibida, isTrue);
      expect(d.finalista!.documentosFisicos, 'CI y notas');
      expect(d.resultado, 'ADJUDICADA');
      expect(d.puedeCorregir, isTrue);
    });

    test('un finalista sin aviso trae plazo y documentos nulos', () {
      final d = MiPostulacionDetalle.fromJson(detalleJson(extra: {
        'finalista': {'plazoEntrega': null, 'documentacionRecibida': false, 'documentosFisicos': null},
      }));

      expect(d.finalista!.plazoEntrega, isNull);
      expect(d.finalista!.documentosFisicos, isNull);
    });

    test('las etiquetas de los pasos distinguen el reenvío pendiente del envío completado', () {
      PasoLineaTiempo paso(String estado, String situacion) =>
          PasoLineaTiempo.fromJson({'estado': estado, 'fecha': null, 'situacion': situacion});

      expect(paso('pendiente', 'COMPLETADO').etiqueta, 'Postulación enviada');
      expect(paso('pendiente', 'PENDIENTE').etiqueta, 'Reenvío de la postulación corregida');
      expect(paso('resultado', 'PENDIENTE').etiqueta, 'Resultado de la adjudicación');
      expect(paso('devuelta', 'COMPLETADO').etiqueta, 'Devuelta para corrección');
      expect(paso('raro', 'COMPLETADO').etiqueta, 'raro');
    });
  });

  group('PostulacionesService', () {
    test('listarMias hace GET /postulaciones/mias con la página, el filtro y el sort, y omite lo vacío', () async {
      final adapter = FakeAdapter((_) => respuestaJson(200, exito([resumenJson(1)], meta: metaJson())));
      final servicio = servicioCon(adapter);

      final pagina = await servicio.listarMias(gestionAcademica: 'g-1', page: 2, pageSize: 10, sort: '-fechaEnvio');
      await servicio.listarMias();

      expect(adapter.peticiones[0].method, 'GET');
      expect(adapter.peticiones[0].path, '/postulaciones/mias');
      expect(adapter.peticiones[0].queryParameters,
          {'page': 2, 'pageSize': 10, 'filter[gestionAcademica]': 'g-1', 'sort': '-fechaEnvio'});
      expect(adapter.peticiones[1].queryParameters, {'page': 1, 'pageSize': 20});
      expect(pagina.items.single.codigo, 'post-1');
      expect(pagina.meta.totalItems, 1);
    });

    test('obtener hace GET /postulaciones/{codigo}', () async {
      final adapter = FakeAdapter((_) => respuestaJson(200, exito(detalleJson())));

      final detalle = await servicioCon(adapter).obtener('post-1');

      expect(adapter.peticiones.single.path, '/postulaciones/post-1');
      expect(detalle.resumen.codigo, 'post-1');
    });

    test('un 404 con ERR_POS_01 sale como ApiException dentro de un DioException', () async {
      final adapter = FakeAdapter((_) => respuestaJson(404, fallo('ERR_POS_01')));

      await expectLater(
        servicioCon(adapter).obtener('ajena'),
        throwsA(isA<DioException>().having((e) => (e.error as ApiException).code, 'code', 'ERR_POS_01')),
      );
    });
  });

  group('mapearErrorPostulaciones', () {
    DioException apiError(String codigo, {int status = 400}) => DioException(
          requestOptions: RequestOptions(path: '/x'),
          response: Response(requestOptions: RequestOptions(path: '/x'), statusCode: status),
          error: ApiException(code: codigo, message: 'mensaje del backend'),
        );

    test('ERR_POS_01: la postulación no existe o no es tuya', () {
      final e = mapearErrorPostulaciones(apiError('ERR_POS_01', status: 404));

      expect(e.mensaje, 'La postulación no existe o no es tuya');
      expect(e.noEncontrada, isTrue);
    });

    test('una falla de conexión y un error del servidor ofrecen reintentar', () {
      final sinRespuesta = DioException(requestOptions: RequestOptions(path: '/x'));
      final servidor = mapearErrorPostulaciones(apiError('ERR_SYS_00', status: 500));

      expect(mapearErrorPostulaciones(sinRespuesta).red, isTrue);
      expect(mapearErrorPostulaciones(sinRespuesta).mensaje, contains('No se pudo conectar'));
      expect(servidor.red, isTrue);
      expect(servidor.mensaje, isNot(contains('mensaje del backend')));
    });

    test('ERR_AUTH_07 y lo desconocido tienen su texto', () {
      expect(mapearErrorPostulaciones(apiError('ERR_AUTH_07', status: 403)).mensaje,
          'No tienes permiso para esta acción.');
      expect(mapearErrorPostulaciones(Exception('raro')).mensaje, 'Ocurrió un error inesperado. Inténtalo de nuevo.');
    });
  });

  group('misPostulacionesListProvider', () {
    late FakeAdapter adapter;

    ProviderContainer contenedor(ResponseBody Function(RequestOptions) responder) {
      adapter = FakeAdapter(responder);
      final c = ProviderContainer(overrides: [
        postulacionesServiceProvider.overrideWithValue(servicioCon(adapter)),
      ]);
      addTearDown(c.dispose);
      return c;
    }

    /// Responde la página pedida: dos páginas de dos elementos cada una (4 en total).
    ResponseBody dosPaginas(RequestOptions o) {
      final page = o.queryParameters['page'] as int;
      final base = page == 1 ? 1 : 3;
      return respuestaJson(200,
          exito([resumenJson(base), resumenJson(base + 1)], meta: metaJson(page: page, totalItems: 4, totalPages: 2)));
    }

    test('carga la primera página y acumula las siguientes sin perder lo cargado', () async {
      final c = contenedor(dosPaginas);
      c.listen(misPostulacionesListProvider, (_, _) {});

      var lista = await c.read(misPostulacionesListProvider.future);
      expect(lista.items.map((p) => p.codigo), ['post-1', 'post-2']);
      expect(lista.hayMas, isTrue);

      await c.read(misPostulacionesListProvider.notifier).cargarSiguiente();
      lista = c.read(misPostulacionesListProvider).value!;

      expect(lista.items.map((p) => p.codigo), ['post-1', 'post-2', 'post-3', 'post-4']);
      expect(lista.hayMas, isFalse);
      expect(lista.cargandoMas, isFalse);
      expect(adapter.peticiones.map((p) => p.queryParameters['page']), [1, 2]);
    });

    test('al llegar a la última página no pide más', () async {
      final c = contenedor(dosPaginas);
      c.listen(misPostulacionesListProvider, (_, _) {});
      await c.read(misPostulacionesListProvider.future);
      await c.read(misPostulacionesListProvider.notifier).cargarSiguiente();

      await c.read(misPostulacionesListProvider.notifier).cargarSiguiente();

      expect(adapter.peticiones, hasLength(2));
    });

    test('al cambiar el filtro se descarta lo acumulado y se pide la página 1 con el filtro', () async {
      final c = contenedor(dosPaginas);
      c.listen(misPostulacionesListProvider, (_, _) {});
      await c.read(misPostulacionesListProvider.future);
      await c.read(misPostulacionesListProvider.notifier).cargarSiguiente();

      c.read(misPostulacionesUiProvider.notifier).elegirGestion('gestion-2');
      final lista = await c.read(misPostulacionesListProvider.future);

      expect(adapter.peticiones.last.queryParameters,
          {'page': 1, 'pageSize': 20, 'filter[gestionAcademica]': 'gestion-2'});
      expect(lista.items.map((p) => p.codigo), ['post-1', 'post-2']);
      expect(lista.meta.page, 1);
    });

    test('un fallo al pedir la siguiente página deja lo cargado y marca el error', () async {
      var llamadas = 0;
      final c = contenedor((o) {
        llamadas++;
        return llamadas == 1 ? dosPaginas(o) : respuestaJson(500, fallo('ERR_SYS_00'));
      });
      c.listen(misPostulacionesListProvider, (_, _) {});
      await c.read(misPostulacionesListProvider.future);

      await c.read(misPostulacionesListProvider.notifier).cargarSiguiente();
      final lista = c.read(misPostulacionesListProvider).value!;

      expect(lista.items, hasLength(2));
      expect(lista.errorSiguiente, isTrue);
      expect(lista.cargandoMas, isFalse);
    });

    test('un error al pedir la primera página queda como error del provider', () async {
      final c = contenedor((_) => respuestaJson(500, fallo('ERR_SYS_00')));
      c.listen(misPostulacionesListProvider, (_, _) {});

      await expectLater(c.read(misPostulacionesListProvider.future), throwsA(isA<DioException>()));
    });

    test('una lista vacía no tiene más páginas', () async {
      final c = contenedor((_) => respuestaJson(200, exito([], meta: metaJson(totalItems: 0, totalPages: 0))));
      c.listen(misPostulacionesListProvider, (_, _) {});

      final lista = await c.read(misPostulacionesListProvider.future);

      expect(lista.items, isEmpty);
      expect(lista.hayMas, isFalse);
    });
  });

  group('postulacionDetailProvider', () {
    test('pide el detalle del código de la familia', () async {
      final adapter = FakeAdapter((_) => respuestaJson(200, exito(detalleJson())));
      final c = ProviderContainer(overrides: [
        postulacionesServiceProvider.overrideWithValue(servicioCon(adapter)),
      ]);
      addTearDown(c.dispose);
      c.listen(postulacionDetailProvider('post-1'), (_, _) {});

      final detalle = await c.read(postulacionDetailProvider('post-1').future);

      expect(detalle.resumen.codigo, 'post-1');
      expect(adapter.peticiones.single.path, '/postulaciones/post-1');
    });
  });
}
