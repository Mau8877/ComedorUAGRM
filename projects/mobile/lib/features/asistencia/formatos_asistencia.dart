/// La hora de La Paz (UTC-4, sin horario de verano): la app no trae una base de zonas horarias.
DateTime _laPaz(DateTime instante) => instante.toUtc().subtract(const Duration(hours: 4));

String _dos(int n) => n.toString().padLeft(2, '0');

/// «16/11/2026»: la fecha de hoy en la hora de La Paz.
String formatearFechaAsistencia(DateTime instante) {
  final l = _laPaz(instante);
  return '${_dos(l.day)}/${_dos(l.month)}/${l.year}';
}

/// «14:35»: la hora de un registro en la hora de La Paz.
String formatearHoraAsistencia(DateTime instante) {
  final l = _laPaz(instante);
  return '${_dos(l.hour)}:${_dos(l.minute)}';
}
