import 'package:flutter/material.dart';

import '../../auth/rol_destino.dart';
import '../models/perfil.dart';
import 'campo_solo_lectura.dart';

/// Datos del usuario que NO se editan desde el perfil (CU15, flujo C): nombres, apellidos, CI, rol y estado
/// y, si es Postulante, su registro universitario y carrera. El CI nunca es un campo editable (A4, RF-10);
/// los nombres del personal los corrige el Administrador DUBSS en la gestión de cuentas.
class MisDatos extends StatelessWidget {
  final Perfil perfil;

  const MisDatos({super.key, required this.perfil});

  @override
  Widget build(BuildContext context) {
    final postulante = perfil.postulante;
    final gap = const SizedBox(height: 16);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Mis datos', style: Theme.of(context).textTheme.titleLarge),
        gap,
        CampoSoloLectura(label: 'Nombres', valor: perfil.nombres),
        gap,
        CampoSoloLectura(label: 'Apellidos', valor: perfil.apellidos),
        gap,
        CampoSoloLectura(label: 'CI', valor: perfil.ci, nota: 'El CI no se puede modificar.'),
        gap,
        CampoSoloLectura(label: 'Rol', valor: etiquetaRol(perfil.rol)),
        gap,
        CampoSoloLectura(label: 'Estado de la cuenta', valor: perfil.activo ? 'Activa' : 'Desactivada'),
        if (postulante != null) ...[
          gap,
          CampoSoloLectura(label: 'Registro universitario', valor: postulante.registroUniversitario),
          gap,
          CampoSoloLectura(label: 'Carrera', valor: '${postulante.carrera.nombre} (${postulante.carrera.facultad})'),
        ],
      ],
    );
  }
}
