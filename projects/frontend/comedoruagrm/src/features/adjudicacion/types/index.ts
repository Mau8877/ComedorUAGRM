// Contrato del backend de CU05 -- ver UT-BACKEND/README.md (PanelAdjudicacionResponse,
// SeleccionarFinalistasRequest, SeleccionFinalistasResponse). Solo códigos públicos: nunca ids internos.

/** Datos básicos de la convocatoria del panel. */
export interface ConvocatoriaPanel {
  codigo: string
  nombre: string
  /** `BORRADOR`, `ABIERTA` o `CERRADA`. */
  estado: string
}

/**
 * Estado de adjudicación de una convocatoria. `cupos`, `puntajeMinimo`, `cuposDisponibles` y
 * `cantidadFinalistas` son `null` mientras no estén definidos (la DUBSS aún no los fija): nunca se inventan.
 * `cantidadFinalistas` es un dato solo del Administrador.
 */
export interface PanelAdjudicacion {
  convocatoria: ConvocatoriaPanel
  cupos: number | null
  puntajeMinimo: number | null
  adjudicados: number
  noAdjudicados: number
  cuposDisponibles: number | null
  cantidadFinalistas: number | null
  totalFinalistas: number
  calificadasSinSeleccionar: number
  resultadosPublicados: boolean
  /** Verdadero solo si la convocatoria está cerrada, tiene cupos y puntaje mínimo y no se publicaron los resultados. */
  decisionesHabilitadas: boolean
}

/** Cuántos finalistas seleccionar; sin `cantidad` el backend usa la configurada en la convocatoria. */
export interface SeleccionarFinalistasRequest {
  cantidad?: number
}

export interface FinalistaSeleccionado {
  /** Código público del finalista. */
  codigo: string
  /** Código público de su postulación. */
  postulacion: string
  /** Lugar en la lista priorizada completa. */
  posicion: number
}

export interface SeleccionFinalistasResultado {
  convocatoria: string
  seleccionados: number
  finalistas: FinalistaSeleccionado[]
}
