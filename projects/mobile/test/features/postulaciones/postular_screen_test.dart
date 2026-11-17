import 'package:comedor_uagrm_mobile/core/network/api_client.dart';
import 'package:comedor_uagrm_mobile/core/theme/app_theme.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/models/borrador_postulacion.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/models/formulario_postulacion.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/models/mi_postulacion_detalle.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/models/pagina_postulaciones.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/providers/documentos_postulacion_provider.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/providers/postulaciones_servicios_provider.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/screens/postular_screen.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/services/postulaciones_service.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'documentos_pantallas_test.dart' show ServicioDocumentosFalso;
import 'postular_logica_test.dart' show complementariosMinimosJson, formularioJson, respuestaJson;

/// Servicio del envío simulado: guarda lo pedido y responde con lo que decida el test.
class ServicioEnvioFalso implements PostulacionesService {
  Future<FormularioPostulacion> Function() alIniciar = () async => FormularioPostulacion.fromJson(formularioJson());
  Future<ComprobantePostulacion> Function(String codigo, EnviarPostulacionRequest r)? alEnviar;
  Future<BorradorGuardado> Function(String codigo, GuardarBorradorRequest r)? alGuardar;
  int iniciados = 0;
  final List<EnviarPostulacionRequest> enviados = [];
  final List<GuardarBorradorRequest> guardados = [];

  @override
  Future<FormularioPostulacion> iniciar() {
    iniciados++;
    return alIniciar();
  }

  @override
  Future<ComprobantePostulacion> enviar(String codigo, EnviarPostulacionRequest respuestas) {
    enviados.add(respuestas);
    return alEnviar!(codigo, respuestas);
  }

  @override
  Future<BorradorGuardado> guardarBorrador(String codigo, GuardarBorradorRequest borrador) {
    guardados.add(borrador);
    return alGuardar!(codigo, borrador);
  }

  @override
  Future<BorradorResumen?> miBorrador() async => null;

  @override
  Future<PaginaPostulaciones> listarMias({String? gestionAcademica, int page = 1, int pageSize = 20, String? sort}) =>
      throw UnimplementedError();

  @override
  Future<MiPostulacionDetalle> obtener(String codigo) => throw UnimplementedError();
}

DioException _apiError(String code, String message) => DioException(
      requestOptions: RequestOptions(path: '/postulaciones'),
      error: ApiException(code: code, message: message),
    );

ComprobantePostulacion _comprobante({bool reenvio = false}) => ComprobantePostulacion.fromJson({
      'codigo': 'post-1',
      'estado': 'pendiente',
      'fechaEnvio': '2026-11-16T15:00:00Z',
      'numeroEnvios': reenvio ? 2 : 1,
      'reenvio': reenvio,
      'convocatoria': 'Convocatoria 2026-II',
      'gestion': '2026-II',
    });

Widget _app(ServicioEnvioFalso servicio) {
  final router = GoRouter(routes: [
    GoRoute(path: '/', builder: (context, state) => const PostularScreen()),
    GoRoute(
      path: '/mis-postulaciones/:codigo',
      builder: (context, state) => Scaffold(body: Text('detalle ${state.pathParameters['codigo']}')),
    ),
  ]);
  return ProviderScope(
    overrides: [
      postulacionesServiceProvider.overrideWithValue(servicio),
      documentosPostulacionServiceProvider.overrideWithValue(ServicioDocumentosFalso()),
    ],
    child: MaterialApp.router(theme: AppTheme.light, routerConfig: router),
  );
}

/// Un teléfono alto: el formulario es largo y los botones quedarían fuera de la pantalla de prueba (800x600).
void _pantallaAlta(WidgetTester tester) {
  tester.view.physicalSize = const Size(800, 5000);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
}

void main() {
  testWidgets('al abrir crea la postulación y muestra el perfil precargado y la convocatoria', (tester) async {
    _pantallaAlta(tester);
    final servicio = ServicioEnvioFalso();

    await tester.pumpWidget(_app(servicio));
    await tester.pumpAndSettle();

    expect(servicio.iniciados, 1);
    expect(find.text('Ing. Informática'), findsOneWidget);
    expect(find.text('1234567'), findsOneWidget);
    expect(find.textContaining('Convocatoria 2026-II · gestión 2026-II'), findsOneWidget);
    expect(find.textContaining('Paso 1 de 3'), findsOneWidget);
  });

  testWidgets('sin convocatoria abierta muestra el aviso del backend y no el formulario', (tester) async {
    _pantallaAlta(tester);
    final servicio = ServicioEnvioFalso()
      ..alIniciar = () async => throw _apiError('ERR_ENV_01', 'No hay una convocatoria abierta para postular.');

    await tester.pumpWidget(_app(servicio));
    await tester.pumpAndSettle();

    expect(find.textContaining('No hay una convocatoria abierta'), findsOneWidget);
    expect(find.textContaining('Paso 1 de 3'), findsNothing);
  });

  testWidgets('«Continuar» con el formulario vacío marca los campos obligatorios y no avanza', (tester) async {
    _pantallaAlta(tester);
    await tester.pumpWidget(_app(ServicioEnvioFalso()));
    await tester.pumpAndSettle();

    await tester.tap(find.widgetWithText(FilledButton, 'Continuar'));
    await tester.pumpAndSettle();
    expect(find.textContaining('Paso 2 de 3'), findsOneWidget);

    await tester.tap(find.widgetWithText(FilledButton, 'Continuar'));
    await tester.pumpAndSettle();

    expect(find.text('Este campo es obligatorio.'), findsWidgets);
    expect(find.textContaining('Paso 2 de 3'), findsOneWidget);
  });

  testWidgets('una devuelta se abre con sus respuestas y se reenvía hasta ver el comprobante', (tester) async {
    _pantallaAlta(tester);
    final servicio = ServicioEnvioFalso()
      ..alIniciar = (() async =>
          FormularioPostulacion.fromJson(formularioJson(estado: 'devuelta', respuesta: respuestaJson())))
      ..alEnviar = (_, _) async => _comprobante(reenvio: true);

    await tester.pumpWidget(_app(servicio));
    await tester.pumpAndSettle();
    expect(find.textContaining('fue devuelta para corregirla'), findsOneWidget);

    await tester.tap(find.widgetWithText(FilledButton, 'Continuar'));
    await tester.pumpAndSettle();
    expect(find.text('1800'), findsOneWidget);
    expect(find.text('72.5'), findsOneWidget);

    await tester.tap(find.widgetWithText(FilledButton, 'Continuar'));
    await tester.pumpAndSettle();
    expect(find.textContaining('Paso 3 de 3'), findsOneWidget);
    expect(find.text('Carnet de identidad'), findsOneWidget);

    await tester.tap(find.widgetWithText(FilledButton, 'Enviar postulación'));
    await tester.pumpAndSettle();
    expect(find.text('¿Enviar tu postulación?'), findsOneWidget);
    await tester.tap(find.text('Confirmar envío'));
    await tester.pumpAndSettle();

    expect(find.text('Postulación reenviada'), findsOneWidget);
    expect(servicio.enviados, hasLength(1));
    expect(servicio.enviados.single.toJson(), respuestaJson());

    await tester.tap(find.text('Ver mi postulación'));
    await tester.pumpAndSettle();
    expect(find.text('detalle post-1'), findsOneWidget);
  });

  testWidgets('si el backend dice que faltan documentos muestra el motivo y deja corregir', (tester) async {
    _pantallaAlta(tester);
    final servicio = ServicioEnvioFalso()
      ..alIniciar = (() async => FormularioPostulacion.fromJson(formularioJson(respuesta: respuestaJson())))
      ..alEnviar = (_, _) async => throw _apiError('ERR_ENV_04', 'Falta completar documentos: Certificado de notas');

    await tester.pumpWidget(_app(servicio));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Continuar'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Continuar'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Enviar postulación'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Confirmar envío'));
    await tester.pumpAndSettle();

    expect(find.text('Falta completar documentos: Certificado de notas'), findsOneWidget);
    expect(find.text('Postulación enviada'), findsNothing);
    expect(find.widgetWithText(FilledButton, 'Enviar postulación'), findsOneWidget);
  });

  group('formulario oficial de la DUBSS', () {
    Future<void> irAlFormulario(WidgetTester tester, ServicioEnvioFalso servicio) async {
      _pantallaAlta(tester);
      await tester.pumpWidget(_app(servicio));
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Continuar'));
      await tester.pumpAndSettle();
    }

    testWidgets('muestra las secciones del formulario en papel', (tester) async {
      await irAlFormulario(tester, ServicioEnvioFalso());

      for (final titulo in ['Dependencia económica', 'Grupo familiar', 'Residencia y vivienda', 'Otros datos']) {
        expect(find.text(titulo), findsOneWidget);
      }
      expect(find.text('¿De quién depende usted? *'), findsOneWidget);
      expect(find.text('Tenencia de la vivienda *'), findsOneWidget);
      expect(find.text('Aclaración o comentario personal'), findsOneWidget);
    });

    testWidgets('«Continuar» sin contestar lo obligatorio del formulario oficial no avanza', (tester) async {
      await irAlFormulario(tester, ServicioEnvioFalso());

      await tester.tap(find.widgetWithText(FilledButton, 'Continuar'));
      await tester.pumpAndSettle();

      expect(find.text('Selecciona una opción.'), findsWidgets);
      expect(find.textContaining('Paso 2 de 3'), findsOneWidget);
    });

    testWidgets('«¿Cuántos?» solo aparece si dijo que tiene hijos', (tester) async {
      final conHijos = {...respuestaJson(), 'complementarios': {...complementariosMinimosJson(), 'tieneHijos': true}};
      await irAlFormulario(
        tester,
        ServicioEnvioFalso()..alIniciar = () async => FormularioPostulacion.fromJson(formularioJson(respuesta: conHijos)),
      );
      expect(find.text('¿Cuántos? *'), findsOneWidget);
    });

    testWidgets('sin hijos no pregunta cuántos', (tester) async {
      await irAlFormulario(
        tester,
        ServicioEnvioFalso()
          ..alIniciar = () async => FormularioPostulacion.fromJson(formularioJson(respuesta: respuestaJson())),
      );
      expect(find.text('¿Cuántos? *'), findsNothing);
    });

    testWidgets('agrega y quita integrantes del grupo familiar, hasta 10', (tester) async {
      await irAlFormulario(tester, ServicioEnvioFalso());
      expect(find.text('Familiar 1'), findsNothing);

      for (var i = 0; i < 10; i++) {
        await tester.tap(find.widgetWithText(OutlinedButton, 'Agregar familiar'));
        await tester.pumpAndSettle();
      }
      expect(find.text('10 de 10'), findsOneWidget);
      expect(tester.widget<OutlinedButton>(find.widgetWithText(OutlinedButton, 'Agregar familiar')).onPressed, isNull);

      await tester.tap(find.text('Quitar familiar 10'));
      await tester.pumpAndSettle();
      expect(find.text('9 de 10'), findsOneWidget);
    });

    testWidgets('envía el grupo familiar con la forma del backend', (tester) async {
      final servicio = ServicioEnvioFalso()
        ..alIniciar = (() async => FormularioPostulacion.fromJson(formularioJson(respuesta: respuestaJson())))
        ..alEnviar = (_, _) async => _comprobante();
      await irAlFormulario(tester, servicio);

      await tester.tap(find.widgetWithText(OutlinedButton, 'Agregar familiar'));
      await tester.pumpAndSettle();
      await tester.enterText(find.widgetWithText(TextFormField, 'Nombre completo'), 'María');
      await tester.enterText(find.widgetWithText(TextFormField, 'Edad'), '45');
      await tester.tap(find.widgetWithText(FilledButton, 'Continuar'));
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Enviar postulación'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Confirmar envío'));
      await tester.pumpAndSettle();

      final enviado = servicio.enviados.single.toJson();
      expect((enviado['complementarios'] as Map)['grupoFamiliar'], [
        {'nombre': 'María', 'parentesco': null, 'edad': 45, 'ocupacion': null, 'observacion': null},
      ]);
    });
  });

  group('guardar borrador (CU-17)', () {
    BorradorGuardado guardado() => BorradorGuardado.fromJson(
        {'postulacion': 'post-1', 'guardadoEn': '2026-07-12T18:32:00Z', 'camposPendientes': 5});

    Future<void> irAlFormulario(WidgetTester tester, ServicioEnvioFalso servicio) async {
      _pantallaAlta(tester);
      await tester.pumpWidget(_app(servicio));
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Continuar'));
      await tester.pumpAndSettle();
    }

    testWidgets('guarda lo escrito con los campos vacíos en null y muestra la hora, sin avanzar de paso',
        (tester) async {
      final servicio = ServicioEnvioFalso()..alGuardar = (_, _) async => guardado();
      await irAlFormulario(tester, servicio);

      await tester.enterText(find.widgetWithText(TextFormField, 'Ingresos familiares mensuales (Bs) *'), '1800');
      await tester.tap(find.widgetWithText(OutlinedButton, 'Guardar borrador'));
      await tester.pumpAndSettle();

      expect(servicio.guardados.length, 1);
      expect(servicio.guardados.single.ingresosFamiliares, 1800);
      expect(servicio.guardados.single.dependientes, isNull);
      expect(find.textContaining('Borrador guardado · 14:32'), findsOneWidget);
      expect(find.textContaining('Paso 2 de 3'), findsOneWidget);
    });

    testWidgets('un valor mal escrito no se guarda y se señala', (tester) async {
      final servicio = ServicioEnvioFalso()..alGuardar = (_, _) async => guardado();
      await irAlFormulario(tester, servicio);

      await tester.enterText(find.widgetWithText(TextFormField, 'Promedio académico *'), 'abc');
      await tester.tap(find.widgetWithText(OutlinedButton, 'Guardar borrador'));
      await tester.pumpAndSettle();

      expect(servicio.guardados, isEmpty);
      expect(find.textContaining('No se guardó el borrador'), findsOneWidget);
    });

    testWidgets('al retomar un borrador ya guardado muestra la hora del último guardado y sus datos',
        (tester) async {
      final servicio = ServicioEnvioFalso()
        ..alIniciar = () async => FormularioPostulacion.fromJson(
            formularioJson(respuesta: {'ingresosFamiliares': 1800, 'guardadoEn': '2026-07-12T18:32:00Z'}));
      await irAlFormulario(tester, servicio);

      expect(find.text('1800'), findsOneWidget);
      expect(find.textContaining('Borrador guardado · 14:32'), findsOneWidget);
    });

    testWidgets('si el backend rechaza el guardado (convocatoria cerrada) muestra el motivo', (tester) async {
      final servicio = ServicioEnvioFalso()..alGuardar = (_, _) => throw _apiError('ERR_VAL_03', 'El plazo finalizó');
      await irAlFormulario(tester, servicio);

      await tester.tap(find.widgetWithText(OutlinedButton, 'Guardar borrador'));
      await tester.pumpAndSettle();

      expect(find.textContaining('el plazo finalizó'), findsOneWidget);
    });
  });
}
