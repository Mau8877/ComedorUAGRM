// Contrato del backend de CU25 (BitacoraResponse). Solo lectura. Sin ids internos: la entidad afectada viene como
// `entidad` y, cuando aplica, con los códigos públicos dentro de `detalle`.

export interface ActorBitacora {
  codigo: string
  nombres: string
  apellidos: string
  correo: string
}

export interface RegistroBitacora {
  codigo: string
  fecha: string
  /** `null` cuando lo hizo el sistema (ej. el cierre automático de una convocatoria). */
  usuario: ActorBitacora | null
  accion: string
  entidad: string | null
  detalle: Record<string, unknown> | null
  ip: string | null
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

export interface BitacoraParams {
  page: number
  pageSize: number
  /** Busca en nombres, apellidos y correo del usuario. */
  search?: string
  accion?: string
  /** AAAA-MM-DD, hora de Bolivia, inclusivo. */
  desde?: string
  hasta?: string
  sort?: string
}
