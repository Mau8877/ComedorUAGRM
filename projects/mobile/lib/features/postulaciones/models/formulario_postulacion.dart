// Contrato del backend de CU-02 (`FormularioPostulacionResponse`, `ComprobantePostulacionResponse`). Solo códigos
// públicos: nunca ids internos.

import 'datos_complementarios.dart';

/// Los datos del perfil que el formulario muestra precargados.
class PerfilFormulario {
  final String nombres;
  final String apellidos;
  final String ci;
  final String carrera;

  const PerfilFormulario({
    required this.nombres,
    required this.apellidos,
    required this.ci,
    required this.carrera,
  });

  factory PerfilFormulario.fromJson(Map<String, dynamic> json) =>
      PerfilFormulario(
        nombres: json['nombres'] as String,
        apellidos: json['apellidos'] as String,
        ci: json['ci'] as String,
        carrera: json['carrera'] as String,
      );
}

/// La convocatoria a la que se postula.
class ConvocatoriaFormulario {
  final String codigo;
  final String nombre;
  final String gestion;
  final DateTime fechaCierre;

  const ConvocatoriaFormulario({
    required this.codigo,
    required this.nombre,
    required this.gestion,
    required this.fechaCierre,
  });

  factory ConvocatoriaFormulario.fromJson(Map<String, dynamic> json) =>
      ConvocatoriaFormulario(
        codigo: json['codigo'] as String,
        nombre: json['nombre'] as String,
        gestion: json['gestion'] as String,
        fechaCierre: DateTime.parse(json['fechaCierre'] as String),
      );
}

/// Las opciones de las listas desplegables: las define el backend (por definir con la DUBSS), la app no las repite.
class OpcionesFormulario {
  final List<String> procedencias;
  final List<String> patrimonios;
  final List<String> estadosVulnerabilidad;

  // Las del formulario oficial de la DUBSS (FORM-DUBS-001).
  final List<String> dependencias;
  final List<String> ocupacionesSosten;
  final List<String> rangosIngresos;
  final List<String> cantidadesHijos;
  final List<String> tenenciasVivienda;

  const OpcionesFormulario({
    required this.procedencias,
    required this.patrimonios,
    required this.estadosVulnerabilidad,
    this.dependencias = const [],
    this.ocupacionesSosten = const [],
    this.rangosIngresos = const [],
    this.cantidadesHijos = const [],
    this.tenenciasVivienda = const [],
  });

  factory OpcionesFormulario.fromJson(Map<String, dynamic> json) =>
      OpcionesFormulario(
        procedencias: [
          for (final o in json['procedencias'] as List<dynamic>) o as String,
        ],
        patrimonios: [
          for (final o in json['patrimonios'] as List<dynamic>) o as String,
        ],
        estadosVulnerabilidad: [
          for (final o in json['estadosVulnerabilidad'] as List<dynamic>)
            o as String,
        ],
        dependencias: _lista(json['dependencias']),
        ocupacionesSosten: _lista(json['ocupacionesSosten']),
        rangosIngresos: _lista(json['rangosIngresos']),
        cantidadesHijos: _lista(json['cantidadesHijos']),
        tenenciasVivienda: _lista(json['tenenciasVivienda']),
      );

  static List<String> _lista(dynamic valor) => [
    for (final o in (valor as List<dynamic>? ?? const [])) o as String,
  ];
}

/// Las respuestas ya guardadas (reenvío de una devuelta); cualquier campo puede ser nulo.
class RespuestaSocioeconomica {
  final num? ingresosFamiliares;
  final int? dependientes;
  final String? procedencia;
  final String? patrimonio;
  final String? estadoVulnerabilidad;
  final num? promedioAcademico;

  /// Cuándo se guardó por última vez (CU-17); `null` si todavía no se guardó nada.
  final DateTime? guardadoEn;

  /// Los datos del formulario oficial; `null` si todavía no se guardó ninguno.
  final DatosComplementarios? complementarios;

  const RespuestaSocioeconomica({
    this.ingresosFamiliares,
    this.dependientes,
    this.procedencia,
    this.patrimonio,
    this.estadoVulnerabilidad,
    this.promedioAcademico,
    this.guardadoEn,
    this.complementarios,
  });

  factory RespuestaSocioeconomica.fromJson(Map<String, dynamic> json) =>
      RespuestaSocioeconomica(
        ingresosFamiliares: json['ingresosFamiliares'] as num?,
        dependientes: (json['dependientes'] as num?)?.toInt(),
        procedencia: json['procedencia'] as String?,
        patrimonio: json['patrimonio'] as String?,
        estadoVulnerabilidad: json['estadoVulnerabilidad'] as String?,
        promedioAcademico: json['promedioAcademico'] as num?,
        guardadoEn: json['guardadoEn'] == null
            ? null
            : DateTime.parse(json['guardadoEn'] as String),
        complementarios: json['complementarios'] == null
            ? null
            : DatosComplementarios.fromJson(
                json['complementarios'] as Map<String, dynamic>,
              ),
      );
}

/// Lo que muestra «Nueva postulación»: la postulación en edición (borrador, o devuelta a corregir) y su contexto.
class FormularioPostulacion {
  final String postulacion;

  /// `borrador` o `devuelta`.
  final String estado;
  final int numeroEnvios;
  final ConvocatoriaFormulario convocatoria;
  final PerfilFormulario perfil;
  final OpcionesFormulario opciones;
  final RespuestaSocioeconomica? respuesta;

  const FormularioPostulacion({
    required this.postulacion,
    required this.estado,
    required this.numeroEnvios,
    required this.convocatoria,
    required this.perfil,
    required this.opciones,
    required this.respuesta,
  });

  bool get esReenvio => estado == 'devuelta';

  factory FormularioPostulacion.fromJson(Map<String, dynamic> json) =>
      FormularioPostulacion(
        postulacion: json['postulacion'] as String,
        estado: json['estado'] as String,
        numeroEnvios: (json['numeroEnvios'] as num).toInt(),
        convocatoria: ConvocatoriaFormulario.fromJson(
          json['convocatoria'] as Map<String, dynamic>,
        ),
        perfil: PerfilFormulario.fromJson(
          json['perfil'] as Map<String, dynamic>,
        ),
        opciones: OpcionesFormulario.fromJson(
          json['opciones'] as Map<String, dynamic>,
        ),
        respuesta: json['respuesta'] == null
            ? null
            : RespuestaSocioeconomica.fromJson(
                json['respuesta'] as Map<String, dynamic>,
              ),
      );
}

/// Las respuestas del formulario que se envían (ya validadas y convertidas a número).
class EnviarPostulacionRequest {
  final num ingresosFamiliares;
  final int dependientes;
  final String procedencia;
  final String patrimonio;
  final String estadoVulnerabilidad;
  final num promedioAcademico;
  final DatosComplementarios complementarios;

  const EnviarPostulacionRequest({
    required this.ingresosFamiliares,
    required this.dependientes,
    required this.procedencia,
    required this.patrimonio,
    required this.estadoVulnerabilidad,
    required this.promedioAcademico,
    this.complementarios = const DatosComplementarios(),
  });

  Map<String, dynamic> toJson() => {
    'ingresosFamiliares': ingresosFamiliares,
    'dependientes': dependientes,
    'procedencia': procedencia,
    'patrimonio': patrimonio,
    'estadoVulnerabilidad': estadoVulnerabilidad,
    'promedioAcademico': promedioAcademico,
    'complementarios': complementarios.toJson(),
  };
}

/// El comprobante del envío: `codigo` es el código público de la postulación.
class ComprobantePostulacion {
  final String codigo;
  final String estado;
  final DateTime fechaEnvio;
  final int numeroEnvios;
  final bool reenvio;
  final String convocatoria;
  final String gestion;

  const ComprobantePostulacion({
    required this.codigo,
    required this.estado,
    required this.fechaEnvio,
    required this.numeroEnvios,
    required this.reenvio,
    required this.convocatoria,
    required this.gestion,
  });

  factory ComprobantePostulacion.fromJson(Map<String, dynamic> json) =>
      ComprobantePostulacion(
        codigo: json['codigo'] as String,
        estado: json['estado'] as String,
        fechaEnvio: DateTime.parse(json['fechaEnvio'] as String),
        numeroEnvios: (json['numeroEnvios'] as num).toInt(),
        reenvio: json['reenvio'] as bool,
        convocatoria: json['convocatoria'] as String,
        gestion: json['gestion'] as String,
      );
}
