// Contrato del backend del CU23 -- ver UT-BACKEND/README.md (BecaResponse, ResumenPadronResponse,
// CandidatoReemplazoResponse, ReemplazoResponse). Solo códigos públicos: nunca ids internos. Las fechas son de
// calendario (`yyyy-MM-dd`, hora de Bolivia): nunca se convierten con `new Date()`.

export type EstadoBeca = 'VIGENTE' | 'SUSPENDIDA' | 'BAJA'

export interface Becado {
  /** El código público del postulante. */
  codigo: string
  nombres: string
  apellidos: string
  ci: string
  carrera: string
}

/** Una fila del padrón. `fechaFin` es nula mientras ningún CU la defina. */
export interface Beca {
  codigo: string
  becado: Becado
  estado: EstadoBeca
  fechaInicio: string
  fechaFin: string | null
}

/** El resumen del padrón de una convocatoria. `pendientesDeAlta` es 0 mientras no se publiquen los resultados. */
export interface ResumenPadron {
  convocatoria: { codigo: string; nombre: string }
  resultadosPublicados: boolean
  adjudicados: number
  pendientesDeAlta: number
  vigentes: number
  suspendidas: number
  bajas: number
}

/** El postulante que se propone para ocupar un cupo liberado. `posicion` es la de la lista priorizada. */
export interface CandidatoReemplazo {
  postulanteCodigo: string
  nombres: string
  apellidos: string
  ci: string
  carrera: string
  posicion: number
  puntajeTotal: number
}

export interface Reemplazo {
  becaLiberada: Beca
  becaReemplazante: Beca
}

export interface AltaPadron {
  creadas: number
}

export interface MetaPagina {
  page: number
  pageSize: number
  totalItems: number
  totalPages: number
}

export interface Pagina<T> {
  data: T[]
  meta: MetaPagina
}

export interface ParamsPadron {
  page: number
  pageSize: number
  /** Busca en nombres, apellidos y CI. */
  search?: string
  estado?: EstadoBeca
  /** `becado`, `ci`, `carrera`, `estado` o `fechaInicio`, con `-` para descendente. */
  sort?: string
}
