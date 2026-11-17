// Contrato del backend de CU11 -- ver UT-BACKEND/README.md (DatosCotejoResponse, RegistroDigitalResponse,
// CotejoResponse, RegistrarCotejoRequest). Solo códigos públicos: nunca ids internos ni el CI.

export type ResultadoCotejo = 'CONFORME' | 'CON_DISCREPANCIAS'

/** Lo que se necesita para cotejar una planilla: sus datos y cuántos registros digitales hay en su periodo. */
export interface DatosCotejo {
  planilla: string
  convocatoria: string
  convocatoriaNombre: string
  /** `AAAA-MM-DD`, ambas fechas incluidas. */
  fechaDesde: string
  fechaHasta: string
  totalBecados: number
  generadaEn: string
  /** 0 cuando no hay ninguno (A1): no es un error. */
  registrosDigitales: number
  becadosConAsistencia: number
}

/** Un registro digital de asistencia dentro del periodo de la planilla. */
export interface RegistroDigital {
  nombres: string
  apellidos: string
  carrera: string
  /** `AAAA-MM-DD`. */
  fecha: string
  horaRegistro: string
}

/** Un cotejo ya registrado. `realizadoPor` es solo el nombre para mostrar. */
export interface Cotejo {
  codigo: string
  planilla: string
  fechaCotejo: string
  totalRegistros: number
  coincidencias: number
  discrepancias: number
  resultado: ResultadoCotejo
  observaciones: string | null
  realizadoPor: string
  creadoEn: string
}

/** El cuerpo de `POST /planillas/{codigo}/cotejos`. */
export interface RegistrarCotejoRequest {
  totalRegistros: number
  coincidencias: number
  discrepancias: number
  resultado: ResultadoCotejo
  observaciones?: string
}

/** `sort` es `campo` o `-campo`; cada listado admite solo los campos que documenta el backend. */
export interface ListadoParams {
  page: number
  pageSize: number
  sort?: string
}
