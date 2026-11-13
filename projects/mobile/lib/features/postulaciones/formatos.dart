/// La hora de La Paz (UTC-4, sin horario de verano): la app no trae una base de zonas horarias.
DateTime _laPaz(DateTime instante) => instante.toUtc().subtract(const Duration(hours: 4));

String _dos(int n) => n.toString().padLeft(2, '0');

/// «13/11/2026»: solo la fecha, en la hora de La Paz (la lista, la fecha de envío).
String formatearFecha(DateTime instante) {
  final l = _laPaz(instante);
  return '${_dos(l.day)}/${_dos(l.month)}/${l.year}';
}

/// «13/11/2026, 11:30»: fecha y hora en la hora de La Paz (los pasos de la línea de tiempo, las observaciones).
String formatearFechaHora(DateTime instante) {
  final l = _laPaz(instante);
  return '${formatearFecha(instante)}, ${_dos(l.hour)}:${_dos(l.minute)}';
}

/// «20/11/2026»: un día sin hora (el plazo de entrega), sin correrlo por la zona horaria.
String formatearDia(DateTime dia) => '${_dos(dia.day)}/${_dos(dia.month)}/${dia.year}';
