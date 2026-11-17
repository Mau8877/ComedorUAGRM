import { z } from 'zod'

import type { RegistrarCotejoRequest, ResultadoCotejo } from '../types'

export const MAX_OBSERVACIONES = 2000

const ENTERO = /^\d+$/

/**
 * Las reglas del cotejo (CU11): las mismas que valida el backend (`ERR_COT_02`, `03` y `04`) para dar feedback
 * inmediato; el servidor vuelve a validar. Los campos son texto porque así los entrega `<input>`.
 *
 * * El Administrador declara el total de registros que comparó y cuántos coinciden; las discrepancias son la
 * * diferencia y el resultado se deduce de ellas (con discrepancias hay que dejar observaciones, A2). Así lo que se
 * * envía nunca es inconsistente.
 */
export const cotejoSchema = z
  .object({
    total: z.string().trim().min(1, 'El total de registros es obligatorio'),
    coincidencias: z.string().trim().min(1, 'Las coincidencias son obligatorias'),
    observaciones: z.string().max(MAX_OBSERVACIONES, `Máximo ${MAX_OBSERVACIONES} caracteres`),
  })
  .superRefine(({ total, coincidencias, observaciones }, ctx) => {
    const totalValido = ENTERO.test(total.trim())
    const coincidenciasValidas = ENTERO.test(coincidencias.trim())
    if (total.trim() && !totalValido) {
      ctx.addIssue({ code: 'custom', path: ['total'], message: 'Ingresa un número entero, 0 o mayor' })
    }
    if (coincidencias.trim() && !coincidenciasValidas) {
      ctx.addIssue({ code: 'custom', path: ['coincidencias'], message: 'Ingresa un número entero, 0 o mayor' })
    }
    if (!totalValido || !coincidenciasValidas) return
    if (Number(coincidencias) > Number(total)) {
      ctx.addIssue({
        code: 'custom',
        path: ['coincidencias'],
        message: 'Las coincidencias no pueden superar el total de registros',
      })
      return
    }
    if (Number(total) - Number(coincidencias) > 0 && observaciones.trim() === '') {
      ctx.addIssue({
        code: 'custom',
        path: ['observaciones'],
        message: 'Con discrepancias, las observaciones son obligatorias',
      })
    }
  })

export type CotejoFormulario = z.infer<typeof cotejoSchema>

/** Lo que se deduce de lo ingresado: las discrepancias y el resultado; `null` si todavía no hay números válidos. */
export function calcularCotejo(
  valores: Pick<CotejoFormulario, 'total' | 'coincidencias'>,
): { discrepancias: number; resultado: ResultadoCotejo } | null {
  if (!ENTERO.test(valores.total.trim()) || !ENTERO.test(valores.coincidencias.trim())) return null
  const discrepancias = Number(valores.total) - Number(valores.coincidencias)
  if (discrepancias < 0) return null
  return { discrepancias, resultado: discrepancias > 0 ? 'CON_DISCREPANCIAS' : 'CONFORME' }
}

/** El cuerpo de la petición a partir del formulario ya validado. */
export function crearSolicitudCotejo(valores: CotejoFormulario): RegistrarCotejoRequest {
  const calculado = calcularCotejo(valores)
  if (!calculado) throw new Error('El formulario del cotejo no es válido')
  const observaciones = valores.observaciones.trim()
  return {
    totalRegistros: Number(valores.total),
    coincidencias: Number(valores.coincidencias),
    discrepancias: calculado.discrepancias,
    resultado: calculado.resultado,
    ...(observaciones ? { observaciones } : {}),
  }
}
