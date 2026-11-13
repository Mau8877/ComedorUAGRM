import type { ResumenPublicacion } from '../types'

/** Por qué no se puede publicar, o `null` si se puede. El backend sigue siendo quien decide de verdad. */
export function motivoDeBloqueo(resumen: ResumenPublicacion): string | null {
  if (resumen.puedePublicar) return null
  if (resumen.yaPublicada) return 'Los resultados de esta convocatoria ya fueron publicados.'
  if (resumen.convocatoria.estado !== 'CERRADA') return 'La convocatoria debe estar cerrada para publicar sus resultados.'
  if (resumen.calificadasPendientes > 0) {
    return resumen.calificadasPendientes === 1
      ? 'Falta 1 decisión de adjudicación para poder publicar.'
      : `Faltan ${resumen.calificadasPendientes} decisiones de adjudicación para poder publicar.`
  }
  return 'No se pueden publicar los resultados de esta convocatoria todavía.'
}
