// Contrato del backend del CU-02 (enviar postulación): EnvioPostulacionController. Solo códigos públicos.

export interface PerfilFormulario {
  nombres: string
  apellidos: string
  ci: string
  carrera: string
}

export interface ConvocatoriaFormulario {
  codigo: string
  nombre: string
  gestion: string
  /** Instante ISO-8601 (UTC). */
  fechaCierre: string
}

/** Las opciones de las listas desplegables: las define el backend (por definir con la DUBSS), el cliente no las repite. */
export interface OpcionesFormulario {
  procedencias: string[]
  patrimonios: string[]
  estadosVulnerabilidad: string[]
  // * Las del formulario oficial de la DUBSS (FORM-DUBS-001).
  dependencias: string[]
  ocupacionesSosten: string[]
  rangosIngresos: string[]
  cantidadesHijos: string[]
  tenenciasVivienda: string[]
}

/** Una fila de la tabla «Grupo familiar» del formulario oficial (hasta 10). */
export interface MiembroGrupoFamiliar {
  nombre: string | null
  parentesco: string | null
  edad: number | null
  ocupacion: string | null
  observacion: string | null
}

/** Los datos del formulario oficial de la DUBSS que el backend guarda aparte (`datos_extra`). Todo puede ser nulo. */
export interface DatosComplementarios {
  dependenciaEconomica: string | null
  ocupacionSosten: string | null
  rangoIngresos: string | null
  tieneHijos: boolean | null
  cantidadHijos: string | null
  grupoFamiliar: MiembroGrupoFamiliar[] | null
  residencia: {
    provincia: string | null
    zonaAnillo: string | null
    barrio: string | null
    calle: string | null
  } | null
  tenenciaVivienda: string | null
  infraestructura: {
    dormitorios: number | null
    banos: number | null
    comedor: number | null
    sala: number | null
    patio: number | null
  } | null
  otroBeneficio: boolean | null
  otroBeneficioDetalle: string | null
  discapacidad: boolean | null
  discapacidadDetalle: string | null
  comentario: string | null
}

/** Las respuestas ya guardadas (reenvío de una devuelta); cualquier campo puede ser nulo. */
export interface RespuestaSocioeconomica {
  ingresosFamiliares: number | null
  dependientes: number | null
  procedencia: string | null
  patrimonio: string | null
  estadoVulnerabilidad: string | null
  promedioAcademico: number | null
  /** Instante ISO-8601 del último guardado (CU-17); `null` si todavía no se guardó nada. */
  guardadoEn: string | null
  /** Los datos del formulario oficial; `null` si todavía no se guardó ninguno. */
  complementarios: DatosComplementarios | null
}

/** Lo que muestra «Nueva postulación»: la postulación en edición (borrador, o devuelta a corregir) y su contexto. */
export interface FormularioPostulacion {
  postulacion: string
  estado: 'borrador' | 'devuelta'
  numeroEnvios: number
  convocatoria: ConvocatoriaFormulario
  perfil: PerfilFormulario
  opciones: OpcionesFormulario
  respuesta: RespuestaSocioeconomica | null
}

/** El cuerpo de `POST /postulaciones/{codigo}/enviar`. */
export interface EnviarPostulacionRequest {
  ingresosFamiliares: number
  dependientes: number
  procedencia: string
  patrimonio: string
  estadoVulnerabilidad: string
  promedioAcademico: number
  complementarios: DatosComplementarios
}

/** El comprobante del envío: `codigo` es el código público de la postulación. */
export interface ComprobantePostulacion {
  codigo: string
  estado: string
  fechaEnvio: string
  numeroEnvios: number
  reenvio: boolean
  convocatoria: string
  gestion: string
}

// ---- Borrador de la postulación (CU-17) ----

/**
 * El cuerpo de `PUT /postulaciones/{codigo}/borrador`: el formulario tal como está. Todo puede ir en `null`: un campo
 * omitido se guarda vacío (es `PUT`, la representación completa).
 */
export interface GuardarBorradorRequest {
  ingresosFamiliares: number | null
  dependientes: number | null
  procedencia: string | null
  patrimonio: string | null
  estadoVulnerabilidad: string | null
  promedioAcademico: number | null
  complementarios: DatosComplementarios | null
}

/** La confirmación de «Guardar borrador». */
export interface BorradorGuardado {
  postulacion: string
  guardadoEn: string
  camposPendientes: number
}

/** «Mi borrador»: el borrador de la convocatoria abierta y lo que le falta para poder enviarlo. */
export interface BorradorResumen {
  postulacion: string
  convocatoria: string
  gestion: string
  fechaCierre: string
  guardadoEn: string | null
  camposPendientes: number
  documentosPendientes: number
}
