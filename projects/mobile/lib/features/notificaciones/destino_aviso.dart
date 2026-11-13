import 'models/mensaje_push.dart';

/// A dónde lleva tocar un aviso push (CU04, A6): al detalle de la postulación si el aviso trae su código; sin
/// código (o sin aviso) no lleva a ningún lado y la app solo se abre. Es solo el mapeo del dato a una ruta: esta
/// feature no importa nada de `postulaciones/`, emite la ruta hacia el router.
String? rutaDeAviso(MensajePush? mensaje) {
  final codigo = mensaje?.postulacionCodigo;
  if (codigo == null || codigo.isEmpty) return null;
  return '/mis-postulaciones/$codigo';
}
