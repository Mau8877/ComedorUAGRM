import 'package:comedor_uagrm_mobile/core/theme/app_theme.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/models/mi_postulacion_detalle.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/models/pagina_postulaciones.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/providers/postulaciones_servicios_provider.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/services/postulaciones_service.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/widgets/mis_postulaciones_vista.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'postulaciones_logica_test.dart' show metaJson, resumenJson;

/// Servicio simulado: guarda lo pedido y responde con lo que decida el test.
class ServicioFalso implements PostulacionesService {
  final List<({String? gestion, int page})> llamadas = [];
  Future<PaginaPostulaciones> Function(String? gestion, int page) listar = (_, _) async =>
      PaginaPostulaciones.fromJson({'data': <dynamic>[], 'meta': metaJson(totalItems: 0, totalPages: 0)});
  Future<MiPostulacionDetalle> Function(String codigo)? detalle;

  @override
  Future<PaginaPostulaciones> listarMias({String? gestionAcademica, int page = 1, int pageSize = 20, String? sort}) {
    llamadas.add((gestion: gestionAcademica, page: page));
    return listar(gestionAcademica, page);
  }

  @override
  Future<MiPostulacionDetalle> obtener(String codigo) => detalle!(codigo);
}

PaginaPostulaciones pagina(List<Map<String, dynamic>> filas, {int page = 1, int totalPages = 1}) =>
    PaginaPostulaciones.fromJson({
      'data': filas,
      'meta': metaJson(page: page, totalItems: filas.length, totalPages: totalPages),
    });

Widget app(ServicioFalso servicio, Widget vista, {GoRouter? router}) {
  final rutas = router ??
      GoRouter(routes: [
        GoRoute(path: '/', builder: (context, state) => Scaffold(body: vista)),
        GoRoute(
          path: '/mis-postulaciones/:codigo',
          builder: (context, state) => Scaffold(body: Text('detalle ${state.pathParameters['codigo']}')),
        ),
      ]);
  return ProviderScope(
    overrides: [postulacionesServiceProvider.overrideWithValue(servicio)],
    child: MaterialApp.router(theme: AppTheme.light, routerConfig: rutas),
  );
}

void main() {
  group('MisPostulacionesVista', () {
    testWidgets('muestra cada postulación con su convocatoria, gestión, fecha de envío y estado visible',
        (tester) async {
      final servicio = ServicioFalso()
        ..listar = (_, _) async => pagina([resumenJson(1, estado: 'calificada'), resumenJson(2, estado: 'devuelta')]);

      await tester.pumpWidget(app(servicio, const MisPostulacionesVista()));
      await tester.pumpAndSettle();

      expect(find.text('Beca 1'), findsOneWidget);
      expect(find.text('Beca 2'), findsOneWidget);
      expect(find.textContaining('Gestión 2026-1 · Enviada el 13/11/2026'), findsOneWidget);
      expect(find.text('Calificada'), findsOneWidget);
      expect(find.text('Devuelta'), findsOneWidget);
    });

    testWidgets('sin postulaciones muestra el mensaje y no invita a postular mientras no exista el CU-02',
        (tester) async {
      await tester.pumpWidget(app(ServicioFalso(), const MisPostulacionesVista()));
      await tester.pumpAndSettle();

      expect(find.text('Todavía no enviaste ninguna postulación'), findsOneWidget);
      expect(find.text('Postular'), findsNothing);
      expect(find.byType(ChoiceChip), findsNothing);
    });

    testWidgets('tocar una tarjeta abre el detalle de esa postulación', (tester) async {
      final servicio = ServicioFalso()..listar = (_, _) async => pagina([resumenJson(1), resumenJson(2)]);
      await tester.pumpWidget(app(servicio, const MisPostulacionesVista()));
      await tester.pumpAndSettle();

      await tester.tap(find.text('Beca 2'));
      await tester.pumpAndSettle();

      expect(find.text('detalle post-2'), findsOneWidget);
    });

    testWidgets('el filtro por gestión recarga desde la página 1 con la gestión elegida', (tester) async {
      final servicio = ServicioFalso()
        ..listar = (gestion, _) async =>
            gestion == null ? pagina([resumenJson(1), resumenJson(2)]) : pagina([resumenJson(2)]);
      await tester.pumpWidget(app(servicio, const MisPostulacionesVista()));
      await tester.pumpAndSettle();
      expect(find.byType(ChoiceChip), findsNWidgets(3)); // Todas + dos gestiones

      await tester.tap(find.widgetWithText(ChoiceChip, '2026-2'));
      await tester.pumpAndSettle();

      expect(servicio.llamadas.last, (gestion: 'gestion-2', page: 1));
      expect(find.text('Beca 1'), findsNothing);
      expect(find.text('Beca 2'), findsOneWidget);
      // Las demás gestiones siguen disponibles para volver a elegirlas.
      expect(find.widgetWithText(ChoiceChip, '2026-1'), findsOneWidget);
    });

    testWidgets('un filtro sin resultados dice que no hay postulaciones en esa gestión', (tester) async {
      final servicio = ServicioFalso()
        ..listar = (gestion, _) async => gestion == null ? pagina([resumenJson(1)]) : pagina([]);
      await tester.pumpWidget(app(servicio, const MisPostulacionesVista()));
      await tester.pumpAndSettle();

      await tester.tap(find.widgetWithText(ChoiceChip, '2026-1'));
      await tester.pumpAndSettle();

      expect(find.text('No hay postulaciones en esa gestión'), findsOneWidget);
      expect(find.text('Todavía no enviaste ninguna postulación'), findsNothing);
    });

    testWidgets('una falla de conexión muestra el mensaje con «Reintentar», que vuelve a pedir', (tester) async {
      var intentos = 0;
      final servicio = ServicioFalso()
        ..listar = (_, _) async {
          intentos++;
          if (intentos == 1) throw DioException(requestOptions: RequestOptions(path: '/postulaciones/mias'));
          return pagina([resumenJson(1)]);
        };
      await tester.pumpWidget(app(servicio, const MisPostulacionesVista()));
      await tester.pumpAndSettle();

      expect(find.textContaining('No se pudo conectar'), findsOneWidget);
      await tester.tap(find.text('Reintentar'));
      await tester.pumpAndSettle();

      expect(find.text('Beca 1'), findsOneWidget);
      expect(find.textContaining('No se pudo conectar'), findsNothing);
    });

    testWidgets('al llegar al final pide la página siguiente y la suma', (tester) async {
      final servicio = ServicioFalso()
        ..listar = (_, page) async => page == 1
            ? pagina([for (var i = 1; i <= 12; i++) resumenJson(i)], page: 1, totalPages: 2)
            : pagina([resumenJson(13)], page: 2, totalPages: 2);
      await tester.pumpWidget(app(servicio, const MisPostulacionesVista()));
      await tester.pumpAndSettle();
      expect(servicio.llamadas, hasLength(1));

      await tester.drag(find.byType(ListView), const Offset(0, -3000));
      await tester.pumpAndSettle();

      expect(servicio.llamadas.map((l) => l.page), [1, 2]);
      expect(find.text('Beca 13'), findsOneWidget);
    });
  });
}
