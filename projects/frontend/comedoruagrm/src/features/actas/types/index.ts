// Contrato del backend de CU08 -- ver UT-BACKEND/README.md (ActaResponse, ConvocatoriaPendienteActaResponse,
// AccesoActaResponse). Solo códigos públicos: nunca ids internos ni la ruta del archivo.

export interface ConvocatoriaActa {
  codigo: string
  nombre: string
}

/** Un acta generada. `generadaPor` es solo el nombre para mostrar. */
export interface Acta {
  codigo: string
  /** `NNN/AAAA`, lo asigna el backend. */
  numero: string
  convocatoria: ConvocatoriaActa
  /** Etiqueta de la gestión académica. */
  gestion: string
  adjudicados: number
  generadaEn: string
  generadaPor: string
}

/** Una convocatoria con resultados publicados y sin acta. */
export interface ConvocatoriaPendienteActa {
  codigo: string
  nombre: string
  publicadoEn: string
  adjudicados: number
}

/** La dirección temporal y firmada del PDF; vence en `expiraEn`. */
export interface AccesoActa {
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

/** `sort` es `campo` o `-campo`; el backend solo admite `numero` y `generadaEn`. */
export interface ActasParams {
  page: number
  pageSize: number
  search?: string
  sort?: string
}

export interface PendientesParams {
  page: number
  pageSize: number
}
