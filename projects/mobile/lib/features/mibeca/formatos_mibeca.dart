/// La hora de La Paz (UTC-4, sin horario de verano): la app no trae una base de zonas horarias.
DateTime _laPaz(DateTime instante) => instante.toUtc().subtract(const Duration(hours: 4));

String _dos(int n) => n.toString().padLeft(2, '0');

/// «18/09/2026» de un día de calendario (`AAAA-MM-DD` ya parseado: no se convierte de zona).
String formatearDiaBeca(DateTime dia) => '${_dos(dia.day)}/${_dos(dia.month)}/${dia.year}';

/// «28/08/2026» de un instante, en la hora de La Paz.
String formatearFechaInstanteBeca(DateTime instante) => formatearDiaBeca(_laPaz(instante));

/// «12:30» de un instante, en la hora de La Paz.
String formatearHoraBeca(DateTime instante) {
  final l = _laPaz(instante);
  return '${_dos(l.hour)}:${_dos(l.minute)}';
}

/// `AAAA-MM-DD` de un día, el formato de `filter[desde]` y `filter[hasta]`.
String diaParaApi(DateTime dia) => '${dia.year}-${_dos(dia.month)}-${_dos(dia.day)}';

/// Etiqueta legible del estado de la beca (uno desconocido se muestra tal cual).
String etiquetaEstadoBeca(String estado) {
  switch (estado) {
    case 'VIGENTE':
      return 'Vigente';
    case 'SUSPENDIDA':
      return 'Suspendida';
    case 'BAJA':
      return 'Dada de baja';
    default:
      return estado;
  }
}
