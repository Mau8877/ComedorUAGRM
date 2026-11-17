import { z } from 'zod'

import type { ResumenPlanilla } from '../types'

/** Días que caben en la hoja de la planilla (una columna de firma por día): el backend rechaza más (ERR_PLA_03). */
export const MAX_DIAS_PLANILLA = 31

const DIA_MS = 24 * 60 * 60 * 1000

/** Cantidad de días del periodo, ambas fechas incluidas (`AAAA-MM-DD`). */
function diasDelPeriodo(desde: string, hasta: string): number {
  return Math.round((Date.parse(`${hasta}T00:00:00Z`) - Date.parse(`${desde}T00:00:00Z`)) / DIA_MS) + 1
}

/**
 * Las reglas del periodo de una planilla (CU09, A1): las mismas que valida el backend (ERR_PLA_01, 02 y 03), para dar
 * feedback inmediato; el servidor vuelve a validar. Las fechas son `AAAA-MM-DD` (así las entrega `<input type="date">`)
 * y el periodo tiene que caer dentro de la gestión académica de la convocatoria.
 *
 * * Es una fábrica porque los límites salen del resumen de la convocatoria elegida. Sin resumen todavía solo se
 * * exige que haya fechas y que la final no sea anterior a la inicial.
 */
export function crearPeriodoSchema(gestion?: Pick<ResumenPlanilla, 'gestion' | 'gestionDesde' | 'gestionHasta'>) {
  return z
    .object({
      desde: z.string().min(1, 'La fecha inicial es obligatoria'),
      hasta: z.string().min(1, 'La fecha final es obligatoria'),
    })
    .superRefine(({ desde, hasta }, ctx) => {
      if (!desde || !hasta) return
      if (hasta < desde) {
        ctx.addIssue({ code: 'custom', path: ['hasta'], message: 'La fecha final no puede ser anterior a la inicial' })
        return
      }
      if (gestion && (desde < gestion.gestionDesde || hasta > gestion.gestionHasta)) {
        const campo = desde < gestion.gestionDesde ? 'desde' : 'hasta'
        ctx.addIssue({
          code: 'custom',
          path: [campo],
          message: `El periodo debe estar dentro de la gestión ${gestion.gestion}`,
        })
        return
      }
      if (diasDelPeriodo(desde, hasta) > MAX_DIAS_PLANILLA) {
        ctx.addIssue({
          code: 'custom',
          path: ['hasta'],
          message: `El periodo no puede superar los ${MAX_DIAS_PLANILLA} días`,
        })
      }
    })
}

export type PeriodoFormulario = z.infer<ReturnType<typeof crearPeriodoSchema>>
