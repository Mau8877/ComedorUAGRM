import 'package:comedor_uagrm_mobile/core/network/api_client.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/envio_error_mapper.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/models/datos_complementarios.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/models/formulario_postulacion.dart';
import 'package:comedor_uagrm_mobile/features/postulaciones/validacion_formulario.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

Map<String, dynamic> formularioJson({String estado = 'borrador', Map<String, dynamic>? respuesta}) => {
      'postulacion': 'post-1',
      'estado': estado,
      'numeroEnvios': estado == 'devuelta' ? 1 : 0,
      'convocatoria': {
        'codigo': 'conv-1',
        'nombre': 'Convocatoria 2026-II',
        'gestion': '2026-II',
        'fechaCierre': '2026-12-20T20:00:00Z',
      },
      'perfil': {'nombres': 'Pia', 'apellidos': 'Perez', 'ci': '1234567', 'carrera': 'Ing. Informática'},
      'opciones': {
        'procedencias': ['Capital', 'Provincia'],
        'patrimonios': ['Sin vivienda propia', 'Vivienda propia'],
        'estadosVulnerabilidad': ['Hogar monoparental', 'Sin situación especial'],
        'dependencias': ['Independiente', 'Solo de padre o madre', 'Otro familiar', 'De la pareja', 'Ambos padres'],
        'ocupacionesSosten': ['Asalariado formal', 'Asalariado informal', 'Comerciante minorista', 'Rentista'],
        'rangosIngresos': ['Menor de Bs. 2500', 'De Bs. 2501 a Bs. 4000', 'De Bs. 4001 a Bs. 6000', 'Más de Bs. 6000'],
        'cantidadesHijos': ['1', 'Más de 1'],
        'tenenciasVivienda': ['Herencia', 'De los padres', 'Cedida', 'Anticrético', 'Alquiler'],
      },
      'respuesta': respuesta,
    };

Map<String, dynamic> respuestaJson() => {
      'ingresosFamiliares': 1800,
      'dependientes': 4,
      'procedencia': 'Provincia',
      'patrimonio': 'Sin vivienda propia',
      'estadoVulnerabilidad': 'Hogar monoparental',
      'promedioAcademico': 72.5,
      'complementarios': complementariosMinimosJson(),
    };

/// Solo lo obligatorio del formulario oficial contestado; lo opcional vacío (así lo arma la app).
Map<String, dynamic> complementariosMinimosJson() => {
      'dependenciaEconomica': 'Solo de padre o madre',
      'ocupacionSosten': 'Comerciante minorista',
      'rangoIngresos': 'De Bs. 2501 a Bs. 4000',
      'tieneHijos': false,
      'cantidadHijos': null,
      'grupoFamiliar': null,
      'residencia': {'provincia': null, 'zonaAnillo': null, 'barrio': null, 'calle': null},
      'tenenciaVivienda': 'Alquiler',
      'infraestructura': {'dormitorios': null, 'banos': null, 'comedor': null, 'sala': null, 'patio': null},
      'otroBeneficio': false,
      'otroBeneficioDetalle': null,
      'discapacidad': false,
      'discapacidadDetalle': null,
      'comentario': null,
    };

DioException _apiError(String code, String message) => DioException(
      requestOptions: RequestOptions(path: '/postulaciones'),
      error: ApiException(code: code, message: message),
    );

void main() {
  group('modelos del envío (CU-02)', () {
    test('FormularioPostulacion.fromJson sin respuestas', () {
      final f = FormularioPostulacion.fromJson(formularioJson());

      expect(f.postulacion, 'post-1');
      expect(f.esReenvio, isFalse);
      expect(f.perfil.carrera, 'Ing. Informática');
      expect(f.convocatoria.gestion, '2026-II');
      expect(f.opciones.procedencias, ['Capital', 'Provincia']);
      expect(f.respuesta, isNull);
    });

    test('FormularioPostulacion.fromJson de una devuelta trae sus respuestas', () {
      final f = FormularioPostulacion.fromJson(formularioJson(estado: 'devuelta', respuesta: respuestaJson()));

      expect(f.esReenvio, isTrue);
      expect(f.respuesta!.dependientes, 4);
      expect(f.respuesta!.promedioAcademico, 72.5);
      expect(f.respuesta!.complementarios!.tenenciaVivienda, 'Alquiler');
    });

    test('ComprobantePostulacion.fromJson', () {
      final c = ComprobantePostulacion.fromJson({
        'codigo': 'post-1',
        'estado': 'pendiente',
        'fechaEnvio': '2026-11-16T15:00:00Z',
        'numeroEnvios': 2,
        'reenvio': true,
        'convocatoria': 'Convocatoria 2026-II',
        'gestion': '2026-II',
      });

      expect(c.reenvio, isTrue);
      expect(c.fechaEnvio, DateTime.utc(2026, 11, 16, 15));
    });

    test('EnviarPostulacionRequest.toJson lleva los seis campos y los datos del formulario oficial', () {
      final r = EnviarPostulacionRequest(
        ingresosFamiliares: 1800,
        dependientes: 4,
        procedencia: 'Provincia',
        patrimonio: 'Sin vivienda propia',
        estadoVulnerabilidad: 'Hogar monoparental',
        promedioAcademico: 72.5,
        complementarios: DatosComplementarios.fromJson(complementariosMinimosJson()),
      );

      expect(r.toJson(), respuestaJson());
    });

    test('las opciones del formulario oficial llegan del backend (y faltan sin error en un backend viejo)', () {
      final f = FormularioPostulacion.fromJson(formularioJson());

      expect(f.opciones.dependencias, contains('Ambos padres'));
      expect(f.opciones.rangosIngresos, hasLength(4));
      expect(f.opciones.tenenciasVivienda, contains('Alquiler'));

      final viejo = OpcionesFormulario.fromJson({
        'procedencias': ['Capital'],
        'patrimonios': ['Vivienda propia'],
        'estadosVulnerabilidad': ['Sin situación especial'],
      });
      expect(viejo.dependencias, isEmpty);
    });
  });

  group('validaciones del formulario', () {
    test('todos los campos vacíos son obligatorios', () {
      expect(validarIngresos(''), mensajeObligatorio);
      expect(validarDependientes(' '), mensajeObligatorio);
      expect(validarPromedio(null), mensajeObligatorio);
      expect(validarOpcion(null), isNotNull);
      expect(validarOpcion(''), isNotNull);
    });

    test('ingresos: monto positivo con hasta 2 decimales', () {
      expect(validarIngresos('1800'), isNull);
      expect(validarIngresos('1800,50'), isNull);
      expect(validarIngresos('0'), isNull);
      expect(validarIngresos('-5'), isNotNull);
      expect(validarIngresos('12.345'), isNotNull);
      expect(validarIngresos('mil'), isNotNull);
    });

    test('dependientes: entero entre 0 y 50', () {
      expect(validarDependientes('0'), isNull);
      expect(validarDependientes('50'), isNull);
      expect(validarDependientes('51'), isNotNull);
      expect(validarDependientes('2.5'), isNotNull);
    });

    test('promedio: entre 0 y 100 con hasta 2 decimales', () {
      expect(validarPromedio('0'), isNull);
      expect(validarPromedio('100'), isNull);
      expect(validarPromedio('72,5'), isNull);
      expect(validarPromedio('101'), isNotNull);
      expect(validarPromedio('72.555'), isNotNull);
      expect(validarPromedio('abc'), isNotNull);
    });

    test('ValoresFormulario.desde precarga las respuestas guardadas como texto', () {
      final v = ValoresFormulario.desde(RespuestaSocioeconomica.fromJson(respuestaJson()));

      expect(v.ingresosFamiliares, '1800');
      expect(v.dependientes, '4');
      expect(v.promedioAcademico, '72.5');
      expect(v.procedencia, 'Provincia');
      expect(v.esValido, isTrue);
    });

    test('ValoresFormulario.desde sin respuestas queda vacío e inválido', () {
      final v = ValoresFormulario.desde(null);

      expect(v.ingresosFamiliares, '');
      expect(v.procedencia, isNull);
      expect(v.esValido, isFalse);
    });

    test('aRequest convierte el texto a números (coma decimal incluida)', () {
      const v = ValoresFormulario(
        ingresosFamiliares: '1800,50',
        dependientes: '4',
        procedencia: 'Provincia',
        patrimonio: 'Sin vivienda propia',
        estadoVulnerabilidad: 'Hogar monoparental',
        promedioAcademico: '72,5',
      );

      final r = v.aRequest();

      expect(r.ingresosFamiliares, 1800.5);
      expect(r.dependientes, 4);
      expect(r.promedioAcademico, 72.5);
    });
  });

  group('datos del formulario oficial (ValoresComplementarios)', () {
    ValoresComplementarios minimos() => ValoresComplementarios.desde(DatosComplementarios.fromJson(complementariosMinimosJson()));

    test('sin datos guardados faltan los siete obligatorios', () {
      expect(const ValoresComplementarios().faltantes(), [
        'de quién depende',
        'ocupación',
        'rango de ingresos',
        'si tiene hijos',
        'tenencia de la vivienda',
        'si tiene otro beneficio en la universidad',
        'si tiene alguna discapacidad',
      ]);
    });

    test('con lo obligatorio contestado es válido', () {
      expect(minimos().faltantes(), isEmpty);
      expect(minimos().esValido, isTrue);
    });

    test('si tiene hijos pide cuántos; si no tiene, no', () {
      final con = ValoresComplementarios(
        dependenciaEconomica: 'Ambos padres',
        ocupacionSosten: 'Rentista',
        rangoIngresos: 'Menor de Bs. 2500',
        tieneHijos: true,
        tenenciaVivienda: 'Herencia',
        otroBeneficio: false,
        discapacidad: false,
      );

      expect(con.faltantes(), ['cuántos hijos tiene']);
      expect(minimos().aDatos().cantidadHijos, isNull);
    });

    test('el grupo familiar admite hasta 10 y descarta las filas vacías', () {
      ValoresComplementarios conFamiliares(int n) => ValoresComplementarios(
            grupoFamiliar: [for (var i = 0; i < n; i++) const ValoresFamiliar(nombre: 'Familiar')],
          );

      expect(conFamiliares(10).errores(), isEmpty);
      expect(conFamiliares(11).errores().single, contains('hasta 10'));

      final datos = const ValoresComplementarios(
        grupoFamiliar: [ValoresFamiliar(nombre: ' María ', edad: '45'), ValoresFamiliar()],
      ).aDatos();
      expect(datos.grupoFamiliar, hasLength(1));
      expect(datos.grupoFamiliar!.single.nombre, 'María');
      expect(datos.grupoFamiliar!.single.edad, 45);
    });

    test('señala la edad, los ambientes y los textos fuera de rango', () {
      final e = ValoresComplementarios(
        grupoFamiliar: const [ValoresFamiliar(edad: '121')],
        dormitorios: '21',
        patio: 'mucho',
        comentario: 'x' * 501,
      ).errores();

      expect(e, hasLength(4));
      expect(e.join(' '), allOf(contains('edad'), contains('dormitorios'), contains('patio'), contains('comentario')));
    });

    test('el detalle solo se guarda si la respuesta fue sí', () {
      final datos = const ValoresComplementarios(
        otroBeneficio: true,
        otroBeneficioDetalle: ' Beca de trabajo ',
        discapacidad: false,
        discapacidadDetalle: 'se ignora',
      ).aDatos();

      expect(datos.otroBeneficioDetalle, 'Beca de trabajo');
      expect(datos.discapacidadDetalle, isNull);
    });

    test('lo guardado vuelve al formulario y, al enviarlo, da los mismos datos', () {
      final guardado = DatosComplementarios.fromJson({
        ...complementariosMinimosJson(),
        'tieneHijos': true,
        'cantidadHijos': 'Más de 1',
        'grupoFamiliar': [
          {'nombre': 'María', 'parentesco': 'Madre', 'edad': 45, 'ocupacion': null, 'observacion': null},
        ],
      });

      expect(ValoresComplementarios.desde(guardado).aDatos().toJson(), guardado.toJson());
    });
  });

  group('mapearErrorEnvio', () {
    test('ERR_ENV_01 es un aviso de que no hay convocatoria abierta, con el mensaje del backend', () {
      final e = mapearErrorEnvio(_apiError('ERR_ENV_01', 'No hay una convocatoria abierta para postular.'));

      expect(e.sinConvocatoria, isTrue);
      expect(e.mensaje, contains('No hay una convocatoria abierta'));
    });

    test('ERR_VAL_01 y ERR_VAL_02 indican que no puede postular', () {
      expect(mapearErrorEnvio(_apiError('ERR_VAL_01', 'Ya tienes una postulación vigente')).noPuedePostular, isTrue);
      expect(mapearErrorEnvio(_apiError('ERR_VAL_02', 'Tu cuenta está desactivada')).noPuedePostular, isTrue);
    });

    test('ERR_VAL_03 y ERR_ENV_03 indican que el plazo finalizó o ya se envió', () {
      expect(mapearErrorEnvio(_apiError('ERR_VAL_03', 'x')).cerrada, isTrue);
      expect(mapearErrorEnvio(_apiError('ERR_ENV_03', 'x')).cerrada, isTrue);
    });

    test('ERR_ENV_04 y ERR_ENV_05 traen lo que falta tal cual lo explica el backend', () {
      expect(mapearErrorEnvio(_apiError('ERR_ENV_04', 'Falta completar documentos: Carnet')).mensaje,
          'Falta completar documentos: Carnet');
      expect(mapearErrorEnvio(_apiError('ERR_ENV_05', 'Revisa el formulario: x')).mensaje, contains('Revisa'));
    });

    test('sin respuesta del servidor permite reintentar', () {
      final e = mapearErrorEnvio(DioException(requestOptions: RequestOptions(path: '/postulaciones')));

      expect(e.red, isTrue);
    });

    test('un error desconocido da el mensaje genérico', () {
      expect(mapearErrorEnvio(Exception('x')).mensaje, 'Ocurrió un error inesperado. Inténtalo de nuevo.');
    });
  });
}
