import type { ComplementariosValues, MiembroGrupoFamiliarValues, RespuestasSocioeconomicasValues } from '../schemas'
import type { DatosComplementarios, RespuestaSocioeconomica } from '../types'

const texto = (n: number | string | null | undefined) => (n === null || n === undefined ? '' : String(n))
const siNo = (b: boolean | null | undefined) => (b === null || b === undefined ? '' : b ? 'si' : 'no')

/** Una fila vacía de «Grupo familiar», para el botón «Agregar familiar». */
export function miembroVacio(): MiembroGrupoFamiliarValues {
  return { nombre: '', parentesco: '', edad: '', ocupacion: '', observacion: '' }
}

/** Los datos del formulario oficial como texto; sin datos guardados, todo vacío (y el grupo familiar sin filas). */
export function complementariosIniciales(datos: DatosComplementarios | null | undefined): ComplementariosValues {
  return {
    dependenciaEconomica: datos?.dependenciaEconomica ?? '',
    ocupacionSosten: datos?.ocupacionSosten ?? '',
    rangoIngresos: datos?.rangoIngresos ?? '',
    tieneHijos: siNo(datos?.tieneHijos),
    cantidadHijos: datos?.cantidadHijos ?? '',
    grupoFamiliar: (datos?.grupoFamiliar ?? []).map((m) => ({
      nombre: m.nombre ?? '',
      parentesco: m.parentesco ?? '',
      edad: texto(m.edad),
      ocupacion: m.ocupacion ?? '',
      observacion: m.observacion ?? '',
    })),
    provincia: datos?.residencia?.provincia ?? '',
    zonaAnillo: datos?.residencia?.zonaAnillo ?? '',
    barrio: datos?.residencia?.barrio ?? '',
    calle: datos?.residencia?.calle ?? '',
    tenenciaVivienda: datos?.tenenciaVivienda ?? '',
    dormitorios: texto(datos?.infraestructura?.dormitorios),
    banos: texto(datos?.infraestructura?.banos),
    comedor: texto(datos?.infraestructura?.comedor),
    sala: texto(datos?.infraestructura?.sala),
    patio: texto(datos?.infraestructura?.patio),
    otroBeneficio: siNo(datos?.otroBeneficio),
    otroBeneficioDetalle: datos?.otroBeneficioDetalle ?? '',
    discapacidad: siNo(datos?.discapacidad),
    discapacidadDetalle: datos?.discapacidadDetalle ?? '',
    comentario: datos?.comentario ?? '',
  }
}

/** Las respuestas ya guardadas (reenvío de una devuelta) como texto del formulario; sin respuestas, todo vacío. */
export function valoresIniciales(respuesta: RespuestaSocioeconomica | null): RespuestasSocioeconomicasValues {
  return {
    ingresosFamiliares: texto(respuesta?.ingresosFamiliares),
    dependientes: texto(respuesta?.dependientes),
    procedencia: respuesta?.procedencia ?? '',
    patrimonio: respuesta?.patrimonio ?? '',
    estadoVulnerabilidad: respuesta?.estadoVulnerabilidad ?? '',
    promedioAcademico: texto(respuesta?.promedioAcademico),
    complementarios: complementariosIniciales(respuesta?.complementarios),
  }
}
