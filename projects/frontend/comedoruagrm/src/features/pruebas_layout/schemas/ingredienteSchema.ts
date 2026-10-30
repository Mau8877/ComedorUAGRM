import { z } from 'zod'
import type { CategoriaIngrediente, UnidadIngrediente } from '../types'

export const CATEGORIAS_INGREDIENTE = [
  'Verdura',
  'Fruta',
  'Lácteo',
  'Carne',
  'Grano',
  'Condimento',
] as const satisfies readonly CategoriaIngrediente[]

export const UNIDADES_INGREDIENTE = [
  'kg',
  'L',
  'unidad',
  'docena',
] as const satisfies readonly UnidadIngrediente[]

// * Los campos numéricos se validan como string (no z.coerce.number()) para
// * que coincidan 1:1 con lo que un <Input type="number"> guarda en el
// * estado del form -- la conversión a number pasa recién al armar el
// * payload en el submit, no acá.
function numeroNoNegativo(mensaje: string) {
  return z
    .string()
    .min(1, mensaje)
    .refine((valor) => !Number.isNaN(Number(valor)) && Number(valor) >= 0, mensaje)
}

export const ingredienteSchema = z.object({
  nombre: z.string().min(1, 'El nombre es obligatorio'),
  foto: z.string().min(1, 'La foto es obligatoria').url('Debe ser una URL válida'),
  categoria: z.enum(CATEGORIAS_INGREDIENTE, 'Seleccioná una categoría'),
  unidad: z.enum(UNIDADES_INGREDIENTE, 'Seleccioná una unidad'),
  stock: numeroNoNegativo('El stock no puede ser negativo'),
  stockMinimo: numeroNoNegativo('El stock mínimo no puede ser negativo'),
  precioUnitario: z
    .string()
    .min(1, 'El precio es obligatorio')
    .refine((valor) => !Number.isNaN(Number(valor)) && Number(valor) > 0, 'El precio debe ser mayor a 0'),
})

export type IngredienteFormValues = z.infer<typeof ingredienteSchema>
