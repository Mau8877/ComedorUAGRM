import { z } from 'zod'

// * Mismas reglas que el backend (GestionAcademicaRequest): el servidor vuelve a validar, esto es para dar
// * feedback inmediato campo por campo. Las fechas son `yyyy-MM-dd` (el valor de un <input type="date">), que se
// * comparan como texto porque ese formato ordena igual que las fechas.

const FECHA = /^\d{4}-\d{2}-\d{2}$/

export const gestionSchema = z
  .object({
    etiqueta: z
      .string()
      .trim()
      .min(1, 'La etiqueta es obligatoria')
      .max(50, 'La etiqueta no puede superar los 50 caracteres'),
    fechaInicio: z.string().regex(FECHA, 'La fecha de inicio es obligatoria'),
    fechaFin: z.string().regex(FECHA, 'La fecha de fin es obligatoria'),
  })
  .refine((v) => !FECHA.test(v.fechaInicio) || !FECHA.test(v.fechaFin) || v.fechaFin > v.fechaInicio, {
    message: 'La fecha de fin debe ser posterior a la de inicio',
    path: ['fechaFin'],
  })

export type GestionRequest = z.infer<typeof gestionSchema>
