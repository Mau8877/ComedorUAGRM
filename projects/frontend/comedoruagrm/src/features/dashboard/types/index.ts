// Tipos que reflejan exactamente los DTOs del backend del dashboard (CU12).

export interface ConvocatoriaFiltro {
  codigo: string
  nombre: string
}

export interface GestionFiltro {
  codigo: string
  etiqueta: string
  /** La gestión que contiene la fecha de hoy (o la más reciente si ninguna la contiene). */
  vigente: boolean
  convocatorias: ConvocatoriaFiltro[]
}

export interface FiltrosDashboard {
  gestiones: GestionFiltro[]
}

export interface Totales {
  postulacionesRecibidas: number
  finalistas: number
  becasAdjudicadas: number
  /** null mientras no exista el registro de asistencia (CU-10). */
  asistenciaPromedio: number | null
}

export interface IndicadorMensual {
  /** `yyyy-MM`. */
  mes: string
  postulaciones: number
  finalistas: number
  adjudicadas: number
  /** null mientras no exista el registro de asistencia (CU-10). */
  asistencia: number | null
}

export interface Indicadores {
  /** null si todavía no hay gestiones. */
  gestion: { codigo: string; etiqueta: string } | null
  /** null = «Todas las convocatorias». */
  convocatoria: { codigo: string; nombre: string } | null
  totales: Totales
  porMes: IndicadorMensual[]
  /** null si el lote nunca se ejecutó. */
  actualizadoEn: string | null
  sinDatos: boolean
}

/** La selección de filtros; sin valor = sin filtro (gestión vigente / todas las convocatorias). */
export interface SeleccionDashboard {
  gestion?: string
  convocatoria?: string
}
