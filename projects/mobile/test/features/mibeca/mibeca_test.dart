import 'package:comedor_uagrm_mobile/core/network/api_client.dart';
import 'package:comedor_uagrm_mobile/core/theme/app_theme.dart';
import 'package:comedor_uagrm_mobile/features/mibeca/formatos_mibeca.dart';
import 'package:comedor_uagrm_mobile/features/mibeca/mibeca_error_mapper.dart';
import 'package:comedor_uagrm_mobile/features/mibeca/models/mi_beca.dart';
import 'package:comedor_uagrm_mobile/features/mibeca/models/pagina_asistencias.dart';
import 'package:comedor_uagrm_mobile/features/mibeca/providers/mibeca_provider.dart';
import 'package:comedor_uagrm_mobile/features/mibeca/providers/mibeca_servicios_provider.dart';
import 'package:comedor_uagrm_mobile/features/mibeca/services/mibeca_service.dart';
import 'package:comedor_uagrm_mobile/features/mibeca/widgets/mi_beca_vista.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

Map<String, dynamic> _becaJson({String estado = 'VIGENTE', String? motivo, int total = 13}) => {
      'codigo': 'beca-1',
      'estado': estado,
      'convocatoria': 'Convocatoria 2026-II',
      'gestion': '2026-II',
      'fechaAdjudicacion': '2026-08-28T14:00:00Z',
      'fechaInicio': '2026-09-01',
      'fechaFin': null,
      'motivo': motivo,
      'totalAsistencias': total,
    };

Map<String, dynamic> _asistenciaJson(int dia) => {
      'fecha': '2026-09-${dia.toString().padLeft(2, '0')}',
      'horaRegistro': '2026-09-${dia.toString().padLeft(2, '0')}T16:30:00Z',
    };

PaginaAsistencias _pagina(List<int> dias, {int page = 1, int totalPages = 1}) => PaginaAsistencias.fromJson({
      'data': [for (final d in dias) _asistenciaJson(d)],
      'meta': {'page': page, 'pageSize': 20, 'totalItems': dias.length, 'totalPages': totalPages},
    });

DioException _apiError(String code, [int status = 404]) => DioException(
      requestOptions: RequestOptions(path: '/x'),
      error: ApiException(code: code, message: 'mensaje'),
      response: Response(requestOptions: RequestOptions(path: '/x'), statusCode: status),
    );

/// Servicio simulado: guarda lo pedido y responde con lo que decida cada test.
class ServicioFalso implements MiBecaService {
  final List<({String? desde, String? hasta, int page})> llamadas = [];
  Future<MiBeca> Function() beca = () async => MiBeca.fromJson(_becaJson());
  Future<PaginaAsistencias> Function(int page) pagina = (_) async => _pagina([18, 17]);

  @override
  Future<MiBeca> obtener() => beca();

  @override
  Future<PaginaAsistencias> listarAsistencias({String? desde, String? hasta, int page = 1, int pageSize = 20}) {
    llamadas.add((desde: desde, hasta: hasta, page: page));
    return pagina(page);
  }
}

Widget _app(ServicioFalso servicio, {VoidCallback? onVerPostulaciones}) => ProviderScope(
      overrides: [miBecaServiceProvider.overrideWithValue(servicio)],
      child: MaterialApp(
        theme: AppTheme.light,
        home: Scaffold(body: MiBecaVista(onVerPostulaciones: onVerPostulaciones)),
      ),
    );

void main() {
  group('modelos', () {
    test('MiBeca.fromJson lee el estado, la gestión, el motivo y el total', () {
      final beca = MiBeca.fromJson(_becaJson(estado: 'BAJA', motivo: 'Abandonó', total: 5));

      expect(beca.estado, 'BAJA');
      expect(beca.vigente, isFalse);
      expect(beca.gestion, '2026-II');
      expect(beca.motivo, 'Abandonó');
      expect(beca.totalAsistencias, 5);
      expect(beca.fechaAdjudicacion, DateTime.parse('2026-08-28T14:00:00Z'));
    });

    test('una beca de reemplazo no tiene fecha de adjudicación', () {
      final beca = MiBeca.fromJson({..._becaJson(), 'fechaAdjudicacion': null});

      expect(beca.fechaAdjudicacion, isNull);
      expect(beca.vigente, isTrue);
    });

    test('PaginaAsistencias.fromJson lee los elementos y el meta', () {
      final pagina = _pagina([18, 17], page: 2, totalPages: 3);

      expect(pagina.items, hasLength(2));
      expect(pagina.items.first.fecha, DateTime.parse('2026-09-18'));
      expect(pagina.meta.page, 2);
      expect(pagina.meta.totalPages, 3);
    });
  });

  group('formatos', () {
    test('el día de calendario no cambia por zona horaria', () {
      expect(formatearDiaBeca(DateTime.parse('2026-09-18')), '18/09/2026');
      expect(diaParaApi(DateTime(2026, 9, 5)), '2026-09-05');
    });

    test('la hora y la fecha de un instante se muestran en La Paz (UTC-4)', () {
      expect(formatearHoraBeca(DateTime.parse('2026-09-18T16:30:00Z')), '12:30');
      // 02:00 UTC del 29/08 son las 22:00 del 28/08 en La Paz.
      expect(formatearFechaInstanteBeca(DateTime.parse('2026-08-29T02:00:00Z')), '28/08/2026');
    });

    test('etiquetaEstadoBeca traduce los tres estados', () {
      expect(etiquetaEstadoBeca('VIGENTE'), 'Vigente');
      expect(etiquetaEstadoBeca('SUSPENDIDA'), 'Suspendida');
      expect(etiquetaEstadoBeca('BAJA'), 'Dada de baja');
    });
  });

  group('mapearErrorMiBeca', () {
    test('ERR_MBE_01 es «sin beca»', () {
      final error = mapearErrorMiBeca(_apiError('ERR_MBE_01'));

      expect(error.sinBeca, isTrue);
      expect(error.red, isFalse);
    });

    test('sin respuesta o un 500 se pueden reintentar', () {
      expect(mapearErrorMiBeca(DioException(requestOptions: RequestOptions(path: '/x'))).red, isTrue);
      expect(mapearErrorMiBeca(_apiError('ERR_SYS_00', 500)).red, isTrue);
    });

    test('un error desconocido da el mensaje genérico', () {
      final error = mapearErrorMiBeca(_apiError('ERR_RARO'));

      expect(error.sinBeca, isFalse);
      expect(error.mensaje, contains('inesperado'));
    });
  });

  group('providers', () {
    ProviderContainer contenedor(ServicioFalso servicio) {
      final c = ProviderContainer(overrides: [miBecaServiceProvider.overrideWithValue(servicio)]);
      addTearDown(c.dispose);
      return c;
    }

    test('el historial pide la página 1 sin rango', () async {
      final servicio = ServicioFalso();
      final c = contenedor(servicio);
      c.listen(miBecaAsistenciasProvider, (_, _) {});

      final lista = await c.read(miBecaAsistenciasProvider.future);

      expect(lista.items, hasLength(2));
      expect(servicio.llamadas.single, (desde: null, hasta: null, page: 1));
    });

    test('elegir un rango recarga desde la página 1 con filter[desde] y filter[hasta]', () async {
      final servicio = ServicioFalso();
      final c = contenedor(servicio);
      c.listen(miBecaAsistenciasProvider, (_, _) {});
      await c.read(miBecaAsistenciasProvider.future);

      c.read(miBecaRangoUiProvider.notifier).elegir(DateTimeRange(start: DateTime(2026, 9, 2), end: DateTime(2026, 9, 30)));
      await c.read(miBecaAsistenciasProvider.future);

      expect(servicio.llamadas.last, (desde: '2026-09-02', hasta: '2026-09-30', page: 1));
    });

    test('cargarSiguiente suma la página 2; sin más páginas no pide nada', () async {
      final servicio = ServicioFalso()..pagina = (page) async => _pagina(page == 1 ? [18] : [17], page: page, totalPages: 2);
      final c = contenedor(servicio);
      c.listen(miBecaAsistenciasProvider, (_, _) {});
      await c.read(miBecaAsistenciasProvider.future);

      await c.read(miBecaAsistenciasProvider.notifier).cargarSiguiente();
      await c.read(miBecaAsistenciasProvider.notifier).cargarSiguiente();

      expect(c.read(miBecaAsistenciasProvider).value!.items, hasLength(2));
      expect(servicio.llamadas.map((l) => l.page), [1, 2]);
    });

    test('si falla la página siguiente conserva lo cargado y marca el error', () async {
      final servicio = ServicioFalso()
        ..pagina = (page) async {
          if (page == 2) throw _apiError('ERR_SYS_00', 500);
          return _pagina([18], totalPages: 2);
        };
      final c = contenedor(servicio);
      c.listen(miBecaAsistenciasProvider, (_, _) {});
      await c.read(miBecaAsistenciasProvider.future);

      await c.read(miBecaAsistenciasProvider.notifier).cargarSiguiente();

      final lista = c.read(miBecaAsistenciasProvider).value!;
      expect(lista.items, hasLength(1));
      expect(lista.errorSiguiente, isTrue);
      expect(lista.cargandoMas, isFalse);
    });
  });

  group('MiBecaVista', () {
    testWidgets('muestra el estado, la convocatoria, el total y las asistencias con su hora', (tester) async {
      await tester.pumpWidget(_app(ServicioFalso()));
      await tester.pumpAndSettle();

      expect(find.text('Vigente'), findsOneWidget);
      expect(find.text('Convocatoria 2026-II · Gestión 2026-II'), findsOneWidget);
      expect(find.text('13'), findsOneWidget);
      expect(find.text('Adjudicada el 28/08/2026'), findsOneWidget);
      expect(find.text('18/09/2026'), findsOneWidget);
      expect(find.text('17/09/2026'), findsOneWidget);
      // 16:30 UTC son las 12:30 en La Paz.
      expect(find.text('12:30'), findsNWidgets(2));
      expect(find.textContaining('Motivo:'), findsNothing);
    });

    testWidgets('con la beca dada de baja muestra el motivo y el historial sigue disponible', (tester) async {
      final servicio = ServicioFalso()..beca = () async => MiBeca.fromJson(_becaJson(estado: 'BAJA', motivo: 'Abandonó'));
      await tester.pumpWidget(_app(servicio));
      await tester.pumpAndSettle();

      expect(find.text('Dada de baja'), findsOneWidget);
      expect(find.text('Motivo: Abandonó'), findsOneWidget);
      expect(find.text('18/09/2026'), findsOneWidget);
    });

    testWidgets('sin asistencias muestra un mensaje informativo', (tester) async {
      final servicio = ServicioFalso()..pagina = (_) async => _pagina([], totalPages: 0);
      await tester.pumpWidget(_app(servicio));
      await tester.pumpAndSettle();

      expect(find.text('Todavía no hay asistencias registradas'), findsOneWidget);
    });

    testWidgets('con un rango sin resultados lo explica y «Quitar filtro» vuelve a todo', (tester) async {
      final servicio = ServicioFalso();
      await tester.pumpWidget(_app(servicio));
      await tester.pumpAndSettle();
      final contexto = tester.element(find.byType(MiBecaVista));
      ProviderScope.containerOf(contexto)
          .read(miBecaRangoUiProvider.notifier)
          .elegir(DateTimeRange(start: DateTime(2027, 1, 1), end: DateTime(2027, 1, 31)));
      servicio.pagina = (_) async => _pagina([], totalPages: 0);
      await tester.pumpAndSettle();

      expect(find.text('No hay asistencias en ese rango de fechas'), findsOneWidget);
      expect(servicio.llamadas.last.desde, '2027-01-01');

      servicio.pagina = (_) async => _pagina([18]);
      await tester.tap(find.text('Quitar filtro'));
      await tester.pumpAndSettle();

      expect(servicio.llamadas.last.desde, isNull);
      expect(find.text('18/09/2026'), findsOneWidget);
    });

    testWidgets('sin beca avisa y lleva a «Mis postulaciones»', (tester) async {
      var fue = false;
      final servicio = ServicioFalso()..beca = () async => throw _apiError('ERR_MBE_01');
      await tester.pumpWidget(_app(servicio, onVerPostulaciones: () => fue = true));
      await tester.pumpAndSettle();

      expect(find.text('No cuentas con una beca'), findsOneWidget);
      expect(find.text('Historial de asistencia'), findsNothing);
      await tester.tap(find.text('Ir a Mis postulaciones'));
      expect(fue, isTrue);
    });

    testWidgets('si falla la conexión muestra el error y «Reintentar» vuelve a pedir la beca', (tester) async {
      var intentos = 0;
      final servicio = ServicioFalso()
        ..beca = () async {
          intentos++;
          if (intentos == 1) throw DioException(requestOptions: RequestOptions(path: '/mi-beca'));
          return MiBeca.fromJson(_becaJson());
        };
      await tester.pumpWidget(_app(servicio));
      await tester.pumpAndSettle();

      expect(find.textContaining('No se pudo conectar'), findsOneWidget);
      await tester.tap(find.text('Reintentar'));
      await tester.pumpAndSettle();

      expect(find.text('Vigente'), findsOneWidget);
    });

    testWidgets('es de solo lectura: no ofrece acciones de edición ni de registro', (tester) async {
      await tester.pumpWidget(_app(ServicioFalso()));
      await tester.pumpAndSettle();

      for (final texto in ['Editar', 'Eliminar', 'Registrar asistencia']) {
        expect(find.text(texto), findsNothing);
      }
    });
  });
}
