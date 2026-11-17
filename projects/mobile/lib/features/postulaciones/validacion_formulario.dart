import 'models/borrador_postulacion.dart';
import 'models/datos_complementarios.dart';
import 'models/formulario_postulacion.dart';

// Mismas reglas que el web (`respuestasSocioeconomicasSchema`) y que el backend (`EnvioPostulacionService`): el
// servidor vuelve a validar, esto es para dar feedback inmediato campo por campo. Cada función devuelve el mensaje de
// error o `null` si el valor es válido. Los topes están por definir con la DUBSS (hoy promedio 0 a 100).

const mensajeObligatorio = 'Este campo es obligatorio.';

final _ingresos = RegExp(r'^\d{1,10}([.,]\d{1,2})?$');
final _enteroCorto = RegExp(r'^\d{1,2}$');
final _promedio = RegExp(r'^(100([.,]0{1,2})?|\d{1,2}([.,]\d{1,2})?)$');

num _aNumero(String texto) => num.parse(texto.trim().replaceAll(',', '.'));

String? validarIngresos(String? valor) {
  final v = valor?.trim() ?? '';
  if (v.isEmpty) return mensajeObligatorio;
  if (!_ingresos.hasMatch(v)) {
    return 'Ingresa un monto válido, con hasta 2 decimales.';
  }
  return null;
}

String? validarDependientes(String? valor) {
  final v = valor?.trim() ?? '';
  if (v.isEmpty) return mensajeObligatorio;
  if (!_enteroCorto.hasMatch(v)) return 'Ingresa un número entero.';
  if (int.parse(v) > 50) {
    return 'El número de dependientes no puede superar 50.';
  }
  return null;
}

String? validarPromedio(String? valor) {
  final v = valor?.trim() ?? '';
  if (v.isEmpty) return mensajeObligatorio;
  if (!_promedio.hasMatch(v)) {
    return 'El promedio debe estar entre 0 y 100, con hasta 2 decimales.';
  }
  return null;
}

String? validarOpcion(String? valor) =>
    (valor == null || valor.isEmpty) ? 'Selecciona una opción.' : null;

// ---- Datos del formulario oficial de la DUBSS (FORM-DUBS-001) ----
// Mismos topes que el backend (`OpcionesSocioeconomicas`) y que el web. Qué es obligatorio está por definir con la DUBSS.

const grupoFamiliarMaximo = 10;
const _edadMaxima = 120;
const _ambientesMaximo = 20;
const _textoCortoMaximo = 100;
const _textoDetalleMaximo = 150;
const _comentarioMaximo = 500;

final _enteroTresCifras = RegExp(r'^\d{1,3}$');

String? _texto(String valor) {
  final t = valor.trim();
  return t.isEmpty ? null : t;
}

int? _entero(String valor) {
  final t = valor.trim();
  return t.isEmpty ? null : int.parse(t);
}

/// «Sí/no» del formulario: obligatorio al enviar.
String? validarSiNo(bool? valor) =>
    valor == null ? 'Selecciona una opción.' : null;

/// Texto libre opcional de hasta [maximo] caracteres.
String? validarTextoOpcional(String? valor, int maximo) =>
    (valor?.trim().length ?? 0) > maximo
    ? 'No puede superar $maximo caracteres.'
    : null;

/// Entero opcional (edad, ambientes) entre 0 y [maximo]; vacío es válido.
String? validarEnteroOpcional(String? valor, int maximo) {
  final t = valor?.trim() ?? '';
  if (t.isEmpty) return null;
  if (!_enteroTresCifras.hasMatch(t) || int.parse(t) > maximo) {
    return 'Debe ser un entero entre 0 y $maximo.';
  }
  return null;
}

/// Una fila de «Grupo familiar» como la escribe la persona (todo texto).
class ValoresFamiliar {
  final String nombre;
  final String parentesco;
  final String edad;
  final String ocupacion;
  final String observacion;

  const ValoresFamiliar({
    this.nombre = '',
    this.parentesco = '',
    this.edad = '',
    this.ocupacion = '',
    this.observacion = '',
  });
}

/// Los datos del formulario oficial tal como los escribe la persona.
class ValoresComplementarios {
  final String? dependenciaEconomica;
  final String? ocupacionSosten;
  final String? rangoIngresos;
  final bool? tieneHijos;
  final String? cantidadHijos;
  final List<ValoresFamiliar> grupoFamiliar;
  final String provincia;
  final String zonaAnillo;
  final String barrio;
  final String calle;
  final String? tenenciaVivienda;
  final String dormitorios;
  final String banos;
  final String comedor;
  final String sala;
  final String patio;
  final bool? otroBeneficio;
  final String otroBeneficioDetalle;
  final bool? discapacidad;
  final String discapacidadDetalle;
  final String comentario;

  const ValoresComplementarios({
    this.dependenciaEconomica,
    this.ocupacionSosten,
    this.rangoIngresos,
    this.tieneHijos,
    this.cantidadHijos,
    this.grupoFamiliar = const [],
    this.provincia = '',
    this.zonaAnillo = '',
    this.barrio = '',
    this.calle = '',
    this.tenenciaVivienda,
    this.dormitorios = '',
    this.banos = '',
    this.comedor = '',
    this.sala = '',
    this.patio = '',
    this.otroBeneficio,
    this.otroBeneficioDetalle = '',
    this.discapacidad,
    this.discapacidadDetalle = '',
    this.comentario = '',
  });

  /// Lo ya guardado como texto del formulario; sin datos, todo vacío.
  factory ValoresComplementarios.desde(DatosComplementarios? d) {
    String t(Object? v) => v == null ? '' : v.toString();
    return ValoresComplementarios(
      dependenciaEconomica: d?.dependenciaEconomica,
      ocupacionSosten: d?.ocupacionSosten,
      rangoIngresos: d?.rangoIngresos,
      tieneHijos: d?.tieneHijos,
      cantidadHijos: d?.cantidadHijos,
      grupoFamiliar: [
        for (final m in d?.grupoFamiliar ?? const <MiembroGrupoFamiliar>[])
          ValoresFamiliar(
            nombre: t(m.nombre),
            parentesco: t(m.parentesco),
            edad: t(m.edad),
            ocupacion: t(m.ocupacion),
            observacion: t(m.observacion),
          ),
      ],
      provincia: t(d?.residencia?.provincia),
      zonaAnillo: t(d?.residencia?.zonaAnillo),
      barrio: t(d?.residencia?.barrio),
      calle: t(d?.residencia?.calle),
      tenenciaVivienda: d?.tenenciaVivienda,
      dormitorios: t(d?.infraestructura?.dormitorios),
      banos: t(d?.infraestructura?.banos),
      comedor: t(d?.infraestructura?.comedor),
      sala: t(d?.infraestructura?.sala),
      patio: t(d?.infraestructura?.patio),
      otroBeneficio: d?.otroBeneficio,
      otroBeneficioDetalle: t(d?.otroBeneficioDetalle),
      discapacidad: d?.discapacidad,
      discapacidadDetalle: t(d?.discapacidadDetalle),
      comentario: t(d?.comentario),
    );
  }

  /// Lo obligatorio que falta, con el nombre que ve la persona.
  List<String> faltantes() => [
    if (_texto(dependenciaEconomica ?? '') == null) 'de quién depende',
    if (_texto(ocupacionSosten ?? '') == null) 'ocupación',
    if (_texto(rangoIngresos ?? '') == null) 'rango de ingresos',
    if (tieneHijos == null)
      'si tiene hijos'
    else if (tieneHijos! && _texto(cantidadHijos ?? '') == null)
      'cuántos hijos tiene',
    if (_texto(tenenciaVivienda ?? '') == null) 'tenencia de la vivienda',
    if (otroBeneficio == null) 'si tiene otro beneficio en la universidad',
    if (discapacidad == null) 'si tiene alguna discapacidad',
  ];

  /// Los valores escritos que no cumplen su regla («campo: motivo»); lo vacío no es un error.
  List<String> errores() {
    final e = <String>[];
    void revisar(String etiqueta, String? motivo) {
      if (motivo != null) e.add('$etiqueta: $motivo');
    }

    if (grupoFamiliar.length > grupoFamiliarMaximo) {
      e.add('grupo familiar: admite hasta $grupoFamiliarMaximo personas.');
    }
    for (var i = 0; i < grupoFamiliar.length; i++) {
      final f = grupoFamiliar[i];
      final n = i + 1;
      revisar(
        'familiar $n, nombre',
        validarTextoOpcional(f.nombre, _textoDetalleMaximo),
      );
      revisar(
        'familiar $n, parentesco',
        validarTextoOpcional(f.parentesco, _textoCortoMaximo),
      );
      revisar('familiar $n, edad', validarEnteroOpcional(f.edad, _edadMaxima));
      revisar(
        'familiar $n, ocupación',
        validarTextoOpcional(f.ocupacion, _textoCortoMaximo),
      );
      revisar(
        'familiar $n, observación',
        validarTextoOpcional(f.observacion, _textoDetalleMaximo),
      );
    }
    revisar('provincia', validarTextoOpcional(provincia, _textoCortoMaximo));
    revisar(
      'zona o anillo',
      validarTextoOpcional(zonaAnillo, _textoCortoMaximo),
    );
    revisar('barrio', validarTextoOpcional(barrio, _textoCortoMaximo));
    revisar('calle', validarTextoOpcional(calle, _textoCortoMaximo));
    revisar(
      'dormitorios',
      validarEnteroOpcional(dormitorios, _ambientesMaximo),
    );
    revisar('baños', validarEnteroOpcional(banos, _ambientesMaximo));
    revisar('comedor', validarEnteroOpcional(comedor, _ambientesMaximo));
    revisar('sala', validarEnteroOpcional(sala, _ambientesMaximo));
    revisar('patio', validarEnteroOpcional(patio, _ambientesMaximo));
    revisar(
      'otro beneficio',
      validarTextoOpcional(otroBeneficioDetalle, _textoDetalleMaximo),
    );
    revisar(
      'tipo y grado de discapacidad',
      validarTextoOpcional(discapacidadDetalle, _textoDetalleMaximo),
    );
    revisar('comentario', validarTextoOpcional(comentario, _comentarioMaximo));
    return e;
  }

  bool get esValido => faltantes().isEmpty && errores().isEmpty;

  /// Los datos con la forma del backend: lo vacío va en `null`, las filas vacías del grupo familiar se descartan y el
  /// detalle solo se guarda si la respuesta fue «sí». Solo válido si [errores] está vacío.
  DatosComplementarios aDatos() {
    final familiares =
        [
              for (final f in grupoFamiliar)
                MiembroGrupoFamiliar(
                  nombre: _texto(f.nombre),
                  parentesco: _texto(f.parentesco),
                  edad: _entero(f.edad),
                  ocupacion: _texto(f.ocupacion),
                  observacion: _texto(f.observacion),
                ),
            ]
            .where(
              (m) =>
                  m.nombre != null ||
                  m.parentesco != null ||
                  m.edad != null ||
                  m.ocupacion != null ||
                  m.observacion != null,
            )
            .toList();
    return DatosComplementarios(
      dependenciaEconomica: _texto(dependenciaEconomica ?? ''),
      ocupacionSosten: _texto(ocupacionSosten ?? ''),
      rangoIngresos: _texto(rangoIngresos ?? ''),
      tieneHijos: tieneHijos,
      cantidadHijos: tieneHijos == false ? null : _texto(cantidadHijos ?? ''),
      grupoFamiliar: familiares.isEmpty ? null : familiares,
      residencia: ResidenciaDatos(
        provincia: _texto(provincia),
        zonaAnillo: _texto(zonaAnillo),
        barrio: _texto(barrio),
        calle: _texto(calle),
      ),
      tenenciaVivienda: _texto(tenenciaVivienda ?? ''),
      infraestructura: InfraestructuraDatos(
        dormitorios: _entero(dormitorios),
        banos: _entero(banos),
        comedor: _entero(comedor),
        sala: _entero(sala),
        patio: _entero(patio),
      ),
      otroBeneficio: otroBeneficio,
      otroBeneficioDetalle: otroBeneficio == true
          ? _texto(otroBeneficioDetalle)
          : null,
      discapacidad: discapacidad,
      discapacidadDetalle: discapacidad == true
          ? _texto(discapacidadDetalle)
          : null,
      comentario: _texto(comentario),
    );
  }
}

/// Los valores del formulario tal como los escribe la persona (todo texto).
class ValoresFormulario {
  final String ingresosFamiliares;
  final String dependientes;
  final String? procedencia;
  final String? patrimonio;
  final String? estadoVulnerabilidad;
  final String promedioAcademico;

  /// Los datos del formulario oficial de la DUBSS.
  final ValoresComplementarios complementarios;

  const ValoresFormulario({
    this.ingresosFamiliares = '',
    this.dependientes = '',
    this.procedencia,
    this.patrimonio,
    this.estadoVulnerabilidad,
    this.promedioAcademico = '',
    this.complementarios = const ValoresComplementarios(),
  });

  /// Las respuestas ya guardadas (reenvío de una devuelta) como texto del formulario; sin respuestas, todo vacío.
  factory ValoresFormulario.desde(RespuestaSocioeconomica? r) {
    String texto(num? n) => n == null
        ? ''
        : (n == n.truncate() ? n.truncate().toString() : n.toString());
    return ValoresFormulario(
      ingresosFamiliares: texto(r?.ingresosFamiliares),
      dependientes: texto(r?.dependientes),
      procedencia: r?.procedencia,
      patrimonio: r?.patrimonio,
      estadoVulnerabilidad: r?.estadoVulnerabilidad,
      promedioAcademico: texto(r?.promedioAcademico),
      complementarios: ValoresComplementarios.desde(r?.complementarios),
    );
  }

  /// `true` si todos los campos son válidos.
  bool get esValido =>
      validarIngresos(ingresosFamiliares) == null &&
      validarDependientes(dependientes) == null &&
      validarOpcion(procedencia) == null &&
      validarOpcion(patrimonio) == null &&
      validarOpcion(estadoVulnerabilidad) == null &&
      validarPromedio(promedioAcademico) == null &&
      complementarios.esValido;

  /// El cuerpo que se envía; solo válido si [esValido].
  EnviarPostulacionRequest aRequest() => EnviarPostulacionRequest(
    ingresosFamiliares: _aNumero(ingresosFamiliares),
    dependientes: int.parse(dependientes.trim()),
    procedencia: procedencia!,
    patrimonio: patrimonio!,
    estadoVulnerabilidad: estadoVulnerabilidad!,
    promedioAcademico: _aNumero(promedioAcademico),
    complementarios: complementarios.aDatos(),
  );

  /// Convierte lo que hay en el formulario en el cuerpo del borrador (CU-17): un campo vacío se guarda vacío (`null`,
  /// no es un error) y uno con texto debe cumplir la misma regla que al enviar. Si alguno no la cumple no se guarda
  /// nada (el backend rechaza igual con `ERR_ENV_05`).
  ResultadoBorrador aBorrador() {
    bool vacio(String? t) => t == null || t.trim().isEmpty;
    final errores = <String>[];
    void revisar(
      String etiqueta,
      String valor,
      String? Function(String?) regla,
    ) {
      if (vacio(valor)) return;
      final motivo = regla(valor);
      if (motivo != null) errores.add('$etiqueta: $motivo');
    }

    revisar('ingresos familiares', ingresosFamiliares, validarIngresos);
    revisar('dependientes', dependientes, validarDependientes);
    revisar('promedio académico', promedioAcademico, validarPromedio);
    errores.addAll(complementarios.errores());
    if (errores.isNotEmpty) return ResultadoBorrador.conErrores(errores);

    String? opcion(String? t) => vacio(t) ? null : t;
    return ResultadoBorrador.ok(
      GuardarBorradorRequest(
        ingresosFamiliares: vacio(ingresosFamiliares)
            ? null
            : _aNumero(ingresosFamiliares),
        dependientes: vacio(dependientes)
            ? null
            : int.parse(dependientes.trim()),
        procedencia: opcion(procedencia),
        patrimonio: opcion(patrimonio),
        estadoVulnerabilidad: opcion(estadoVulnerabilidad),
        promedioAcademico: vacio(promedioAcademico)
            ? null
            : _aNumero(promedioAcademico),
        complementarios: complementarios.aDatos(),
      ),
    );
  }
}

/// Lo que [ValoresFormulario.aBorrador] decide: el cuerpo del borrador, o los datos mal escritos (nunca los dos).
class ResultadoBorrador {
  final GuardarBorradorRequest? borrador;

  /// «campo: motivo» por cada valor escrito que no cumple su regla.
  final List<String> errores;

  const ResultadoBorrador.ok(GuardarBorradorRequest this.borrador)
    : errores = const [];
  const ResultadoBorrador.conErrores(this.errores) : borrador = null;

  bool get esValido => borrador != null;
}
