// Contrato del backend de CU-17 (`BorradorGuardadoResponse`, `BorradorResumenResponse`). Solo códigos públicos.

import 'datos_complementarios.dart';

/// El cuerpo de `PUT /postulaciones/{codigo}/borrador`: el formulario tal como está. Todo puede ir en `null`: un campo
/// omitido se guarda vacío (es `PUT`, la representación completa).
class GuardarBorradorRequest {
  final num? ingresosFamiliares;
  final int? dependientes;
  final String? procedencia;
  final String? patrimonio;
  final String? estadoVulnerabilidad;
  final num? promedioAcademico;
  final DatosComplementarios? complementarios;

  const GuardarBorradorRequest({
    this.ingresosFamiliares,
    this.dependientes,
    this.procedencia,
    this.patrimonio,
    this.estadoVulnerabilidad,
    this.promedioAcademico,
    this.complementarios,
  });

  Map<String, dynamic> toJson() => {
    'ingresosFamiliares': ingresosFamiliares,
    'dependientes': dependientes,
    'procedencia': procedencia,
    'patrimonio': patrimonio,
    'estadoVulnerabilidad': estadoVulnerabilidad,
    'promedioAcademico': promedioAcademico,
    'complementarios': complementarios?.toJson(),
  };
}

/// La confirmación de «Guardar borrador».
class BorradorGuardado {
  final String postulacion;
  final DateTime guardadoEn;
  final int camposPendientes;

  const BorradorGuardado({
    required this.postulacion,
    required this.guardadoEn,
    required this.camposPendientes,
  });

  factory BorradorGuardado.fromJson(Map<String, dynamic> json) =>
      BorradorGuardado(
        postulacion: json['postulacion'] as String,
        guardadoEn: DateTime.parse(json['guardadoEn'] as String),
        camposPendientes: (json['camposPendientes'] as num).toInt(),
      );
}

/// «Mi borrador»: el borrador de la convocatoria abierta y lo que le falta para poder enviarlo.
class BorradorResumen {
  final String postulacion;
  final String convocatoria;
  final String gestion;
  final DateTime fechaCierre;

  /// `null` si todavía no se guardó ningún avance del formulario.
  final DateTime? guardadoEn;
  final int camposPendientes;
  final int documentosPendientes;

  const BorradorResumen({
    required this.postulacion,
    required this.convocatoria,
    required this.gestion,
    required this.fechaCierre,
    required this.guardadoEn,
    required this.camposPendientes,
    required this.documentosPendientes,
  });

  factory BorradorResumen.fromJson(Map<String, dynamic> json) =>
      BorradorResumen(
        postulacion: json['postulacion'] as String,
        convocatoria: json['convocatoria'] as String,
        gestion: json['gestion'] as String,
        fechaCierre: DateTime.parse(json['fechaCierre'] as String),
        guardadoEn: json['guardadoEn'] == null
            ? null
            : DateTime.parse(json['guardadoEn'] as String),
        camposPendientes: (json['camposPendientes'] as num).toInt(),
        documentosPendientes: (json['documentosPendientes'] as num).toInt(),
      );

  /// «Faltan 3 campos y 1 documento.», o «No falta nada: ya puedes enviarlo.».
  String get resumenAvance {
    final partes = <String>[
      if (camposPendientes > 0)
        '$camposPendientes ${camposPendientes == 1 ? "campo" : "campos"}',
      if (documentosPendientes > 0)
        '$documentosPendientes ${documentosPendientes == 1 ? "documento" : "documentos"}',
    ];
    return partes.isEmpty
        ? 'No falta nada: ya puedes enviarlo.'
        : 'Faltan ${partes.join(" y ")}.';
  }
}
