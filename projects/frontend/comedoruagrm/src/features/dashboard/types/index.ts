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

// ── Análisis BI (GET /dashboard/bi) ──

export interface Embudo {
  postulaciones: number
  precalificadas: number
  finalistas: number
  adjudicadas: number
  becasVigentes: number
}

export interface CarreraBi {
  nombre: string
  postulaciones: number
  adjudicadas: number
}

export interface FacultadBi {
  nombre: string
  postulaciones: number
  adjudicadas: number
  /** Parte del total de postulaciones, 0–100; null sin postulaciones. */
  porcentajePostulaciones: number | null
  /** Adjudicadas sobre postulaciones, 0–100; null sin postulaciones. */
  tasaAdjudicacion: number | null
  carreras: CarreraBi[]
}

export interface ConvocatoriaComparada {
  codigo: string
  nombre: string
  cupos: number | null
  postulaciones: number
  finalistas: number
  adjudicadas: number
  tasaAdjudicacion: number | null
  puntajePromedio: number | null
  /** Variación (%) de postulaciones frente a la convocatoria anterior; null si no hay con qué comparar. */
  variacionPostulaciones: number | null
  seleccionada: boolean
}

export interface TramoPuntaje {
  desde: number
  hasta: number
  cantidad: number
}

export interface Cobertura {
  cupos: number
  becasVigentes: number
  becasSuspendidas: number
  becasBaja: number
  reemplazos: number
  porcentaje: number | null
}

export interface AsistenciaDia {
  /** 1 = lunes … 7 = domingo. */
  dia: number
  tasa: number | null
}

export interface AsistenciaBi {
  tasaPromedio: number | null
  diasConRegistro: number
  becasEnRiesgo: number
  porDiaSemana: AsistenciaDia[]
}

export interface GestionTendencia {
  codigo: string
  etiqueta: string
  postulaciones: number
  adjudicadas: number
  variacionPostulaciones: number | null
  seleccionada: boolean
}

export interface Bi {
  gestion: { codigo: string; etiqueta: string } | null
  convocatoria: { codigo: string; nombre: string } | null
  embudo: Embudo
  facultades: FacultadBi[]
  convocatorias: ConvocatoriaComparada[]
  puntaje: TramoPuntaje[]
  puntajePromedio: number | null
  cobertura: Cobertura
  asistencia: AsistenciaBi
  tendencia: GestionTendencia[]
  actualizadoEn: string | null
  sinDatos: boolean
}
