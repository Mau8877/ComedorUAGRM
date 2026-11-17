// Contrato del backend del CU13 -- ver UT-BACKEND/README.md. Los nombres de campo son los de los Response DTO.
// Las fechas son de calendario (`yyyy-MM-dd`, hora de Bolivia): nunca se convierten con `new Date()`.

export type EstadoConvocatoria = 'BORRADOR' | 'ABIERTA' | 'CERRADA' | 'PUBLICADA'

export interface GestionResumen {
  codigo: string
  etiqueta: string
}

export interface GestionAcademica extends GestionResumen {
  fechaInicio: string
  fechaFin: string
}

export interface TipoDocumento {
  codigo: string
  nombre: string
  descripcion: string | null
}

/** Una fila del listado de convocatorias. `cantidadFinalistas` solo la ve el Administrador. */
export interface Convocatoria {
  codigo: string
  nombre: string
  gestion: GestionResumen
  fechaApertura: string
  fechaCierre: string
  cupos: number | null
  puntajeMinimo: number | null
  cantidadFinalistas: number | null
  estado: EstadoConvocatoria
}

export interface DocumentoRequerido {
  tipoDocumento: TipoDocumento
  obligatorio: boolean
}

/** El detalle: lo del listado más sus documentos y si ya tiene postulaciones (con postulaciones no se cambian la apertura ni los documentos). */
export interface ConvocatoriaDetalle extends Convocatoria {
  tienePostulaciones: boolean
  documentos: DocumentoRequerido[]
}

/** Parámetros de `GET /convocatorias` (el estado y la gestión viajan como `filter[estado]` y `filter[gestion]`). */
export interface ConvocatoriasListParams {
  page: number
  pageSize: number
  search?: string
  estado?: EstadoConvocatoria
  gestion?: string
  sort?: string
}

export interface GestionesListParams {
  page: number
  pageSize: number
  search?: string
  sort?: string
}

export interface TiposDocumentoListParams {
  page: number
  pageSize: number
  search?: string
  sort?: string
}

/** Bloque `meta` de todo listado paginado. */
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
