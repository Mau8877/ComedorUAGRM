// Contrato del backend de CU04 -- ver UT-BACKEND/README.md (MiPostulacionResumenResponse,
// MiPostulacionDetalleResponse). Solo códigos públicos: nunca ids internos, CI, correo ni el autor de un cambio.

/** Los estados que el postulante puede ver (el `borrador` no se lista y la decisión final sin publicar se ve «calificada»). */
export type EstadoPostulacion = 'pendiente' | 'devuelta' | 'archivada' | 'calificada' | 'adjudicada' | 'no_adjudicada'

export interface ConvocatoriaResumen {
  codigo: string
  nombre: string
}

/** `nombre` es la etiqueta de la gestión académica. */
export interface GestionAcademicaResumen {
  codigo: string
  nombre: string
}

/** Una postulación enviada en «Mis postulaciones». `estado` es el visible: el cliente nunca lo recalcula. */
export interface MiPostulacionResumen {
  codigo: string
  convocatoria: ConvocatoriaResumen
  gestionAcademica: GestionAcademicaResumen
  fechaEnvio: string
  estado: EstadoPostulacion
  numeroEnvios: number
}

export type SituacionPaso = 'COMPLETADO' | 'PENDIENTE'

/**
 * Un paso de la línea de tiempo. Los `COMPLETADO` salen del historial real (con fecha); los `PENDIENTE` son los que
 * faltan y no tienen fecha. `estado` es un estado de la postulación o `resultado` (el paso final de la adjudicación).
 */
export interface PasoLineaTiempo {
  estado: string
  fecha: string | null
  situacion: SituacionPaso
}

export interface Observaciones {
  texto: string
  fecha: string
}

/** El bloque «Eres finalista». `plazoEntrega` y `documentosFisicos` son `null` si todavía no hubo aviso. */
export interface FinalistaPostulante {
  plazoEntrega: string | null
  documentacionRecibida: boolean
  documentosFisicos: string | null
}

export type ResultadoPostulacion = 'ADJUDICADA' | 'NO_ADJUDICADA'

/**
 * El detalle de una postulación propia. Los bloques que no corresponden al estado vienen en `null`: `observaciones`
 * (solo «devuelta»), `motivo` (solo archivo o rechazo del CU20), `finalista` (solo si lo es) y `resultado` (solo con
 * los resultados publicados).
 */
export interface MiPostulacionDetalle extends MiPostulacionResumen {
  lineaDeTiempo: PasoLineaTiempo[]
  observaciones: Observaciones | null
  motivo: string | null
  finalista: FinalistaPostulante | null
  resultado: ResultadoPostulacion | null
  puedeCorregir: boolean
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

/** `sort` es `campo` o `-campo`; el backend solo admite `fechaEnvio`. `gestionAcademica` es el código público. */
export interface MisPostulacionesParams {
  page: number
  pageSize: number
  gestionAcademica?: string
  sort?: string
}
