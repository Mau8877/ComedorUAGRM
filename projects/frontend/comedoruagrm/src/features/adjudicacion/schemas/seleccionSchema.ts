import { z } from 'zod'

import type { SeleccionarFinalistasRequest } from '../types'

// * Mismas reglas que el backend (SeleccionarFinalistasRequest): la cantidad es un entero positivo. En el
// * formulario se escribe como texto; el campo vacío significa «sin cantidad» (el backend usa la configurada
// * en la convocatoria) y por eso el schema base lo admite. Si la convocatoria no tiene una configurada, el
// * diálogo usa `seleccionSchemaRequerido`, que exige escribirla.

const MENSAJE = 'La cantidad debe ser un número entero positivo'
const ENTERO = /^\d+$/

/** Convierte el texto del campo a número; `NaN` si no es un entero positivo (cero incluido). */
export function leerCantidad(texto: string): number {
  const limpio = texto.trim()
  if (!ENTERO.test(limpio)) return Number.NaN
  const numero = Number(limpio)
  return numero > 0 ? numero : Number.NaN
}

const esValida = (texto: string) => texto.trim() === '' || !Number.isNaN(leerCantidad(texto))

export const seleccionSchema = z.object({
  cantidad: z.string().refine(esValida, { message: MENSAJE }),
})

export const seleccionSchemaRequerido = z.object({
  cantidad: z
    .string()
    .refine((texto) => texto.trim() !== '', { message: 'Indica cuántos finalistas seleccionar' })
    .refine(esValida, { message: MENSAJE }),
})

export type SeleccionValues = z.infer<typeof seleccionSchema>

/** El pedido al backend: sin `cantidad` si el campo quedó vacío. */
export function aSeleccionRequest(valores: SeleccionValues): SeleccionarFinalistasRequest {
  return valores.cantidad.trim() === '' ? {} : { cantidad: leerCantidad(valores.cantidad) }
}
