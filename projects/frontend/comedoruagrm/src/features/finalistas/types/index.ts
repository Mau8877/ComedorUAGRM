// Contrato del backend de CU21 -- ver UT-BACKEND/README.md (FinalistaResponse,
// ResultadoNotificacionResponse).

export type EstadoNotificacion = 'SIN_ENVIAR' | 'ENVIADA' | 'FALLIDA'

/** Resultado de la última auditoría del expediente (CU07); `SIN_AUDITAR` si todavía no hay ninguna. */
export type ResultadoAuditoriaFinalista = 'SIN_AUDITAR' | 'VALIDADO' | 'OBSERVADO' | 'NO_VALIDO'

export interface Finalista {
  /** Código público del finalista. */
  codigo: string
  /** Lugar del finalista en la convocatoria (lo manda el backend). */
  posicion: number
  nombres: string
  apellidos: string
  ci: string
  puntaje: number | null
  estadoNotificacion: EstadoNotificacion
  /** Fecha (sin hora) límite de entrega, `YYYY-MM-DD`. */
  plazoEntrega: string | null
  notificadoEn: string | null
  /** Fecha (sin hora) en que entregó la documentación, `YYYY-MM-DD`; con valor = «Recibida». */
  documentacionRecibidaEn: string | null
  /** Resultado de la última auditoría del expediente (CU07). */
  resultadoAuditoria: ResultadoAuditoriaFinalista
  /** IA + puntaje académico de la última auditoría; nulo mientras falte alguno de los dos. */
  puntajeTotal: number | null
}

export interface FinalistasParams {
  convocatoriaId: string
  search?: string
  page: number
  pageSize: number
}

/** `enviados` son los avisos registrados en la bandeja; `fallidos`, los que no se pudieron registrar. */
export interface ResultadoNotificacion {
  solicitados: number
  enviados: number
  fallidos: number
}

export interface NotificarFinalistasRequest {
  finalistas: string[]
  plazoEntrega: string
  lugarHorario: string
  documentos: string
}

export interface RegistrarEntregaRequest {
  fechaRecepcion?: string
  observaciones?: string
}
