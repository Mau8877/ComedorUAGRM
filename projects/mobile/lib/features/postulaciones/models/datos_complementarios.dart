// Contrato del backend (`DatosComplementarios`): los datos del formulario socioeconómico oficial de la DUBSS
// (FORM-DUBS-001) que no tienen columna propia y que el backend guarda aparte (`datos_extra`). Todo puede ser nulo.

/// Una fila de la tabla «Grupo familiar» del formulario oficial (hasta 10).
class MiembroGrupoFamiliar {
  final String? nombre;
  final String? parentesco;
  final int? edad;
  final String? ocupacion;
  final String? observacion;

  const MiembroGrupoFamiliar({
    this.nombre,
    this.parentesco,
    this.edad,
    this.ocupacion,
    this.observacion,
  });

  factory MiembroGrupoFamiliar.fromJson(Map<String, dynamic> json) =>
      MiembroGrupoFamiliar(
        nombre: json['nombre'] as String?,
        parentesco: json['parentesco'] as String?,
        edad: (json['edad'] as num?)?.toInt(),
        ocupacion: json['ocupacion'] as String?,
        observacion: json['observacion'] as String?,
      );

  Map<String, dynamic> toJson() => {
    'nombre': nombre,
    'parentesco': parentesco,
    'edad': edad,
    'ocupacion': ocupacion,
    'observacion': observacion,
  };
}

/// «5. De la residencia»: dónde vive el postulante.
class ResidenciaDatos {
  final String? provincia;
  final String? zonaAnillo;
  final String? barrio;
  final String? calle;

  const ResidenciaDatos({
    this.provincia,
    this.zonaAnillo,
    this.barrio,
    this.calle,
  });

  factory ResidenciaDatos.fromJson(Map<String, dynamic> json) =>
      ResidenciaDatos(
        provincia: json['provincia'] as String?,
        zonaAnillo: json['zonaAnillo'] as String?,
        barrio: json['barrio'] as String?,
        calle: json['calle'] as String?,
      );

  Map<String, dynamic> toJson() => {
    'provincia': provincia,
    'zonaAnillo': zonaAnillo,
    'barrio': barrio,
    'calle': calle,
  };
}

/// «7. Infraestructura de la vivienda»: la cantidad de cada ambiente.
class InfraestructuraDatos {
  final int? dormitorios;
  final int? banos;
  final int? comedor;
  final int? sala;
  final int? patio;

  const InfraestructuraDatos({
    this.dormitorios,
    this.banos,
    this.comedor,
    this.sala,
    this.patio,
  });

  factory InfraestructuraDatos.fromJson(Map<String, dynamic> json) =>
      InfraestructuraDatos(
        dormitorios: (json['dormitorios'] as num?)?.toInt(),
        banos: (json['banos'] as num?)?.toInt(),
        comedor: (json['comedor'] as num?)?.toInt(),
        sala: (json['sala'] as num?)?.toInt(),
        patio: (json['patio'] as num?)?.toInt(),
      );

  Map<String, dynamic> toJson() => {
    'dormitorios': dormitorios,
    'banos': banos,
    'comedor': comedor,
    'sala': sala,
    'patio': patio,
  };
}

/// Los datos del formulario oficial de la DUBSS. Obligatorios al enviar: de quién depende, ocupación, rango de
/// ingresos, si tiene hijos (y cuántos), tenencia de la vivienda, otro beneficio y discapacidad.
class DatosComplementarios {
  final String? dependenciaEconomica;
  final String? ocupacionSosten;
  final String? rangoIngresos;
  final bool? tieneHijos;
  final String? cantidadHijos;
  final List<MiembroGrupoFamiliar>? grupoFamiliar;
  final ResidenciaDatos? residencia;
  final String? tenenciaVivienda;
  final InfraestructuraDatos? infraestructura;
  final bool? otroBeneficio;
  final String? otroBeneficioDetalle;
  final bool? discapacidad;
  final String? discapacidadDetalle;
  final String? comentario;

  const DatosComplementarios({
    this.dependenciaEconomica,
    this.ocupacionSosten,
    this.rangoIngresos,
    this.tieneHijos,
    this.cantidadHijos,
    this.grupoFamiliar,
    this.residencia,
    this.tenenciaVivienda,
    this.infraestructura,
    this.otroBeneficio,
    this.otroBeneficioDetalle,
    this.discapacidad,
    this.discapacidadDetalle,
    this.comentario,
  });

  factory DatosComplementarios.fromJson(Map<String, dynamic> json) =>
      DatosComplementarios(
        dependenciaEconomica: json['dependenciaEconomica'] as String?,
        ocupacionSosten: json['ocupacionSosten'] as String?,
        rangoIngresos: json['rangoIngresos'] as String?,
        tieneHijos: json['tieneHijos'] as bool?,
        cantidadHijos: json['cantidadHijos'] as String?,
        grupoFamiliar: json['grupoFamiliar'] == null
            ? null
            : [
                for (final m in json['grupoFamiliar'] as List<dynamic>)
                  MiembroGrupoFamiliar.fromJson(m as Map<String, dynamic>),
              ],
        residencia: json['residencia'] == null
            ? null
            : ResidenciaDatos.fromJson(
                json['residencia'] as Map<String, dynamic>,
              ),
        tenenciaVivienda: json['tenenciaVivienda'] as String?,
        infraestructura: json['infraestructura'] == null
            ? null
            : InfraestructuraDatos.fromJson(
                json['infraestructura'] as Map<String, dynamic>,
              ),
        otroBeneficio: json['otroBeneficio'] as bool?,
        otroBeneficioDetalle: json['otroBeneficioDetalle'] as String?,
        discapacidad: json['discapacidad'] as bool?,
        discapacidadDetalle: json['discapacidadDetalle'] as String?,
        comentario: json['comentario'] as String?,
      );

  Map<String, dynamic> toJson() => {
    'dependenciaEconomica': dependenciaEconomica,
    'ocupacionSosten': ocupacionSosten,
    'rangoIngresos': rangoIngresos,
    'tieneHijos': tieneHijos,
    'cantidadHijos': cantidadHijos,
    'grupoFamiliar': grupoFamiliar?.map((m) => m.toJson()).toList(),
    'residencia': residencia?.toJson(),
    'tenenciaVivienda': tenenciaVivienda,
    'infraestructura': infraestructura?.toJson(),
    'otroBeneficio': otroBeneficio,
    'otroBeneficioDetalle': otroBeneficioDetalle,
    'discapacidad': discapacidad,
    'discapacidadDetalle': discapacidadDetalle,
    'comentario': comentario,
  };
}
