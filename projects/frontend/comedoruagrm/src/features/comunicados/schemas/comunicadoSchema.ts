import { z } from 'zod'

import type { DestinatariosComunicado, PublicarComunicadoRequest } from '../types'

// * Mismas reglas que el backend (PublicarComunicadoRequest): título de hasta 150 caracteres, contenido
// * de hasta 5000 y los destinatarios (`FINALISTAS` y `ADJUDICADOS` además necesitan una convocatoria).
export const comunicadoSchema = z.object({
  titulo: z
    .string()
    .trim()
    .min(1, 'El título es obligatorio')
    .max(150, 'El título no puede pasar de 150 caracteres'),
  contenido: z
    .string()
    .trim()
    .min(1, 'El contenido es obligatorio')
    .max(5000, 'El contenido no puede pasar de 5000 caracteres'),
  destinatarios: z.enum(['TODOS', 'FINALISTAS', 'ADJUDICADOS'], { error: 'Indica a quién va dirigido' }),
})

export type ComunicadoValues = z.infer<typeof comunicadoSchema>

export const ETIQUETAS_DESTINATARIOS: Record<DestinatariosComunicado, string> = {
  TODOS: 'Todos los postulantes',
  FINALISTAS: 'Finalistas',
  ADJUDICADOS: 'Adjudicados',
}

/** `FINALISTAS` y `ADJUDICADOS` se dirigen a una convocatoria: sin ella no se sabe a quién (ERR_COM_02). */
export function requiereConvocatoria(destinatarios: DestinatariosComunicado): boolean {
  return destinatarios === 'FINALISTAS' || destinatarios === 'ADJUDICADOS'
}

export function aPublicarRequest(valores: ComunicadoValues, convocatoriaId: string | undefined): PublicarComunicadoRequest {
  return {
    titulo: valores.titulo.trim(),
    contenido: valores.contenido.trim(),
    destinatarios: valores.destinatarios,
    ...(convocatoriaId ? { convocatoriaId } : {}),
  }
}
