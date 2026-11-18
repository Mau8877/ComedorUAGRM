// Tipos que reflejan exactamente los DTOs del backend de las pantallas de inicio (`/api/v1/panel`).

export interface ConvocatoriaAbierta {
  codigo: string
  nombre: string
  /** Instante ISO-8601. */
  fechaCierre: string
  cupos: number | null
  postulaciones: number
}

export interface PanelAdministrador {
  convocatoriasAbiertas: number
  postulacionesPendientes: number
  postulacionesCalificadas: number
  becasVigentes: number
  becasSuspendidas: number
  asistenciasHoy: number
  convocatoriaAbierta: ConvocatoriaAbierta | null
}

export interface AsistenciaDia {
  /** `yyyy-MM-dd`. */
  fecha: string
  asistencias: number
}

export interface PanelComedor {
  /** `yyyy-MM-dd`. */
  hoy: string
  becasVigentes: number
  asistenciasHoy: number
  registradasPorMi: number
  /** Instante ISO-8601; null si hoy no se registró ninguna. */
  ultimoRegistro: string | null
  /** Siempre 7 días, del más antiguo al de hoy. */
  ultimosDias: AsistenciaDia[]
}

export interface ConvocatoriaAbiertaPostulante {
  codigo: string
  nombre: string
  /** Instante ISO-8601. */
  fechaCierre: string
}

export interface PanelPostulante {
  convocatoriaAbierta: ConvocatoriaAbiertaPostulante | null
  /** Empezó a postular en la convocatoria abierta y no envió. */
  tieneBorrador: boolean
  /** Ya envió su postulación en la convocatoria abierta. */
  yaPostulo: boolean
}
