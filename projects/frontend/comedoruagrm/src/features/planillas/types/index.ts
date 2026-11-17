// Contrato del backend de CU09 -- ver UT-BACKEND/README.md (PlanillaResponse, ResumenPlanillaResponse,
// AccesoPlanillaResponse, GenerarPlanillaRequest). Solo códigos públicos: nunca ids internos ni la ruta del archivo.

/** Una planilla de asistencia generada. `generadaPor` es solo el nombre para mostrar. */
export interface Planilla {
  codigo: string
  /** Código de la convocatoria a la que pertenece. */
  convocatoria: string
  /** `AAAA-MM-DD`, ambas fechas incluidas. */
  fechaDesde: string
  fechaHasta: string
  /** Becados vigentes que incluyó al generarse. */
  totalBecados: number
  generadaEn: string
  generadaPor: string
}

/** Lo que la pantalla necesita antes de generar: cuántos becados entrarían y los límites del periodo. */
export interface ResumenPlanilla {
  becadosVigentes: number
  /** Etiqueta de la gestión académica. */
  gestion: string
  /** `AAAA-MM-DD`: el periodo tiene que estar dentro de la gestión. */
  gestionDesde: string
  gestionHasta: string
}

/** El periodo de una planilla (el cuerpo de `POST /convocatorias/{codigo}/planillas`). */
export interface PeriodoPlanilla {
  fechaDesde: string
  fechaHasta: string
}

/** La dirección temporal y firmada del PDF; vence en `expiraEn`. */
export interface AccesoPlanilla {
  url: string
  expiraEn: string
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

/** `sort` es `campo` o `-campo`; el backend solo admite `generadaEn` y `fechaDesde`. */
export interface PlanillasParams {
  page: number
  pageSize: number
  sort?: string
}
