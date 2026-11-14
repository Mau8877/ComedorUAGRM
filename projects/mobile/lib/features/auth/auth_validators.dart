/// `validator` de los formularios de CU01, en español. Misma regla de negocio
/// que la web (schemas de Zod) y el backend (`RegistroRequest`); el backend
/// sigue siendo la fuente de verdad (ver FORMULARIOS_MOBILE.md).
/// Cada función devuelve el mensaje de error, o `null` si el valor es válido.
class AuthValidators {
  const AuthValidators._();

  static final _correo = RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$');
  static final _registro = RegExp(r'^[1-9]\d{8}$');

  static String? obligatorio(String? valor, String mensaje) =>
      (valor == null || valor.trim().isEmpty) ? mensaje : null;

  static String? texto(String? valor, {required String obligatorioMsg, required int max, required String largoMsg}) {
    final falta = obligatorio(valor, obligatorioMsg);
    if (falta != null) return falta;
    return valor!.trim().length > max ? largoMsg : null;
  }

  static String? nombres(String? v) => texto(v,
      obligatorioMsg: 'Los nombres son obligatorios', max: 100, largoMsg: 'Los nombres no pueden superar los 100 caracteres');

  static String? apellidos(String? v) => texto(v,
      obligatorioMsg: 'Los apellidos son obligatorios', max: 100, largoMsg: 'Los apellidos no pueden superar los 100 caracteres');

  /// El formato del CI está por definir con la DUBSS: solo no vacío y largo máximo.
  static String? ci(String? v) =>
      texto(v, obligatorioMsg: 'El CI es obligatorio', max: 20, largoMsg: 'El CI no puede superar los 20 caracteres');

  /// Exactamente 9 dígitos y sin cero inicial.
  static String? registroUniversitario(String? v) {
    final falta = obligatorio(v, 'El registro universitario es obligatorio');
    if (falta != null) return falta;
    return _registro.hasMatch(v!) ? null : 'El registro universitario debe tener 9 dígitos y no empezar por 0';
  }

  static String? carrera(String? v) => (v == null || v.isEmpty) ? 'La carrera es obligatoria' : null;

  static String? telefono(String? v) => texto(v,
      obligatorioMsg: 'El teléfono es obligatorio', max: 20, largoMsg: 'El teléfono no puede superar los 20 caracteres');

  static String? correo(String? v) {
    final falta = obligatorio(v, 'El correo electrónico es obligatorio');
    if (falta != null) return falta;
    final limpio = v!.trim();
    if (limpio.length > 150) return 'El correo no puede superar los 150 caracteres';
    return _correo.hasMatch(limpio) ? null : 'El correo electrónico no es válido';
  }

  /// De 6 a 72 caracteres (72 es el máximo que procesa BCrypt), sin más exigencias.
  static String? contrasena(String? v) {
    if (v == null || v.isEmpty) return 'La contraseña es obligatoria';
    if (v.length < 6) return 'La contraseña debe tener al menos 6 caracteres';
    if (v.length > 72) return 'La contraseña no puede superar los 72 caracteres';
    return null;
  }

  static String? confirmacion(String? v, String contrasena) {
    if (v == null || v.isEmpty) return 'Confirma tu contraseña';
    return v == contrasena ? null : 'Las contraseñas no coinciden';
  }

  /// El login no valida el largo: cualquier dato incorrecto da el mismo error del backend.
  static String? contrasenaLogin(String? v) => (v == null || v.isEmpty) ? 'La contraseña es obligatoria' : null;
}
