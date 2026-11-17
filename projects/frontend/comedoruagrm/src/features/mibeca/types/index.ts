// Contrato del backend de CU24 (MiBecaResponse y AsistenciaBecadoResponse). Solo lectura y sin ids internos.

export type EstadoBeca = 'VIGENTE' | 'SUSPENDIDA' | 'BAJA'

export interface MiBeca {
  codigo: string
  estado: EstadoBeca
  convocatoria: string
  gestion: string
  /** `null` en una beca de reemplazo (no tiene adjudicación propia). */
  fechaAdjudicacion: string | null
  /** AAAA-MM-DD. */
  fechaInicio: string
  fechaFin: string | null
  /** Solo con la beca suspendida o dada de baja. */
  motivo: string | null
  totalAsistencias: number
}

export interface AsistenciaBecado {
  /** AAAA-MM-DD, día de calendario en hora de Bolivia. */
  fecha: string
  /** Instante ISO en que el Personal del Comedor la registró. */
  horaRegistro: string
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

export interface AsistenciasParams {
  page: number
  pageSize: number
  /** AAAA-MM-DD, hora de Bolivia, inclusivo. */
  desde?: string
  hasta?: string
  /** `fecha` o `-fecha` (por defecto el backend manda la más reciente primero). */
  sort?: string
}
