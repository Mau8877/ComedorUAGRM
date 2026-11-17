// Contrato del backend de CU16 -- ver docs/UnidadesTrabajo/CU16 - Digitalizar y adjuntar documentos/UT-BACKEND/README.md
// (DocumentosPostulacionResponse y compañía). Solo códigos públicos: nunca ids internos ni la ruta de almacenamiento.

/** Un archivo ya adjuntado a un documento requerido. */
export interface ArchivoAdjunto {
  codigo: string
  nombreArchivo: string
  mimeType: string
  tamanoBytes: number
  subidoEn: string
}

/** Un documento que exige la convocatoria, con su estado y los archivos que lleva. */
export interface DocumentoRequerido {
  tipoDocumentoCodigo: string
  nombre: string
  descripcion: string | null
  obligatorio: boolean
  adjuntado: boolean
  archivos: ArchivoAdjunto[]
}

/** Lo que el cliente valida antes de subir (el backend vuelve a validar, por el contenido del archivo). */
export interface ReglasArchivo {
  tamanoMaximoBytes: number
  /** Tipos MIME permitidos, p. ej. `application/pdf`. */
  formatosPermitidos: string[]
  maximoArchivosPorTipo: number
}

export interface DocumentosPostulacion {
  /** Si todavía se puede adjuntar, reemplazar o eliminar (borrador o devuelta, con la convocatoria abierta). */
  puedeEditar: boolean
  requeridos: DocumentoRequerido[]
  reglas: ReglasArchivo
}

/** Una dirección temporal firmada para abrir un archivo. Nunca una ruta permanente. */
export interface AccesoDocumento {
  url: string
  expiraEn: string
}
