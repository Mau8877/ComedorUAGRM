import { z } from 'zod'

import type { DatosComplementarios, GuardarBorradorRequest } from '../types'

// * Mismas reglas que el backend (EnvioPostulacionService): el servidor vuelve a validar, esto es para dar feedback
// * inmediato campo por campo. El formulario trabaja con texto (lo que escribe la persona); `parse` devuelve el
// * cuerpo que se manda, con los números ya convertidos. Las opciones de las listas las valida el backend.
// ? El máximo y la escala del promedio y los ingresos están por definir con la DUBSS (hoy 0 a 100 y 2 decimales).
// ? Qué datos del formulario oficial son obligatorios también está por definir con la DUBSS.

const OBLIGATORIO = 'Este campo es obligatorio.'
const ELEGIR = 'Selecciona una opción.'

export const GRUPO_FAMILIAR_MAXIMO = 10
const EDAD_MAXIMA = 120
const AMBIENTES_MAXIMO = 20
const TEXTO_CORTO_MAXIMO = 100
const TEXTO_DETALLE_MAXIMO = 150
const COMENTARIO_MAXIMO = 500

const aNumero = (texto: string) => Number(texto.replace(',', '.'))

const ingresos = z
  .string()
  .trim()
  .min(1, OBLIGATORIO)
  .regex(/^\d{1,10}([.,]\d{1,2})?$/, 'Ingresa un monto válido, con hasta 2 decimales.')
  .transform(aNumero)

const dependientes = z
  .string()
  .trim()
  .min(1, OBLIGATORIO)
  .regex(/^\d{1,2}$/, 'Ingresa un número entero.')
  .refine((v) => Number(v) <= 50, 'El número de dependientes no puede superar 50.')
  .transform(Number)

const promedio = z
  .string()
  .trim()
  .min(1, OBLIGATORIO)
  .regex(/^(100([.,]0{1,2})?|\d{1,2}([.,]\d{1,2})?)$/, 'El promedio debe estar entre 0 y 100, con hasta 2 decimales.')
  .transform(aNumero)

const opcion = z.string().min(1, ELEGIR)

const campos = {
  ingresosFamiliares: ingresos,
  dependientes,
  procedencia: opcion,
  patrimonio: opcion,
  estadoVulnerabilidad: opcion,
  promedioAcademico: promedio,
}

// ---- Datos del formulario oficial de la DUBSS (FORM-DUBS-001) ----

const vacioANulo = (v: string) => (v === '' ? null : v)

/** Un texto libre y opcional de hasta `max` caracteres; vacío = `null`. */
const textoOpcional = (max: number, que: string) =>
  z.string().trim().max(max, `${que} no puede superar ${max} caracteres.`).transform(vacioANulo)

/** Una cantidad entera opcional (edad, ambientes de la vivienda); vacía = `null`. */
const enteroOpcional = (max: number, que: string) =>
  z
    .string()
    .trim()
    .refine((v) => v === '' || (/^\d{1,3}$/.test(v) && Number(v) <= max), `${que} debe ser un entero entre 0 y ${max}.`)
    .transform((v) => (v === '' ? null : Number(v)))

/** «sí»/«no» marcado en el formulario; obligatorio al enviar. */
const siNo = z
  .string()
  .refine((v) => v === 'si' || v === 'no', ELEGIR)
  .transform((v) => v === 'si')

/** «sí»/«no» del borrador: sin marcar es `null`, no un error. */
const siNoBorrador = z
  .string()
  .refine((v) => v === '' || v === 'si' || v === 'no', ELEGIR)
  .transform((v) => (v === '' ? null : v === 'si'))

const miembroGrupoFamiliar = z.object({
  nombre: textoOpcional(TEXTO_DETALLE_MAXIMO, 'El nombre'),
  parentesco: textoOpcional(TEXTO_CORTO_MAXIMO, 'El parentesco'),
  edad: enteroOpcional(EDAD_MAXIMA, 'La edad'),
  ocupacion: textoOpcional(TEXTO_CORTO_MAXIMO, 'La ocupación'),
  observacion: textoOpcional(TEXTO_DETALLE_MAXIMO, 'La observación'),
})

/** Lo que tiene en común el formulario completo y el borrador: todo lo que no es obligatorio. */
const comunes = {
  grupoFamiliar: z
    .array(miembroGrupoFamiliar)
    .max(GRUPO_FAMILIAR_MAXIMO, `El grupo familiar admite hasta ${GRUPO_FAMILIAR_MAXIMO} personas.`),
  provincia: textoOpcional(TEXTO_CORTO_MAXIMO, 'La provincia'),
  zonaAnillo: textoOpcional(TEXTO_CORTO_MAXIMO, 'La zona o anillo'),
  barrio: textoOpcional(TEXTO_CORTO_MAXIMO, 'El barrio'),
  calle: textoOpcional(TEXTO_CORTO_MAXIMO, 'La calle'),
  dormitorios: enteroOpcional(AMBIENTES_MAXIMO, 'La cantidad'),
  banos: enteroOpcional(AMBIENTES_MAXIMO, 'La cantidad'),
  comedor: enteroOpcional(AMBIENTES_MAXIMO, 'La cantidad'),
  sala: enteroOpcional(AMBIENTES_MAXIMO, 'La cantidad'),
  patio: enteroOpcional(AMBIENTES_MAXIMO, 'La cantidad'),
  otroBeneficioDetalle: textoOpcional(TEXTO_DETALLE_MAXIMO, 'El detalle'),
  discapacidadDetalle: textoOpcional(TEXTO_DETALLE_MAXIMO, 'El tipo y grado'),
  comentario: textoOpcional(COMENTARIO_MAXIMO, 'El comentario'),
}

const complementariosCompletoSchema = z
  .object({
    ...comunes,
    dependenciaEconomica: opcion,
    ocupacionSosten: opcion,
    rangoIngresos: opcion,
    tieneHijos: siNo,
    cantidadHijos: z.string().transform(vacioANulo),
    tenenciaVivienda: opcion,
    otroBeneficio: siNo,
    discapacidad: siNo,
  })
  .check((ctx) => {
    // * «¿Cuántos?» solo se pide si marcó que tiene hijos (el formulario oficial lo deja en blanco si no).
    if (ctx.value.tieneHijos && ctx.value.cantidadHijos === null) {
      ctx.issues.push({ code: 'custom', message: ELEGIR, path: ['cantidadHijos'], input: ctx.value })
    }
  })

const complementariosBorradorSchema = z.object({
  ...comunes,
  dependenciaEconomica: z.string().transform(vacioANulo),
  ocupacionSosten: z.string().transform(vacioANulo),
  rangoIngresos: z.string().transform(vacioANulo),
  tieneHijos: siNoBorrador,
  cantidadHijos: z.string().transform(vacioANulo),
  tenenciaVivienda: z.string().transform(vacioANulo),
  otroBeneficio: siNoBorrador,
  discapacidad: siNoBorrador,
})

export const respuestasSocioeconomicasSchema = z.object({
  ...campos,
  complementarios: complementariosCompletoSchema,
})

/** Lo que el formulario guarda mientras se escribe: todo texto. */
export type RespuestasSocioeconomicasValues = z.input<typeof respuestasSocioeconomicasSchema>
/** Los datos del formulario oficial como texto (los «sí/no» son `'si'`, `'no'` o vacío). */
export type ComplementariosValues = RespuestasSocioeconomicasValues['complementarios']
export type MiembroGrupoFamiliarValues = ComplementariosValues['grupoFamiliar'][number]

/** El cuerpo que se envía: números ya convertidos y los datos del formulario oficial con la forma del backend. */
export type RespuestasSocioeconomicasRequest = NonNullable<ReturnType<typeof aRequest>>

type ComplementariosParseados = z.output<typeof complementariosBorradorSchema>

/** Da a los datos ya validados la forma que espera el backend (`DatosComplementarios`). */
function aDatosComplementarios(c: ComplementariosParseados): DatosComplementarios {
  return {
    dependenciaEconomica: c.dependenciaEconomica,
    ocupacionSosten: c.ocupacionSosten,
    rangoIngresos: c.rangoIngresos,
    tieneHijos: c.tieneHijos,
    cantidadHijos: c.cantidadHijos,
    grupoFamiliar: c.grupoFamiliar.length > 0 ? c.grupoFamiliar : null,
    residencia: { provincia: c.provincia, zonaAnillo: c.zonaAnillo, barrio: c.barrio, calle: c.calle },
    tenenciaVivienda: c.tenenciaVivienda,
    infraestructura: { dormitorios: c.dormitorios, banos: c.banos, comedor: c.comedor, sala: c.sala, patio: c.patio },
    otroBeneficio: c.otroBeneficio,
    otroBeneficioDetalle: c.otroBeneficioDetalle,
    discapacidad: c.discapacidad,
    discapacidadDetalle: c.discapacidadDetalle,
    comentario: c.comentario,
  }
}

/** Valida el formulario completo y devuelve el cuerpo de `POST /enviar`; `null` si algo no es válido. */
export function aRequest(valores: RespuestasSocioeconomicasValues) {
  const resultado = respuestasSocioeconomicasSchema.safeParse(valores)
  if (!resultado.success) return null
  const { complementarios, ...columnas } = resultado.data
  return { ...columnas, complementarios: aDatosComplementarios(complementarios) }
}

// ---- Borrador (CU-17) ----

type Campo = keyof typeof campos

/** Los nombres de los campos del formulario que van en columna, en el orden en que se muestran. */
export const CAMPOS_SOCIOECONOMICOS = Object.keys(campos) as Campo[]

/** Clave de los errores del bloque del formulario oficial dentro de `ResultadoBorrador`. */
export const CLAVE_COMPLEMENTARIOS = 'complementarios'

export type ResultadoBorrador =
  | { ok: true; borrador: GuardarBorradorRequest }
  | { ok: false; errores: Partial<Record<Campo | typeof CLAVE_COMPLEMENTARIOS, string>> }

/**
 * Convierte lo que hay en el formulario en el cuerpo del borrador: un campo vacío se guarda vacío (`null`, no es
 * un error) y uno con texto debe cumplir la misma regla que al enviar. Si alguno no la cumple no se guarda nada
 * (el backend rechaza igual con `ERR_ENV_05`).
 */
export function aBorrador(valores: RespuestasSocioeconomicasValues): ResultadoBorrador {
  const borrador: Record<string, unknown> = {}
  const errores: Partial<Record<Campo | typeof CLAVE_COMPLEMENTARIOS, string>> = {}
  for (const campo of CAMPOS_SOCIOECONOMICOS) {
    const texto = valores[campo].trim()
    if (texto === '') {
      borrador[campo] = null
      continue
    }
    const resultado = campos[campo].safeParse(texto)
    if (resultado.success) borrador[campo] = resultado.data
    else errores[campo] = resultado.error.issues[0]?.message ?? 'El valor no es válido.'
  }
  const complementarios = complementariosBorradorSchema.safeParse(valores.complementarios)
  if (complementarios.success) borrador.complementarios = aDatosComplementarios(complementarios.data)
  else errores[CLAVE_COMPLEMENTARIOS] = complementarios.error.issues[0]?.message ?? 'El valor no es válido.'
  if (Object.keys(errores).length > 0) return { ok: false, errores }
  return { ok: true, borrador: borrador as unknown as GuardarBorradorRequest }
}
