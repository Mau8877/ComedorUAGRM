import 'perfil.dart';

/// Cuerpo de `PATCH /perfil` (CU15): solo los datos de contacto que cambiaron.
///
/// * `toJson` omite los campos nulos: lo que no cambió no viaja. El CI, los nombres y
/// * los demás datos no son parte de esta clase (el CI no se modifica, RF-10).
/// * `contrasenaActual` solo viaja cuando cambia el correo (reautenticación, A7).
/// ! La contraseña no se loguea ni se guarda fuera del formulario.
class ActualizarPerfilRequest {
  final String? correo;
  final String? telefono;
  final String? contrasenaActual;

  const ActualizarPerfilRequest({this.correo, this.telefono, this.contrasenaActual});

  /// Arma el request con solo lo que cambió respecto del [perfil]. Un correo que difiere solo en
  /// mayúsculas o espacios no cuenta como cambio.
  factory ActualizarPerfilRequest.desdeCambios({
    required Perfil perfil,
    required String correo,
    required String telefono,
    required String contrasenaActual,
  }) {
    final correoCambio = correo.trim().toLowerCase() != perfil.correo.trim().toLowerCase();
    final telefonoActual = perfil.postulante?.telefono;
    final telefonoCambio = telefonoActual != null && telefono.trim() != telefonoActual;
    return ActualizarPerfilRequest(
      correo: correoCambio ? correo.trim() : null,
      telefono: telefonoCambio ? telefono.trim() : null,
      contrasenaActual: correoCambio ? contrasenaActual : null,
    );
  }

  /// `true` si no hay nada que guardar.
  bool get vacio => correo == null && telefono == null;

  Map<String, dynamic> toJson() => {
        'correo': ?correo,
        'telefono': ?telefono,
        'contrasenaActual': ?contrasenaActual,
      };
}
