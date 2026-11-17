import 'package:comedor_uagrm_mobile/features/postulaciones/formatos.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/models/borrador_postulacion.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/models/formulario_postulacion.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/services/postulaciones_service.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/validacion_formulario.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

/// Un `HttpClientAdapter` que anota la petición y responde con lo que se le pida (el interceptor del sobre vive en
/// `ApiClient`, así que acá `data` ya es lo desempaquetado).
class _Adaptador implements HttpClientAdapter {
  RequestOptions? ultima;
  final Object? datos;

  _Adaptador(this.datos);

  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<List<int>>? requestStream, Future<void>? cancelFuture) async {
    ultima = options;
    return ResponseBody.fromString(datos == null ? 'null' : _json(datos!), 200, headers: {
      Headers.contentTypeHeader: [Headers.jsonContentType],
    });
  }

  String _json(Object o) => Transformer.isJsonMimeType(Headers.jsonContentType) ? _codificar(o) : '';

  @override
  void close({bool force = false}) {}
}

String _codificar(Object o) {
  // Suficiente para los mapas planos de estos tests.
  final entradas = (o as Map<String, dynamic>).entries.map((e) {
    final v = e.value;
    return '"${e.key}":${v == null ? 'null' : v is String ? '"$v"' : v}';
  });
  return '{${entradas.join(',')}}';
}

void main() {
  group('modelos del borrador (CU-17)', () {
    test('GuardarBorradorRequest.toJson lleva los seis campos, los vacíos en null', () {
      const r = GuardarBorradorRequest(ingresosFamiliares: 1800, procedencia: 'Provincia');

      expect(r.toJson(), {
        'ingresosFamiliares': 1800,
        'dependientes': null,
        'procedencia': 'Provincia',
        'patrimonio': null,
        'estadoVulnerabilidad': null,
        'promedioAcademico': null,
        'complementarios': null,
      });
    });

    test('BorradorGuardado.fromJson', () {
      final g = BorradorGuardado.fromJson({
        'postulacion': 'post-1',
        'guardadoEn': '2026-07-12T18:32:00Z',
        'camposPendientes': 5,
      });

      expect(g.guardadoEn, DateTime.utc(2026, 7, 12, 18, 32));
      expect(g.camposPendientes, 5);
    });

    test('BorradorResumen.fromJson y su resumen de avance', () {
      final b = BorradorResumen.fromJson({
        'postulacion': 'post-1',
        'convocatoria': 'Convocatoria 2026-II',
        'gestion': '2026-II',
        'fechaCierre': '2026-07-31T20:00:00Z',
        'guardadoEn': null,
        'camposPendientes': 3,
        'documentosPendientes': 1,
      });

      expect(b.guardadoEn, isNull);
      expect(b.resumenAvance, 'Faltan 3 campos y 1 documento.');
    });

    test('el resumen de avance en singular y con todo completo', () {
      BorradorResumen con(int campos, int documentos) => BorradorResumen(
            postulacion: 'p',
            convocatoria: 'c',
            gestion: 'g',
            fechaCierre: DateTime.utc(2026, 7, 31),
            guardadoEn: null,
            camposPendientes: campos,
            documentosPendientes: documentos,
          );

      expect(con(1, 0).resumenAvance, 'Faltan 1 campo.');
      expect(con(0, 2).resumenAvance, 'Faltan 2 documentos.');
      expect(con(0, 0).resumenAvance, 'No falta nada: ya puedes enviarlo.');
    });

    test('RespuestaSocioeconomica.fromJson trae la hora del último guardado', () {
      final r = RespuestaSocioeconomica.fromJson({'ingresosFamiliares': 1800, 'guardadoEn': '2026-07-12T18:32:00Z'});

      expect(r.guardadoEn, DateTime.utc(2026, 7, 12, 18, 32));
      expect(r.dependientes, isNull);
    });

    test('formatearHora usa la hora de La Paz (UTC-4)', () {
      expect(formatearHora(DateTime.utc(2026, 7, 12, 18, 32)), '14:32');
    });
  });

  group('ValoresFormulario.aBorrador', () {
    test('el formulario vacío es un borrador válido: todo va en null', () {
      final r = const ValoresFormulario().aBorrador();

      expect(r.esValido, isTrue);
      final json = r.borrador!.toJson();
      expect(json.entries.where((e) => e.key != 'complementarios').every((e) => e.value == null), isTrue);
      // Un borrador no exige nada del formulario oficial: nada contestado, todo en null.
      final complementarios = json['complementarios'] as Map<String, dynamic>;
      expect(complementarios['dependenciaEconomica'], isNull);
      expect(complementarios['tieneHijos'], isNull);
      expect(complementarios['grupoFamiliar'], isNull);
    });

    test('guarda lo ya contestado del formulario oficial sin exigir el resto', () {
      final r = const ValoresFormulario(
        complementarios: ValoresComplementarios(tieneHijos: true, rangoIngresos: 'Menor de Bs. 2500'),
      ).aBorrador();

      expect(r.esValido, isTrue);
      expect(r.borrador!.complementarios!.tieneHijos, isTrue);
      expect(r.borrador!.complementarios!.rangoIngresos, 'Menor de Bs. 2500');
      expect(r.borrador!.complementarios!.cantidadHijos, isNull);
    });

    test('un valor mal escrito del formulario oficial se señala y no genera borrador', () {
      final r = const ValoresFormulario(complementarios: ValoresComplementarios(dormitorios: 'mucho')).aBorrador();

      expect(r.esValido, isFalse);
      expect(r.errores.single, startsWith('dormitorios:'));
    });

    test('convierte a número lo escrito y deja en null lo vacío', () {
      final r = const ValoresFormulario(ingresosFamiliares: '1800,50', procedencia: 'Provincia', dependientes: ' ')
          .aBorrador();

      expect(r.esValido, isTrue);
      expect(r.borrador!.ingresosFamiliares, 1800.5);
      expect(r.borrador!.procedencia, 'Provincia');
      expect(r.borrador!.dependientes, isNull);
      expect(r.borrador!.promedioAcademico, isNull);
    });

    test('un valor escrito que no cumple su regla se señala y no genera borrador', () {
      final r = const ValoresFormulario(promedioAcademico: '120', dependientes: '4,5').aBorrador();

      expect(r.esValido, isFalse);
      expect(r.errores.length, 2);
      expect(r.errores.first, startsWith('dependientes:'));
      expect(r.errores.last, startsWith('promedio académico:'));
    });
  });

  group('PostulacionesService (CU-17)', () {
    test('guardarBorrador hace PUT /postulaciones/{codigo}/borrador con el formulario', () async {
      final adaptador = _Adaptador({
        'postulacion': 'post-1',
        'guardadoEn': '2026-07-12T18:32:00Z',
        'camposPendientes': 5,
      });
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost/api/v1'))..httpClientAdapter = adaptador;

      final g = await PostulacionesService(dio)
          .guardarBorrador('post-1', const GuardarBorradorRequest(ingresosFamiliares: 1800));

      expect(adaptador.ultima!.method, 'PUT');
      expect(adaptador.ultima!.path, '/postulaciones/post-1/borrador');
      expect((adaptador.ultima!.data as Map<String, dynamic>)['ingresosFamiliares'], 1800);
      expect(g.camposPendientes, 5);
    });

    test('miBorrador devuelve null cuando no hay borrador', () async {
      final adaptador = _Adaptador(null);
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost/api/v1'))..httpClientAdapter = adaptador;

      expect(await PostulacionesService(dio).miBorrador(), isNull);
      expect(adaptador.ultima!.path, '/postulaciones/mi-borrador');
    });

    test('miBorrador devuelve el resumen cuando hay uno', () async {
      final adaptador = _Adaptador({
        'postulacion': 'post-1',
        'convocatoria': 'Convocatoria 2026-II',
        'gestion': '2026-II',
        'fechaCierre': '2026-07-31T20:00:00Z',
        'guardadoEn': '2026-07-12T18:32:00Z',
        'camposPendientes': 3,
        'documentosPendientes': 0,
      });
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost/api/v1'))..httpClientAdapter = adaptador;

      final b = await PostulacionesService(dio).miBorrador();

      expect(b!.convocatoria, 'Convocatoria 2026-II');
      expect(b.resumenAvance, 'Faltan 3 campos.');
    });
  });
}
