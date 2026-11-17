import { z } from 'zod'

// * Mismas reglas que el backend (ConvocatoriaRequest): el servidor vuelve a validar, esto es para dar feedback
// * inmediato campo por campo. El formulario trabaja con texto (lo que escribe la persona); `parse` devuelve el
// * cuerpo que se manda: los opcionales vacíos pasan a `null` y los números a número.
// * Cupos, puntaje mínimo y cantidad de finalistas son opcionales mientras la DUBSS no los defina.

const FECHA = /^\d{4}-\d{2}-\d{2}$/

const enteroPositivoOpcional = (mensaje: string) =>
  z
    .string()
    .trim()
    .refine((v) => v === '' || /^[1-9]\d*$/.test(v), mensaje)
    .transform((v) => (v === '' ? null : Number(v)))

const puntajeOpcional = z
  .string()
  .trim()
  .refine(
    (v) => v === '' || /^(100(\.0{1,2})?|\d{1,2}(\.\d{1,2})?)$/.test(v),
    'El puntaje mínimo debe estar entre 0 y 100, con hasta dos decimales',
  )
  .transform((v) => (v === '' ? null : Number(v)))

const documentoSchema = z.object({
  tipoDocumentoCodigo: z.string().min(1, 'Selecciona el tipo de documento'),
  obligatorio: z.boolean(),
})

export const convocatoriaSchema = z
  .object({
    nombre: z
      .string()
      .trim()
      .min(1, 'El nombre es obligatorio')
      .max(150, 'El nombre no puede superar los 150 caracteres'),
    gestionCodigo: z.string().min(1, 'La gestión académica es obligatoria'),
    fechaApertura: z.string().regex(FECHA, 'La fecha de apertura es obligatoria'),
    fechaCierre: z.string().regex(FECHA, 'La fecha de cierre es obligatoria'),
    cupos: enteroPositivoOpcional('Los cupos deben ser un número entero positivo'),
    puntajeMinimo: puntajeOpcional,
    cantidadFinalistas: enteroPositivoOpcional('La cantidad de finalistas debe ser un número entero positivo'),
    documentos: z.array(documentoSchema),
  })
  .superRefine((v, ctx) => {
    if (FECHA.test(v.fechaApertura) && FECHA.test(v.fechaCierre) && v.fechaCierre <= v.fechaApertura) {
      ctx.addIssue({
        code: 'custom',
        path: ['fechaCierre'],
        message: 'La fecha de cierre debe ser posterior a la de apertura',
      })
    }
    if (v.documentos.length === 0) {
      ctx.addIssue({ code: 'custom', path: ['documentos'], message: 'Indica al menos un documento requerido' })
      return
    }
    const tipos = v.documentos.map((d) => d.tipoDocumentoCodigo).filter(Boolean)
    if (new Set(tipos).size !== tipos.length) {
      ctx.addIssue({ code: 'custom', path: ['documentos'], message: 'Un tipo de documento no puede repetirse' })
    } else if (!v.documentos.some((d) => d.obligatorio)) {
      ctx.addIssue({ code: 'custom', path: ['documentos'], message: 'Indica al menos un documento obligatorio' })
    }
  })

/** Lo que escribe la persona en el formulario (texto). */
export type ConvocatoriaFormValues = z.input<typeof convocatoriaSchema>
/** El cuerpo que se manda al backend (opcionales vacíos como `null`). */
export type ConvocatoriaRequest = z.output<typeof convocatoriaSchema>
