import 'models/estado_postulacion.dart';
import 'models/mi_postulacion_detalle.dart';

/// Los bloques de información asociada que puede mostrar el detalle de una postulación (CU04).
enum BloqueDetalle { observaciones, motivo, finalista, resultado }

/// Qué bloques corresponden a este detalle, en el orden en que se muestran. Se decide solo con lo que manda el
/// backend (los bloques que no corresponden llegan `null`); la app no recalcula el estado. El motivo solo se
/// muestra en una postulación archivada o rechazada, nunca junto a un resultado de la decisión final.
List<BloqueDetalle> bloquesDelDetalle(MiPostulacionDetalle detalle) {
  final estado = detalle.resumen.estado;
  return [
    if (detalle.observaciones != null && estado == EstadoPostulacion.devuelta) BloqueDetalle.observaciones,
    if (detalle.motivo != null &&
        (estado == EstadoPostulacion.archivada || estado == EstadoPostulacion.noAdjudicada))
      BloqueDetalle.motivo,
    if (detalle.finalista != null) BloqueDetalle.finalista,
    if (detalle.resultado != null) BloqueDetalle.resultado,
  ];
}
