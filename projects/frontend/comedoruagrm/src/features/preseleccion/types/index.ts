// Contrato del backend del CU26 -- ver UT-BACKEND/README.md (ModeloPreseleccionResponse,
// ModeloPreseleccionResumenResponse, ResumenReprocesamientoResponse, LoteReprocesamientoResponse). Solo códigos
// públicos: nunca ids internos. Las fechas con hora son instantes ISO-8601 (UTC).

/** Una variable del modelo y su ponderación (puntos máximos). `nombre` es para mostrar. */
export interface ParametroModelo {
  clave: string
  nombre: string
  ponderacion: number
}

/** Una versión del modelo con sus parámetros. `vigente` es la que usan los cálculos nuevos. */
export interface ModeloPreseleccion {
  codigo: string
  version: string
  descripcion: string | null
  vigente: boolean
  creadoEn: string
  parametros: ParametroModelo[]
}

/** Una fila del historial de versiones. */
export interface ModeloResumen {
  codigo: string
  version: string
  descripcion: string | null
  vigente: boolean
  creadoEn: string
}

export type EstadoLote = 'PENDIENTE' | 'EN_PROCESO' | 'COMPLETADO' | 'FALLIDO'

/** El avance de un reprocesamiento. `fallidas` son las que la IA no pudo calcular: reprocesar de nuevo las reintenta. */
export interface LoteReprocesamiento {
  codigo: string
  convocatoria: string
  modelo: string
  estado: EstadoLote
  total: number
  procesadas: number
  fallidas: number
  iniciadoEn: string | null
  terminadoEn: string | null
}

/** Lo que se muestra antes de confirmar. `modelo` es nulo mientras no se guarde ninguna versión. */
export interface ResumenReprocesamiento {
  convocatoria: string
  nombre: string
  gestion: string
  modelo: string | null
  calificadas: number
  porReprocesar: number
  resultadosPublicados: boolean
  loteEnCurso: LoteReprocesamiento | null
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

/** Parámetros de `GET /modelos-preseleccion` (el historial solo admite paginación). */
export interface ParamsHistorial {
  page: number
  pageSize: number
}

// ---- Precalificación con IA de una postulación (CU03): PrecalificacionResponse del backend ----

/** Cuánto aportó una variable. `nivel` es de 0 a 1 (nulo si la postulación no trae el dato); `puntos` y `maximo` están en la escala del puntaje. */
export interface AporteVariable {
  clave: string
  nivel: number | null
  ponderacion: number | null
  puntos: number | null
  maximo: number | null
}

/** Un paso del proceso automático, tal como quedó en el historial de la postulación. `fecha` es nula si el paso no tiene una. */
export interface PasoTraza {
  paso: string
  detalle: string | null
  fecha: string | null
}

/** Si todavía no se calificó, `puntaje`, `modelo`, `fechaCalculo`, `posicion` y `variables` vienen vacíos. */
export interface Precalificacion {
  postulacion: string
  nombres: string
  apellidos: string
  ci: string
  convocatoria: string
  estado: string
  puntaje: number | null
  puntajeMaximo: number | null
  modelo: string | null
  fechaCalculo: string | null
  posicion: number | null
  variables: AporteVariable[]
  advertencias: string[]
  marcadaParaRevision: boolean
  revisionDesde: string | null
  intentos: number
  traza: PasoTraza[]
}
