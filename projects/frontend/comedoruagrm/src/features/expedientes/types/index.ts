// Contrato del backend de CU07 -- ver UT-BACKEND/README.md (ExpedienteResponse, AuditoriaResponse,
// RegistrarAuditoriaRequest, AccesoDocumentoResponse). Solo códigos públicos: nunca ids internos ni la
// ruta de almacenamiento de un documento.

export type ResultadoAuditoria = 'VALIDADO' | 'OBSERVADO' | 'NO_VALIDO'

/** Resultado que muestra la lista de finalistas: `SIN_AUDITAR` cuando todavía no hay ninguna auditoría. */
export type ResultadoAuditoriaLista = ResultadoAuditoria | 'SIN_AUDITAR'

export type EstadoEntrega = 'RECIBIDA' | 'PENDIENTE'

export type EstadoDocumento = 'RECIBIDO' | 'FALTA'

export interface Auditoria {
  codigo: string
  resultado: ResultadoAuditoria
  observaciones: string | null
  /** De 0 a 30; nulo si el auditor no lo registró. */
  puntajeAcademico: number | null
  /** Instante ISO de la auditoría. */
  fecha: string
  /** Nombre completo de quien la registró. */
  auditor: string
}

/** Un tipo de documento del expediente; sin adjunto, `codigo`, `nombreArchivo` y `mimeType` son nulos. */
export interface DocumentoExpediente {
  codigo: string | null
  tipo: string
  obligatorio: boolean
  estado: EstadoDocumento
  nombreArchivo: string | null
  mimeType: string | null
  subidoEn: string | null
}

export interface RespuestaSocioeconomica {
  ingresosFamiliares: number | null
  dependientes: number | null
  patrimonio: string | null
  procedencia: string | null
  promedioAcademico: number | null
  estadoVulnerabilidad: string | null
}

export interface Expediente {
  finalista: {
    codigo: string
    posicion: number
    /** Fecha (sin hora) límite de entrega, `YYYY-MM-DD`. */
    plazoEntrega: string | null
    entrega: { estado: EstadoEntrega; fecha: string | null }
  }
  postulante: { nombres: string; apellidos: string; ci: string; carrera: string | null }
  convocatoria: { codigo: string; nombre: string }
  respuestaSocioeconomica: RespuestaSocioeconomica | null
  documentos: DocumentoExpediente[]
  /** Estado de la postulación (`calificada`, `devuelta`...); solo se audita una «calificada». */
  estadoPostulacion: string
  /** Puntaje vigente de la IA, de 0 a 70. */
  puntajeIa: number | null
  /** El `detalle` que guarda el módulo de IA; nulo si no existe. */
  desglose: Record<string, unknown> | null
  /** El de la ÚLTIMA auditoría. */
  puntajeAcademico: number | null
  /** IA + académico; nulo si falta alguno de los dos. Lo calcula el backend. */
  puntajeTotal: number | null
  /** De la más reciente a la más antigua. */
  auditorias: Auditoria[]
}

export interface RegistrarAuditoriaRequest {
  resultado: ResultadoAuditoria
  observaciones?: string
  puntajeAcademico?: number
}

/** `creada` es false cuando fue una repetición idempotente de la auditoría anterior. */
export interface ResultadoRegistroAuditoria {
  auditoria: Auditoria
  creada: boolean
}

export interface AccesoDocumento {
  /** URL temporal (5 minutos) para abrir el archivo. */
  url: string
  expiraEn: string
}

// Contrato del backend de CU19 (DevolverPostulacionRequest, DevolucionResponse).

export interface DevolverPostulacionRequest {
  /** Observaciones para el postulante: obligatorias, de hasta 500 caracteres. */
  motivo: string
}

export interface DevolucionResultado {
  /** Código público de la postulación. */
  postulacion: string
  estado: string
  motivo: string
  /** Instante ISO de la devolución. */
  fecha: string
  /** Nombre completo de quien la devolvió. */
  devueltaPor: string
}

// Contrato del backend de CU20 (CierrePostulacionRequest, CierrePostulacionResponse).

export interface CerrarPostulacionRequest {
  /** Motivo del cierre: obligatorio, de hasta 500 caracteres. */
  motivo: string
}

/** Qué se hace con la postulación calificada: archivarla («archivada») o rechazarla («no adjudicada»). */
export type AccionCierre = 'archivar' | 'rechazar'

export interface CierrePostulacionResultado {
  /** Código público de la postulación. */
  postulacion: string
  /** `archivada` o `no_adjudicada`. */
  estado: string
  motivo: string
  /** Instante ISO de la decisión. */
  fecha: string
  /** Nombre completo de quien decidió. */
  decididaPor: string
}
