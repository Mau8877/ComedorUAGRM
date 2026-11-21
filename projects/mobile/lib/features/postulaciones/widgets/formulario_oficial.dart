import 'package:flutter/material.dart';

import '../../../shared/widgets/campo_formulario.dart';
import '../models/formulario_postulacion.dart';
import '../validacion_formulario.dart';

/// Los campos de una fila de «Grupo familiar».
class FilaFamiliarController {
  final nombre = TextEditingController();
  final parentesco = TextEditingController();
  final edad = TextEditingController();
  final ocupacion = TextEditingController();
  final observacion = TextEditingController();

  FilaFamiliarController([ValoresFamiliar? v]) {
    if (v == null) return;
    nombre.text = v.nombre;
    parentesco.text = v.parentesco;
    edad.text = v.edad;
    ocupacion.text = v.ocupacion;
    observacion.text = v.observacion;
  }

  ValoresFamiliar get valores => ValoresFamiliar(
    nombre: nombre.text,
    parentesco: parentesco.text,
    edad: edad.text,
    ocupacion: ocupacion.text,
    observacion: observacion.text,
  );

  void dispose() {
    nombre.dispose();
    parentesco.dispose();
    edad.dispose();
    ocupacion.dispose();
    observacion.dispose();
  }
}

/// El estado del formulario oficial de la DUBSS (FORM-DUBS-001): lo que la persona eligió y escribió. La pantalla lo
/// crea, lo [cargar] con lo ya guardado y lee [valores] para validar, guardar el borrador o enviar.
class FormularioOficialController extends ChangeNotifier {
  String? dependenciaEconomica;
  String? ocupacionSosten;
  String? rangoIngresos;
  String? tenenciaVivienda;
  String? cantidadHijos;
  bool? tieneHijos;
  bool? otroBeneficio;
  bool? discapacidad;

  final provincia = TextEditingController();
  final zonaAnillo = TextEditingController();
  final barrio = TextEditingController();
  final calle = TextEditingController();
  final dormitorios = TextEditingController();
  final banos = TextEditingController();
  final comedor = TextEditingController();
  final sala = TextEditingController();
  final patio = TextEditingController();
  final otroBeneficioDetalle = TextEditingController();
  final discapacidadDetalle = TextEditingController();
  final comentario = TextEditingController();
  final List<FilaFamiliarController> filas = [];

  /// Reemplaza lo que hay por [v] (lo ya guardado de una postulación).
  void cargar(ValoresComplementarios v) {
    for (final f in filas) {
      f.dispose();
    }
    filas
      ..clear()
      ..addAll(v.grupoFamiliar.map(FilaFamiliarController.new));
    dependenciaEconomica = v.dependenciaEconomica;
    ocupacionSosten = v.ocupacionSosten;
    rangoIngresos = v.rangoIngresos;
    tenenciaVivienda = v.tenenciaVivienda;
    cantidadHijos = v.cantidadHijos;
    tieneHijos = v.tieneHijos;
    otroBeneficio = v.otroBeneficio;
    discapacidad = v.discapacidad;
    provincia.text = v.provincia;
    zonaAnillo.text = v.zonaAnillo;
    barrio.text = v.barrio;
    calle.text = v.calle;
    dormitorios.text = v.dormitorios;
    banos.text = v.banos;
    comedor.text = v.comedor;
    sala.text = v.sala;
    patio.text = v.patio;
    otroBeneficioDetalle.text = v.otroBeneficioDetalle;
    discapacidadDetalle.text = v.discapacidadDetalle;
    comentario.text = v.comentario;
    notifyListeners();
  }

  ValoresComplementarios get valores => ValoresComplementarios(
    dependenciaEconomica: dependenciaEconomica,
    ocupacionSosten: ocupacionSosten,
    rangoIngresos: rangoIngresos,
    tieneHijos: tieneHijos,
    cantidadHijos: cantidadHijos,
    grupoFamiliar: [for (final f in filas) f.valores],
    provincia: provincia.text,
    zonaAnillo: zonaAnillo.text,
    barrio: barrio.text,
    calle: calle.text,
    tenenciaVivienda: tenenciaVivienda,
    dormitorios: dormitorios.text,
    banos: banos.text,
    comedor: comedor.text,
    sala: sala.text,
    patio: patio.text,
    otroBeneficio: otroBeneficio,
    otroBeneficioDetalle: otroBeneficioDetalle.text,
    discapacidad: discapacidad,
    discapacidadDetalle: discapacidadDetalle.text,
    comentario: comentario.text,
  );

  /// Aplica un cambio de una opción elegida y avisa para repintar.
  void cambiar(void Function() cambio) {
    cambio();
    notifyListeners();
  }

  void agregarFamiliar() {
    if (filas.length >= grupoFamiliarMaximo) return;
    filas.add(FilaFamiliarController());
    notifyListeners();
  }

  void quitarFamiliar(int indice) {
    filas.removeAt(indice).dispose();
    notifyListeners();
  }

  @override
  void dispose() {
    for (final f in filas) {
      f.dispose();
    }
    for (final c in [
      provincia,
      zonaAnillo,
      barrio,
      calle,
      dormitorios,
      banos,
      comedor,
      sala,
      patio,
      otroBeneficioDetalle,
      discapacidadDetalle,
      comentario,
    ]) {
      c.dispose();
    }
    super.dispose();
  }
}

/// Las secciones del formulario oficial: dependencia económica, grupo familiar, residencia y vivienda, y otros datos.
/// Va dentro del `Form` de la pantalla, así sus validadores corren con los del resto.
class FormularioOficial extends StatelessWidget {
  final FormularioOficialController controlador;
  final OpcionesFormulario opciones;

  const FormularioOficial({
    super.key,
    required this.controlador,
    required this.opciones,
  });

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: controlador,
      builder: (context, _) {
        final c = controlador;
        return Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _Seccion('Dependencia económica', [
              _lista(
                '¿De quién depende usted? *',
                c.dependenciaEconomica,
                opciones.dependencias,
                (v) => c.cambiar(() => c.dependenciaEconomica = v),
              ),
              _lista(
                'Ocupación de quien lo sostiene *',
                c.ocupacionSosten,
                opciones.ocupacionesSosten,
                (v) => c.cambiar(() => c.ocupacionSosten = v),
              ),
              _lista(
                'Rango de ingresos *',
                c.rangoIngresos,
                opciones.rangosIngresos,
                (v) => c.cambiar(() => c.rangoIngresos = v),
              ),
            ]),
            _Seccion('Grupo familiar', [
              _siNo(
                '¿Usted tiene hijos? *',
                c.tieneHijos,
                (v) => c.cambiar(() => c.tieneHijos = v),
              ),
              if (c.tieneHijos == true)
                _lista(
                  '¿Cuántos? *',
                  c.cantidadHijos,
                  opciones.cantidadesHijos,
                  (v) => c.cambiar(() => c.cantidadHijos = v),
                ),
              Text(
                'Integrantes del grupo familiar (opcional)',
                style: Theme.of(context).textTheme.titleSmall,
              ),
              for (var i = 0; i < c.filas.length; i++)
                _FilaFamiliar(indice: i, fila: c.filas[i], controlador: c),
              Row(
                children: [
                  OutlinedButton(
                    onPressed: c.filas.length >= grupoFamiliarMaximo
                        ? null
                        : c.agregarFamiliar,
                    child: const Text('Agregar familiar'),
                  ),
                  const SizedBox(width: 12),
                  Text('${c.filas.length} de $grupoFamiliarMaximo'),
                ],
              ),
            ]),
            _Seccion('Residencia y vivienda', [
              _texto(
                'Provincia',
                c.provincia,
                (v) => validarTextoOpcional(v, 100),
              ),
              _texto(
                'Zona o anillo',
                c.zonaAnillo,
                (v) => validarTextoOpcional(v, 100),
              ),
              _texto('Barrio', c.barrio, (v) => validarTextoOpcional(v, 100)),
              _texto('Calle', c.calle, (v) => validarTextoOpcional(v, 100)),
              _lista(
                'Tenencia de la vivienda *',
                c.tenenciaVivienda,
                opciones.tenenciasVivienda,
                (v) => c.cambiar(() => c.tenenciaVivienda = v),
              ),
              _numero('Dormitorios', c.dormitorios, 20),
              _numero('Baños', c.banos, 20),
              _numero('Comedor', c.comedor, 20),
              _numero('Sala', c.sala, 20),
              _numero('Patio', c.patio, 20),
            ]),
            _Seccion('Otros datos', [
              _siNo(
                '¿Tiene otro beneficio dentro de la universidad? *',
                c.otroBeneficio,
                (v) => c.cambiar(() => c.otroBeneficio = v),
              ),
              if (c.otroBeneficio == true)
                _texto(
                  '¿Cuál?',
                  c.otroBeneficioDetalle,
                  (v) => validarTextoOpcional(v, 150),
                ),
              _siNo(
                '¿Tiene algún tipo de discapacidad? *',
                c.discapacidad,
                (v) => c.cambiar(() => c.discapacidad = v),
              ),
              if (c.discapacidad == true)
                _texto(
                  'Tipo y grado',
                  c.discapacidadDetalle,
                  (v) => validarTextoOpcional(v, 150),
                ),
              _texto(
                'Aclaración o comentario personal',
                c.comentario,
                (v) => validarTextoOpcional(v, 500),
                lineas: 3,
              ),
            ]),
          ],
        );
      },
    );
  }

  static Widget _lista(
    String etiqueta,
    String? valor,
    List<String> opciones,
    void Function(String?) alCambiar,
  ) {
    return DropdownButtonFormField<String>(
      initialValue: opciones.contains(valor) ? valor : null,
      isExpanded: true,
      decoration: InputDecoration(labelText: etiqueta),
      items: [
        for (final o in opciones) DropdownMenuItem(value: o, child: Text(o)),
      ],
      validator: validarOpcion,
      autovalidateMode: AutovalidateMode.onUserInteraction,
      onChanged: alCambiar,
    );
  }

  static Widget _siNo(
    String etiqueta,
    bool? valor,
    void Function(bool?) alCambiar,
  ) {
    return DropdownButtonFormField<bool>(
      initialValue: valor,
      isExpanded: true,
      decoration: InputDecoration(labelText: etiqueta),
      items: const [
        DropdownMenuItem(value: true, child: Text('Sí')),
        DropdownMenuItem(value: false, child: Text('No')),
      ],
      validator: validarSiNo,
      autovalidateMode: AutovalidateMode.onUserInteraction,
      onChanged: alCambiar,
    );
  }

  static Widget _texto(
    String etiqueta,
    TextEditingController c,
    String? Function(String?) validar, {
    int lineas = 1,
  }) {
    return CampoFormulario(
      controller: c,
      maxLines: lineas,
      decoration: InputDecoration(labelText: etiqueta),
      validator: validar,
    );
  }

  static Widget _numero(String etiqueta, TextEditingController c, int maximo) {
    return CampoFormulario(
      controller: c,
      keyboardType: TextInputType.number,
      decoration: InputDecoration(labelText: etiqueta),
      validator: (v) => validarEnteroOpcional(v, maximo),
    );
  }
}

class _Seccion extends StatelessWidget {
  final String titulo;
  final List<Widget> hijos;

  const _Seccion(this.titulo, this.hijos);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(titulo, style: Theme.of(context).textTheme.titleMedium),
          for (final h in hijos)
            Padding(padding: const EdgeInsets.only(top: 12), child: h),
        ],
      ),
    );
  }
}

class _FilaFamiliar extends StatelessWidget {
  final int indice;
  final FilaFamiliarController fila;
  final FormularioOficialController controlador;

  const _FilaFamiliar({
    required this.indice,
    required this.fila,
    required this.controlador,
  });

  @override
  Widget build(BuildContext context) {
    final n = indice + 1;
    return Semantics(
      container: true,
      label: 'Familiar $n',
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                'Familiar $n',
                style: Theme.of(context).textTheme.titleSmall,
              ),
              CampoFormulario(
                controller: fila.nombre,
                decoration: const InputDecoration(labelText: 'Nombre completo'),
                validator: (v) => validarTextoOpcional(v, 150),
              ),
              CampoFormulario(
                controller: fila.parentesco,
                decoration: const InputDecoration(labelText: 'Parentesco'),
                validator: (v) => validarTextoOpcional(v, 100),
              ),
              CampoFormulario(
                controller: fila.edad,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: 'Edad'),
                validator: (v) => validarEnteroOpcional(v, 120),
              ),
              CampoFormulario(
                controller: fila.ocupacion,
                decoration: const InputDecoration(labelText: 'Ocupación'),
                validator: (v) => validarTextoOpcional(v, 100),
              ),
              CampoFormulario(
                controller: fila.observacion,
                decoration: const InputDecoration(labelText: 'Observación'),
                validator: (v) => validarTextoOpcional(v, 150),
              ),
              Align(
                alignment: Alignment.centerRight,
                child: TextButton(
                  onPressed: () => controlador.quitarFamiliar(indice),
                  child: Text('Quitar familiar $n'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
