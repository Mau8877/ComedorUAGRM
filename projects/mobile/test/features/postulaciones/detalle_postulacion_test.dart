import 'package:comedor_uagrm_mobile/core/network/api_client.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/bloques_detalle.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/models/mi_postulacion_detalle.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/models/paso_linea_tiempo.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/screens/detalle_postulacion_screen.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/widgets/linea_de_tiempo.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'postulaciones_logica_test.dart' show detalleJson;
import 'postulaciones_pantallas_test.dart' show ServicioFalso, app;

MiPostulacionDetalle detalle(String estado, [Map<String, dynamic> extra = const {}]) =>
    MiPostulacionDetalle.fromJson(detalleJson(estado: estado, extra: extra));

const _observaciones = {'texto': 'Falta el certificado de notas', 'fecha': '2026-11-15T14:00:00Z'};
const _finalista = {
  'plazoEntrega': '2026-11-20',
  'documentacionRecibida': false,
  'documentosFisicos': 'CI y certificado',
};

Widget pantalla(ServicioFalso servicio, {GoRouter? router}) => app(
      servicio,
      const DetallePostulacionScreen(codigo: 'post-1'),
      router: router ??
          GoRouter(initialLocation: '/mis-postulaciones/post-1', routes: [
            GoRoute(path: '/estudiante', builder: (context, state) => const Scaffold(body: Text('inicio'))),
            GoRoute(
              path: '/mis-postulaciones/:codigo',
              builder: (context, state) => DetallePostulacionScreen(codigo: state.pathParameters['codigo']!),
            ),
          ]),
    );

void main() {
  group('bloquesDelDetalle (qué bloques corresponden a cada estado)', () {
    test('una pendiente o calificada sin información asociada no tiene ningún bloque', () {
      expect(bloquesDelDetalle(detalle('pendiente')), isEmpty);
      expect(bloquesDelDetalle(detalle('calificada')), isEmpty);
    });

    test('devuelta muestra solo las observaciones', () {
      expect(bloquesDelDetalle(detalle('devuelta', {'observaciones': _observaciones, 'puedeCorregir': true})),
          [BloqueDetalle.observaciones]);
    });

    test('archivada y rechazada muestran el motivo', () {
      expect(bloquesDelDetalle(detalle('archivada', {'motivo': 'No entregó'})), [BloqueDetalle.motivo]);
      expect(bloquesDelDetalle(detalle('no_adjudicada', {'motivo': 'Documentos falsos'})), [BloqueDetalle.motivo]);
    });

    test('un finalista suma su bloque; con resultado publicado, también el resultado y nunca el motivo', () {
      expect(bloquesDelDetalle(detalle('calificada', {'finalista': _finalista})), [BloqueDetalle.finalista]);
      expect(bloquesDelDetalle(detalle('adjudicada', {'finalista': _finalista, 'resultado': 'ADJUDICADA'})),
          [BloqueDetalle.finalista, BloqueDetalle.resultado]);
      expect(bloquesDelDetalle(detalle('no_adjudicada', {'resultado': 'NO_ADJUDICADA'})), [BloqueDetalle.resultado]);
    });

    test('un motivo que llegue en un estado que no lo admite no se muestra', () {
      expect(bloquesDelDetalle(detalle('adjudicada', {'motivo': 'reservado', 'resultado': 'ADJUDICADA'})),
          [BloqueDetalle.resultado]);
    });
  });

  group('LineaDeTiempo', () {
    testWidgets('muestra los pasos con su fecha y hora de La Paz, y los pendientes atenuados', (tester) async {
      final pasos = [
        PasoLineaTiempo.fromJson({'estado': 'pendiente', 'fecha': '2026-11-13T15:30:00Z', 'situacion': 'COMPLETADO'}),
        PasoLineaTiempo.fromJson({'estado': 'calificada', 'fecha': '2026-11-14T12:00:00Z', 'situacion': 'COMPLETADO'}),
        PasoLineaTiempo.fromJson({'estado': 'resultado', 'fecha': null, 'situacion': 'PENDIENTE'}),
      ];

      await tester.pumpWidget(MaterialApp(home: Scaffold(body: LineaDeTiempo(pasos: pasos))));

      expect(find.text('Postulación enviada'), findsOneWidget);
      expect(find.text('13/11/2026, 11:30'), findsOneWidget);
      expect(find.text('Calificada'), findsOneWidget);
      expect(find.text('14/11/2026, 08:00'), findsOneWidget);
      expect(find.text('Resultado de la adjudicación'), findsOneWidget);
      expect(find.text('Pendiente'), findsOneWidget);
      double opacidad(String clave) => tester.widget<Opacity>(find.byKey(ValueKey(clave))).opacity;
      expect(opacidad('paso-pendiente-completado'), 1);
      expect(opacidad('paso-resultado-pendiente'), lessThan(1));
      expect(find.byIcon(Icons.check_circle), findsNWidgets(2));
      expect(find.byIcon(Icons.radio_button_unchecked), findsOneWidget);
    });
  });

  group('DetallePostulacionScreen', () {
    testWidgets('muestra la convocatoria, la gestión, la fecha de envío, el estado y la línea de tiempo',
        (tester) async {
      final servicio = ServicioFalso()..detalle = (_) async => detalle('calificada');

      await tester.pumpWidget(pantalla(servicio));
      await tester.pumpAndSettle();

      expect(find.text('Beca 1'), findsOneWidget);
      expect(find.textContaining('Gestión 2026-1 · Enviada el 13/11/2026'), findsOneWidget);
      expect(find.text('Calificada'), findsOneWidget);
      expect(find.text('Recorrido de tu postulación'), findsOneWidget);
      expect(find.text('Postulación enviada'), findsOneWidget);
      expect(find.text('Observaciones de la DUBSS'), findsNothing);
      expect(find.text('Eres finalista'), findsNothing);
    });

    testWidgets('devuelta: observaciones con su fecha y sin «Corregir y reenviar» mientras no exista el CU-02',
        (tester) async {
      final servicio = ServicioFalso()
        ..detalle = (_) async => detalle('devuelta', {'observaciones': _observaciones, 'puedeCorregir': true});

      await tester.pumpWidget(pantalla(servicio));
      await tester.pumpAndSettle();

      expect(find.text('Observaciones de la DUBSS'), findsOneWidget);
      expect(find.text('Falta el certificado de notas'), findsOneWidget);
      expect(find.text('15/11/2026, 10:00'), findsOneWidget);
      expect(find.text('Corregir y reenviar'), findsNothing);
    });

    testWidgets('archivada: muestra el motivo del archivo', (tester) async {
      final servicio = ServicioFalso()..detalle = (_) async => detalle('archivada', {'motivo': 'No entregó'});

      await tester.pumpWidget(pantalla(servicio));
      await tester.pumpAndSettle();

      expect(find.text('Motivo del archivo'), findsOneWidget);
      expect(find.text('No entregó'), findsOneWidget);
    });

    testWidgets('finalista: plazo, documentos y estado de la documentación', (tester) async {
      final servicio = ServicioFalso()..detalle = (_) async => detalle('calificada', {'finalista': _finalista});

      await tester.pumpWidget(pantalla(servicio));
      await tester.pumpAndSettle();

      expect(find.text('Eres finalista'), findsOneWidget);
      expect(find.text('20/11/2026'), findsOneWidget);
      expect(find.text('CI y certificado'), findsOneWidget);
      expect(find.text('Pendiente de entrega'), findsOneWidget);
    });

    testWidgets('adjudicada: el resultado sin motivo y sin botón hacia los resultados (no existe en la app)',
        (tester) async {
      final servicio = ServicioFalso()
        ..detalle = (_) async => detalle('adjudicada', {'resultado': 'ADJUDICADA', 'motivo': 'reservado'});

      await tester.pumpWidget(pantalla(servicio));
      await tester.pumpAndSettle();

      expect(find.textContaining('Obtuviste la Beca Comedor'), findsOneWidget);
      expect(find.text('reservado'), findsNothing);
      expect(find.text('Ver resultados de la convocatoria'), findsNothing);
    });

    testWidgets('ERR_POS_01 muestra «Postulación no encontrada» con la vuelta a la lista', (tester) async {
      final servicio = ServicioFalso()
        ..detalle = (_) async => throw DioException(
              requestOptions: RequestOptions(path: '/postulaciones/post-1'),
              response: Response(requestOptions: RequestOptions(path: '/postulaciones/post-1'), statusCode: 404),
              error: ApiException(code: 'ERR_POS_01', message: 'x'),
            );

      await tester.pumpWidget(pantalla(servicio));
      await tester.pumpAndSettle();

      expect(find.text('Postulación no encontrada'), findsOneWidget);
      expect(find.text('La postulación no existe o no es tuya'), findsOneWidget);
      await tester.tap(find.text('Ir a mis postulaciones'));
      await tester.pumpAndSettle();
      expect(find.text('inicio'), findsOneWidget);
    });

    testWidgets('una falla de conexión muestra «Reintentar», que vuelve a pedir el detalle', (tester) async {
      var intentos = 0;
      final servicio = ServicioFalso()
        ..detalle = (_) async {
          intentos++;
          if (intentos == 1) throw DioException(requestOptions: RequestOptions(path: '/postulaciones/post-1'));
          return detalle('pendiente');
        };

      await tester.pumpWidget(pantalla(servicio));
      await tester.pumpAndSettle();
      expect(find.textContaining('No se pudo conectar'), findsOneWidget);

      await tester.tap(find.text('Reintentar'));
      await tester.pumpAndSettle();

      expect(find.text('Beca 1'), findsOneWidget);
      expect(find.textContaining('No se pudo conectar'), findsNothing);
    });

    testWidgets('el botón de volver lleva al inicio del postulante cuando no hay pantalla anterior', (tester) async {
      final servicio = ServicioFalso()..detalle = (_) async => detalle('pendiente');
      await tester.pumpWidget(pantalla(servicio));
      await tester.pumpAndSettle();

      await tester.tap(find.byType(BackButton));
      await tester.pumpAndSettle();

      expect(find.text('inicio'), findsOneWidget);
    });
  });
}
