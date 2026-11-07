// Contrato del backend de CU21 -- ver UT-BACKEND/README.md (FinalistaResponse,
// ResultadoNotificacionResponse).

export type EstadoNotificacion = 'SIN_ENVIAR' | 'ENVIADA' | 'FALLIDA'

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
