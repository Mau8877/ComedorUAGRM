/// Los estados que el postulante puede ver de una postulación (CU04). El `borrador` no se lista y la
/// decisión final sin publicar llega como `calificada`: la app nunca recalcula el estado, solo lo muestra.
enum EstadoPostulacion {
  pendiente('pendiente', 'Pendiente'),
  devuelta('devuelta', 'Devuelta'),
  archivada('archivada', 'Archivada'),
  calificada('calificada', 'Calificada'),
  adjudicada('adjudicada', 'Adjudicada'),
  noAdjudicada('no_adjudicada', 'No adjudicada');

  /// El valor tal cual lo manda el backend.
  final String valor;

  /// El texto que ve la persona.
  final String etiqueta;

  const EstadoPostulacion(this.valor, this.etiqueta);

  /// El estado de un valor del backend; un valor desconocido es un error de contrato.
  static EstadoPostulacion deValor(String valor) {
    for (final estado in values) {
      if (estado.valor == valor) return estado;
    }
    throw FormatException('Estado de postulación desconocido: $valor');
  }
}
