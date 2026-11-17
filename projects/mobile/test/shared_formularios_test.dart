import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:comedor_uagrm_mobile/shared/widgets/campo_formulario.dart';
import 'package:comedor_uagrm_mobile/shared/widgets/selector_buscable.dart';

Widget _app(Widget hijo) => MaterialApp(
  home: Scaffold(body: SingleChildScrollView(child: hijo)),
);

void main() {
  group('CampoFormulario', () {
    testWidgets(
      'muestra el error al salir del campo, no al escribir, y lo quita al corregir',
      (tester) async {
        final claveForm = GlobalKey<FormState>();
        final c1 = TextEditingController();
        final c2 = TextEditingController();
        await tester.pumpWidget(
          _app(
            Form(
              key: claveForm,
              child: Column(
                children: [
                  CampoFormulario(
                    controller: c1,
                    decoration: const InputDecoration(labelText: 'Correo'),
                    validator: (v) =>
                        (v ?? '').contains('@') ? null : 'Correo inválido',
                  ),
                  CampoFormulario(
                    controller: c2,
                    decoration: const InputDecoration(labelText: 'Otro'),
                  ),
                ],
              ),
            ),
          ),
        );

        await tester.tap(find.widgetWithText(TextFormField, 'Correo'));
        await tester.enterText(
          find.widgetWithText(TextFormField, 'Correo'),
          'abc',
        );
        await tester.pump();
        expect(find.text('Correo inválido'), findsNothing);

        await tester.tap(find.widgetWithText(TextFormField, 'Otro'));
        await tester.pump();
        expect(find.text('Correo inválido'), findsOneWidget);

        await tester.enterText(
          find.widgetWithText(TextFormField, 'Correo'),
          'a@b',
        );
        await tester.pump();
        expect(find.text('Correo inválido'), findsNothing);
      },
    );

    testWidgets('digitsOnly descarta letras al teclear', (tester) async {
      final c = TextEditingController();
      await tester.pumpWidget(
        _app(
          CampoFormulario(
            controller: c,
            inputFormatters: [FilteringTextInputFormatter.digitsOnly],
          ),
        ),
      );
      await tester.enterText(find.byType(TextFormField), '12ab3');
      expect(c.text, '123');
    });
  });

  group('SelectorBuscable', () {
    testWidgets(
      'con más de 8 opciones abre un buscador que filtra y selecciona',
      (tester) async {
        String? elegido;
        final opciones = [
          for (var i = 0; i < 12; i++) OpcionSelector('$i', 'Carrera $i'),
        ];
        await tester.pumpWidget(
          _app(
            StatefulBuilder(
              builder: (context, setState) => SelectorBuscable(
                label: 'Carrera',
                opciones: opciones,
                valor: elegido,
                onChanged: (v) => setState(() => elegido = v),
              ),
            ),
          ),
        );

        await tester.tap(find.byType(InputDecorator));
        await tester.pumpAndSettle();
        expect(find.byType(TextField), findsOneWidget);

        await tester.enterText(find.byType(TextField), 'Carrera 11');
        await tester.pump();
        expect(find.text('Carrera 3'), findsNothing);

        await tester.tap(find.widgetWithText(ListTile, 'Carrera 11'));
        await tester.pumpAndSettle();
        expect(elegido, '11');
      },
    );

    testWidgets('con 8 opciones o menos es un desplegable sin buscador', (
      tester,
    ) async {
      final opciones = [
        for (var i = 0; i < 5; i++) OpcionSelector('$i', 'Opción $i'),
      ];
      await tester.pumpWidget(
        _app(
          SelectorBuscable(
            label: 'Rol',
            opciones: opciones,
            valor: null,
            onChanged: (_) {},
          ),
        ),
      );
      expect(find.byType(DropdownButtonFormField<String>), findsOneWidget);
    });
  });
}
