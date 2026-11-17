import 'package:comedor_uagrm_mobile/core/network/api_client.dart';
import 'package:comedor_uagrm_mobile/core/theme/app_theme.dart';
import 'package:comedor_uagrm_mobile/features/asistencia/asistencia_error_mapper.dart';
import 'package:comedor_uagrm_mobile/features/asistencia/formatos_asistencia.dart';
import 'package:comedor_uagrm_mobile/features/asistencia/models/asistencia_registrada.dart';
import 'package:comedor_uagrm_mobile/features/asistencia/models/becado_asistencia.dart';
import 'package:comedor_uagrm_mobile/features/asistencia/providers/asistencia_provider.dart';
import 'package:comedor_uagrm_mobile/features/asistencia/providers/asistencia_servicios_provider.dart';
import 'package:comedor_uagrm_mobile/features/asistencia/screens/registrar_asistencia_screen.dart';
import 'package:comedor_uagrm_mobile/features/asistencia/services/asistencia_service.dart';
import 'package:comedor_uagrm_mobile/features/auth/models/usuario.dart';
import 'package:comedor_uagrm_mobile/features/auth/providers/auth_sesion_provider.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../support/fake_adapter.dart';
import '../../widget_test.dart' show FakeAuthSesion;

const _becadoJson = {
  'becaCodigo': 'beca-1',
  'nombres': 'Ana',
  'apellidos': 'Rojas Vaca',
  'ci': '9812241',
  'carrera': 'Ing. Sistemas',
  'convocatoria': 'Beca 2026-II',
  'estadoBeca': 'VIGENTE',
  'fecha': '2026-11-16',
  'horaRegistro': null,
};

BecadoAsistencia _becado({String estado = 'VIGENTE', String? hora}) =>
    BecadoAsistencia.fromJson({..._becadoJson, 'estadoBeca': estado, 'horaRegistro': hora});

DioException _apiError(String code, [String message = 'mensaje']) => DioException(
      requestOptions: RequestOptions(path: '/x'),
      error: ApiException(code: code, message: message),
      response: Response(requestOptions: RequestOptions(path: '/x'), statusCode: 409),
    );

/// Servicio simulado: guarda lo pedido y devuelve (o lanza) lo que se le diga.
class FakeAsistenciaService extends AsistenciaService {
  final List<String> busquedas = [];
  final List<String> registros = [];
  Object? errorAlBuscar;
  Object? errorAlRegistrar;
  BecadoAsistencia becado = _becado();
  AsistenciaRegistrada resultado = AsistenciaRegistrada(
    codigo: 'a1',
    becaCodigo: 'beca-1',
    horaRegistro: DateTime.parse('2026-11-16T18:35:00Z'),
    yaRegistrada: false,
  );

  FakeAsistenciaService() : super(Dio());

  @override
  Future<BecadoAsistencia> buscarBecado(String ci) async {
    busquedas.add(ci);
    if (errorAlBuscar != null) throw errorAlBuscar!;
    return becado;
  }

  @override
  Future<AsistenciaRegistrada> registrar(String becaCodigo) async {
    registros.add(becaCodigo);
    if (errorAlRegistrar != null) throw errorAlRegistrar!;
    return resultado;
  }
}

ProviderContainer _contenedor(FakeAsistenciaService servicio) {
  final c = ProviderContainer(overrides: [asistenciaServiceProvider.overrideWithValue(servicio)]);
  addTearDown(c.dispose);
  return c;
}

Widget _app(FakeAsistenciaService servicio) {
  return ProviderScope(
    overrides: [
      asistenciaServiceProvider.overrideWithValue(servicio),
      authSesionProvider.overrideWith(
        () => FakeAuthSesion(Usuario.fromJson({
          'codigo': 'u9',
          'nombres': 'Pedro',
          'apellidos': 'Comedor',
          'correo': 'pedro@uagrm.edu.bo',
          'rol': 'PERSONAL_COMEDOR',
        })),
      ),
    ],
    child: MaterialApp(theme: AppTheme.light, home: const RegistrarAsistenciaScreen()),
  );
}

void main() {
  group('modelos', () {
    test('BecadoAsistencia: vigente, nombre completo y hora opcional', () {
      final sin = _becado();
      final con = _becado(hora: '2026-11-16T18:35:00Z');

      expect(sin.vigente, isTrue);
      expect(sin.nombreCompleto, 'Ana Rojas Vaca');
      expect(sin.yaRegistroHoy, isFalse);
      expect(con.yaRegistroHoy, isTrue);
      expect(_becado(estado: 'BAJA').vigente, isFalse);
      expect(sin.conHora(DateTime.utc(2026, 11, 16, 18)).yaRegistroHoy, isTrue);
    });

    test('AsistenciaRegistrada.fromJson', () {
      final r = AsistenciaRegistrada.fromJson({
        'codigo': 'a1',
        'becaCodigo': 'b1',
        'fecha': '2026-11-16',
        'horaRegistro': '2026-11-16T18:35:00Z',
        'yaRegistrada': true,
      });

      expect(r.yaRegistrada, isTrue);
      expect(r.horaRegistro.toUtc(), DateTime.utc(2026, 11, 16, 18, 35));
    });
  });

  group('formatos (hora de La Paz)', () {
    test('la hora y la fecha se muestran en UTC-4', () {
      final i = DateTime.utc(2026, 11, 17, 2, 30);

      expect(formatearHoraAsistencia(i), '22:30');
      expect(formatearFechaAsistencia(i), '16/11/2026');
    });
  });

  group('AsistenciaService', () {
    test('buscarBecado manda el CI en el cuerpo de un POST, no en la URL', () async {
      final adaptador = FakeAdapter((_) => respuestaJson(200, {..._becadoJson}));
      final dio = Dio()..httpClientAdapter = adaptador;

      final becado = await AsistenciaService(dio).buscarBecado('9812241');

      expect(becado.nombres, 'Ana');
      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'POST');
      expect(peticion.path, '/asistencias/buscar-becado');
      expect(peticion.uri.toString(), isNot(contains('9812241')));
      expect(peticion.data, {'ci': '9812241'});
    });

    test('registrar manda el código de la beca', () async {
      final adaptador = FakeAdapter((_) => respuestaJson(201, {
            'codigo': 'a1',
            'becaCodigo': 'beca-1',
            'fecha': '2026-11-16',
            'horaRegistro': '2026-11-16T18:35:00Z',
            'yaRegistrada': false,
          }));
      final dio = Dio()..httpClientAdapter = adaptador;

      final r = await AsistenciaService(dio).registrar('beca-1');

      expect(r.yaRegistrada, isFalse);
      expect(adaptador.peticiones.single.path, '/asistencias');
      expect(adaptador.peticiones.single.data, {'becaCodigo': 'beca-1'});
    });
  });

  group('mapearErrorAsistencia', () {
    test('traduce los códigos de negocio', () {
      expect(mapearErrorAsistencia(_apiError('ERR_ASI_01')).mensaje, contains('no figura en el padrón'));
      expect(mapearErrorAsistencia(_apiError('ERR_ASI_02')).mensaje, contains('no está vigente'));
      expect(mapearErrorAsistencia(_apiError('ERR_ASI_03')).mensaje, contains('ya no existe'));
      expect(mapearErrorAsistencia(_apiError('ERR_AUTH_07')).mensaje, contains('permiso'));
      expect(mapearErrorAsistencia(_apiError('ERR_SYS_01', 'El CI es obligatorio')).mensaje, 'El CI es obligatorio');
      expect(mapearErrorAsistencia(_apiError('ERR_ASI_01')).red, isFalse);
    });

    test('sin respuesta o con error del servidor se puede reintentar', () {
      final sinRed = DioException(requestOptions: RequestOptions(path: '/x'));
      final servidor = DioException(
        requestOptions: RequestOptions(path: '/x'),
        response: Response(requestOptions: RequestOptions(path: '/x'), statusCode: 503),
      );

      expect(mapearErrorAsistencia(sinRed).red, isTrue);
      expect(mapearErrorAsistencia(servidor).red, isTrue);
      expect(mapearErrorAsistencia(StateError('x')).red, isFalse);
    });
  });

  group('AsistenciaNotifier', () {
    test('buscar y registrar: queda con la hora y «recién registrada»', () async {
      final servicio = FakeAsistenciaService();
      final c = _contenedor(servicio);
      c.listen(asistenciaProvider, (_, _) {});

      await c.read(asistenciaProvider.notifier).buscar(' 9812241 ');
      expect(servicio.busquedas, ['9812241']);
      expect(c.read(asistenciaProvider).becado!.nombreCompleto, 'Ana Rojas Vaca');

      await c.read(asistenciaProvider.notifier).registrar();

      final estado = c.read(asistenciaProvider);
      expect(servicio.registros, ['beca-1']);
      expect(estado.becado!.yaRegistroHoy, isTrue);
      expect(estado.recienRegistrada, isTrue);
      expect(estado.error, isNull);
    });

    test('si ya estaba registrada (A3) no hay error y no es «recién registrada»', () async {
      final servicio = FakeAsistenciaService()
        ..resultado = AsistenciaRegistrada(
          codigo: 'a1',
          becaCodigo: 'beca-1',
          horaRegistro: DateTime.parse('2026-11-16T16:00:00Z'),
          yaRegistrada: true,
        );
      final c = _contenedor(servicio);
      c.listen(asistenciaProvider, (_, _) {});
      await c.read(asistenciaProvider.notifier).buscar('9812241');

      await c.read(asistenciaProvider.notifier).registrar();

      final estado = c.read(asistenciaProvider);
      expect(estado.error, isNull);
      expect(estado.recienRegistrada, isFalse);
      expect(estado.becado!.horaRegistro!.toUtc(), DateTime.utc(2026, 11, 16, 16));
    });

    test('con una beca no vigente o ya registrada hoy, registrar no llama a la API', () async {
      final servicio = FakeAsistenciaService()..becado = _becado(estado: 'SUSPENDIDA');
      final c = _contenedor(servicio);
      c.listen(asistenciaProvider, (_, _) {});
      await c.read(asistenciaProvider.notifier).buscar('9812241');
      await c.read(asistenciaProvider.notifier).registrar();

      servicio.becado = _becado(hora: '2026-11-16T16:00:00Z');
      await c.read(asistenciaProvider.notifier).buscar('9812241');
      await c.read(asistenciaProvider.notifier).registrar();

      expect(servicio.registros, isEmpty);
    });

    test('un CI que no figura deja el error y ningún becado (A1)', () async {
      final servicio = FakeAsistenciaService()..errorAlBuscar = _apiError('ERR_ASI_01');
      final c = _contenedor(servicio);
      c.listen(asistenciaProvider, (_, _) {});

      await c.read(asistenciaProvider.notifier).buscar('0');

      final estado = c.read(asistenciaProvider);
      expect(estado.becado, isNull);
      expect(estado.error!.mensaje, contains('no figura en el padrón'));
    });

    test('un error al registrar conserva al becado para reintentar (A5)', () async {
      final servicio = FakeAsistenciaService()..errorAlRegistrar = DioException(requestOptions: RequestOptions(path: '/x'));
      final c = _contenedor(servicio);
      c.listen(asistenciaProvider, (_, _) {});
      await c.read(asistenciaProvider.notifier).buscar('9812241');

      await c.read(asistenciaProvider.notifier).registrar();

      final estado = c.read(asistenciaProvider);
      expect(estado.becado, isNotNull);
      expect(estado.becado!.yaRegistroHoy, isFalse);
      expect(estado.error!.red, isTrue);
    });

    test('siguiente deja la pantalla vacía', () async {
      final servicio = FakeAsistenciaService();
      final c = _contenedor(servicio);
      c.listen(asistenciaProvider, (_, _) {});
      await c.read(asistenciaProvider.notifier).buscar('9812241');

      c.read(asistenciaProvider.notifier).siguiente();

      expect(c.read(asistenciaProvider).becado, isNull);
    });
  });

  group('RegistrarAsistenciaScreen', () {
    testWidgets('sin CI no busca y pide el CI', (tester) async {
      final servicio = FakeAsistenciaService();
      await tester.pumpWidget(_app(servicio));

      await tester.tap(find.text('Buscar'));
      await tester.pump();

      expect(find.text('El CI es obligatorio'), findsOneWidget);
      expect(servicio.busquedas, isEmpty);
    });

    testWidgets('busca, muestra al becado, registra y confirma con la hora', (tester) async {
      final servicio = FakeAsistenciaService();
      await tester.pumpWidget(_app(servicio));

      await tester.enterText(find.byType(TextFormField), '9812241');
      await tester.tap(find.text('Buscar'));
      await tester.pumpAndSettle();

      expect(find.text('Ana Rojas Vaca'), findsOneWidget);
      expect(find.text('Beca vigente'), findsOneWidget);
      expect(find.text('Registrar asistencia'), findsWidgets);

      await tester.tap(find.widgetWithText(FilledButton, 'Registrar asistencia'));
      await tester.pumpAndSettle();

      expect(servicio.registros, ['beca-1']);
      expect(find.textContaining('Asistencia registrada a las 14:35'), findsOneWidget);
      expect(find.text('Siguiente becado'), findsOneWidget);

      await tester.tap(find.text('Siguiente becado'));
      await tester.pumpAndSettle();
      expect(find.text('Ana Rojas Vaca'), findsNothing);
    });

    testWidgets('una beca suspendida muestra el motivo y no ofrece registrar', (tester) async {
      final servicio = FakeAsistenciaService()..becado = _becado(estado: 'SUSPENDIDA');
      await tester.pumpWidget(_app(servicio));

      await tester.enterText(find.byType(TextFormField), '9812241');
      await tester.tap(find.text('Buscar'));
      await tester.pumpAndSettle();

      expect(find.textContaining('no se puede registrar la asistencia'), findsOneWidget);
      expect(find.widgetWithText(FilledButton, 'Registrar asistencia'), findsNothing);
    });

    testWidgets('si ya registró hoy avisa la hora y no ofrece registrar', (tester) async {
      final servicio = FakeAsistenciaService()..becado = _becado(hora: '2026-11-16T16:00:00Z');
      await tester.pumpWidget(_app(servicio));

      await tester.enterText(find.byType(TextFormField), '9812241');
      await tester.tap(find.text('Buscar'));
      await tester.pumpAndSettle();

      expect(find.textContaining('Asistencia ya registrada hoy a las 12:00'), findsOneWidget);
      expect(find.widgetWithText(FilledButton, 'Registrar asistencia'), findsNothing);
    });

    testWidgets('un CI que no figura muestra el aviso', (tester) async {
      final servicio = FakeAsistenciaService()..errorAlBuscar = _apiError('ERR_ASI_01');
      await tester.pumpWidget(_app(servicio));

      await tester.enterText(find.byType(TextFormField), '0');
      await tester.tap(find.text('Buscar'));
      await tester.pumpAndSettle();

      expect(find.textContaining('no figura en el padrón de becados'), findsOneWidget);
    });
  });
}
