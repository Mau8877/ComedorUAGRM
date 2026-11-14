// Contrato del backend de CU06 -- ver UT-BACKEND/README.md (ConvocatoriaPriorizableResponse,
// ResumenListaPriorizadaResponse y PostulacionPriorizadaResponse).

export interface ConvocatoriaPriorizable {
  codigo: string
  nombre: string
  /** Etiqueta de la gestión académica (ej. «2026-II»). */
  gestion: string
  estado: 'BORRADOR' | 'ABIERTA' | 'CERRADA'
  fechaApertura: string
  fechaCierre: string
}

export interface ResumenListaPriorizada {
  totalCalificadas: number
  totalPendientes: number
}

export interface PostulacionPriorizada {
  /** Código público de la postulación. */
  codigo: string
  /** Lugar en la lista completa de la convocatoria (no en la página ni en el filtro). */
  posicion: number
  nombres: string
  apellidos: string
  ci: string
  carrera: { codigo: string; nombre: string }
  puntaje: number
  fechaCalculo: string
  estado: string
}

/** Filtros compartidos por la lista y la exportación a PDF. */
export interface FiltrosLista {
  convocatoriaId: string
  carreraId?: string
  search?: string
}

export interface ListaPriorizadaParams extends FiltrosLista {
  page: number
  pageSize: number
}
