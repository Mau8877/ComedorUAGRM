// Contrato del backend de CU22 -- ver UT-BACKEND/README.md (ResumenPublicacionResponse, DecisionResultadoResponse,
// PublicacionResultadoResponse, ConvocatoriaPublicadaResponse, AdjudicadoPublicoResponse, MiResultadoResponse).
// Solo códigos públicos: nunca ids internos.

/** La convocatoria de un resumen o de una publicación. */
export interface ConvocatoriaResultados {
  codigo: string
  nombre: string
  /** `BORRADOR`, `ABIERTA`, `CERRADA` o `PUBLICADA`. */
  estado: string
}

/** Lo que el Administrador revisa antes de publicar. `publicadoEn` y `publicadoPor` son `null` si no se publicó. */
export interface ResumenPublicacion {
  convocatoria: ConvocatoriaResultados
  adjudicados: number
  noAdjudicados: number
  /** Finalistas que siguen en «calificada» (sin decisión). */
  calificadasPendientes: number
  totalFinalistas: number
  /** Cuántos finalistas recibirán aviso al publicar. */
  aAvisar: number
  yaPublicada: boolean
  publicadoEn: string | null
  publicadoPor: string | null
  /** Verdadero solo si está cerrada, no publicada y sin decisiones pendientes. */
  puedePublicar: boolean
}

export type ResultadoDecision = 'ADJUDICADA' | 'NO_ADJUDICADA'

/** Una decisión de adjudicación. Sin CI, correo ni motivo. `puntajeFinal` puede ser `null`. */
export interface DecisionResultado {
  finalista: string
  nombres: string
  apellidos: string
  resultado: ResultadoDecision
  puntajeFinal: number | null
  fechaAdjudicacion: string
}

export interface PublicacionResultado {
  convocatoria: ConvocatoriaResultados
  publicadoEn: string
  publicadoPor: string
  adjudicados: number
  noAdjudicados: number
  avisados: number
}

export interface ConvocatoriaPublicada {
  codigo: string
  nombre: string
  publicadoEn: string
}

/** Un adjudicado de la lista pública: solo estos cinco datos. `nota` es el puntaje final. */
export interface AdjudicadoPublico {
  nombres: string
  apellidos: string
  nota: number | null
  carrera: string
  facultad: string
}

/** El resultado propio del postulante; `resultado` es `null` si no fue finalista con decisión. Nunca lleva el motivo. */
export interface MiResultado {
  resultado: ResultadoDecision | null
  nota: number | null
  publicadoEn: string
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

/** Parámetros de las decisiones del Administrador; `sort` es `campo` o `-campo` (puntajeFinal, apellidos, nombres). */
export interface DecisionesParams {
  page: number
  pageSize: number
  resultado?: ResultadoDecision
  search?: string
  sort?: string
}

/** Parámetros de la lista pública; `sort` es `campo` o `-campo` (nota, apellidos, nombres). */
export interface AdjudicadosParams {
  page: number
  pageSize: number
  search?: string
  sort?: string
}

export interface PublicadosParams {
  page: number
  pageSize: number
}
