import { z } from 'zod'

import type { ParametroModelo } from '../types'

// * Mismas reglas que el backend (ModeloPreseleccionService.validar): diez variables, cada ponderación entre 0 y 100
// * con hasta 2 decimales, y todas suman 100. El servidor vuelve a validar (ERR_MOD_01): esto es feedback inmediato.
// * Las variables y su orden son los de VariablesModelo del backend: las seis del mockup del CU26 y las cuatro del
// * formulario oficial de la DUBSS; qué variables puntúan y con cuánto peso lo define la DUBSS.
export const VARIABLES_MODELO = [
  { clave: 'ingresos_familiares', nombre: 'Ingresos familiares' },
  { clave: 'dependientes', nombre: 'Dependientes' },
  { clave: 'procedencia', nombre: 'Procedencia' },
  { clave: 'patrimonio', nombre: 'Patrimonio' },
  { clave: 'estado_vulnerabilidad', nombre: 'Estado de vulnerabilidad' },
  { clave: 'promedio_academico', nombre: 'Promedio académico' },
  { clave: 'dependencia_economica', nombre: 'Dependencia económica' },
  { clave: 'ocupacion_sosten', nombre: 'Ocupación de quien sostiene' },
  { clave: 'discapacidad', nombre: 'Discapacidad' },
  { clave: 'hijos', nombre: 'Hijos' },
] as const

/** El nombre para mostrar de una variable; si la clave no se conoce, la misma clave. */
export function nombreVariable(clave: string): string {
  return VARIABLES_MODELO.find((v) => v.clave === clave)?.nombre ?? clave
}

export type ClaveVariable = (typeof VARIABLES_MODELO)[number]['clave']

/** Lo que deben sumar las ponderaciones. */
export const SUMA_PONDERACIONES = 100

const FORMATO_PONDERACION = /^\d{1,3}(?:[.,]\d{1,2})?$/

/** `28`, `28.5` o `28,5` → número; lo que no es un número da `NaN`. */
export function aNumero(valor: string): number {
  return Number(valor.trim().replace(',', '.'))
}

function esPonderacionValida(valor: string): boolean {
  return FORMATO_PONDERACION.test(valor) && aNumero(valor) <= SUMA_PONDERACIONES
}

const ponderacion = z
  .string()
  .trim()
  .min(1, 'Obligatorio')
  .refine(esPonderacionValida, 'Un número de 0 a 100 con hasta 2 decimales')

export const parametrosSchema = z.object({
  descripcion: z.string().trim().max(255, 'La descripción no puede superar los 255 caracteres'),
  ingresos_familiares: ponderacion,
  dependientes: ponderacion,
  procedencia: ponderacion,
  patrimonio: ponderacion,
  estado_vulnerabilidad: ponderacion,
  promedio_academico: ponderacion,
  dependencia_economica: ponderacion,
  ocupacion_sosten: ponderacion,
  discapacidad: ponderacion,
  hijos: ponderacion,
})

/** Lo que escribe el formulario: cada ponderación como texto, tal cual se teclea. */
export type ParametrosFormValues = z.input<typeof parametrosSchema>

/** El formulario con las ponderaciones de una versión; sin versión (todavía no se guardó ninguna), vacío. */
export function valoresIniciales(parametros: ParametroModelo[] | undefined): ParametrosFormValues {
  const porClave = new Map((parametros ?? []).map((p) => [p.clave, String(p.ponderacion)]))
  return {
    descripcion: '',
    ingresos_familiares: porClave.get('ingresos_familiares') ?? '',
    dependientes: porClave.get('dependientes') ?? '',
    procedencia: porClave.get('procedencia') ?? '',
    patrimonio: porClave.get('patrimonio') ?? '',
    estado_vulnerabilidad: porClave.get('estado_vulnerabilidad') ?? '',
    promedio_academico: porClave.get('promedio_academico') ?? '',
    dependencia_economica: porClave.get('dependencia_economica') ?? '',
    ocupacion_sosten: porClave.get('ocupacion_sosten') ?? '',
    discapacidad: porClave.get('discapacidad') ?? '',
    hijos: porClave.get('hijos') ?? '',
  }
}

/**
 * La suma de las ponderaciones escritas. Lo que todavía no es un número válido cuenta como 0, así el indicador se
 * mueve mientras se teclea. Se suma en centésimas para no arrastrar errores de punto flotante (0.1 + 0.2).
 */
export function sumaPonderaciones(valores: ParametrosFormValues): number {
  const centesimas = VARIABLES_MODELO.reduce((total, { clave }) => {
    const texto = valores[clave]
    return total + (esPonderacionValida(texto) ? Math.round(aNumero(texto) * 100) : 0)
  }, 0)
  return centesimas / 100
}

export function sumaEsValida(valores: ParametrosFormValues): boolean {
  return sumaPonderaciones(valores) === SUMA_PONDERACIONES
}

/** El cuerpo de `POST /modelos-preseleccion`. */
export function aPayload(valores: ParametrosFormValues) {
  const descripcion = valores.descripcion.trim()
  return {
    ...(descripcion ? { descripcion } : {}),
    parametros: VARIABLES_MODELO.map(({ clave }) => ({ clave, ponderacion: aNumero(valores[clave]) })),
  }
}

/** La versión que se creará al guardar: `v1.3` → `v1.4`; sin versión vigente, `v1.0`. Igual que el backend. */
export function siguienteVersion(vigente: string | null | undefined): string {
  const partes = vigente ? /^v(\d+)(?:\.(\d+))?$/.exec(vigente.trim()) : null
  if (!partes) return 'v1.0'
  return `v${partes[1]}.${Number(partes[2] ?? 0) + 1}`
}
