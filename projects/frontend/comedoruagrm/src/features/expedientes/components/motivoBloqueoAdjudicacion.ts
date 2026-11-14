import type { PanelAdjudicacion } from '@/features/adjudicacion'

import type { AccionDecision, Expediente } from '../types'

/**
 * Por qué no se puede adjudicar ni no adjudicar todavía, o `null` si se puede intentar (CU05). Los motivos van en
 * este orden: estado de la postulación, convocatoria o resultados, auditoría y, solo al adjudicar, resultado de
 * la última auditoría, puntaje y cupos. «No adjudicar» sigue habilitado con cupos agotados o puntaje bajo.
 * Si el panel aún no cargó, devuelve un motivo: no se asume que se puede. El backend sigue decidiendo de verdad.
 */
export function motivoBloqueoAdjudicacion(
  expediente: Expediente,
  panel: PanelAdjudicacion | undefined,
  accion: AccionDecision,
): string | null {
  switch (expediente.estadoPostulacion) {
    case 'calificada':
      break
    case 'adjudicada':
      return 'La postulación ya está adjudicada.'
    case 'no_adjudicada':
      return 'La postulación ya fue marcada como no adjudicada.'
    case 'archivada':
      return 'La postulación ya está archivada.'
    default:
      return 'La postulación ya no está calificada.'
  }

  if (!panel) return 'Cargando los cupos de la convocatoria…'
  if (panel.resultadosPublicados) return 'Los resultados ya se publicaron.'
  if (!panel.decisionesHabilitadas) {
    return 'La convocatoria debe estar cerrada y tener cupos y puntaje mínimo definidos.'
  }

  const ultima = expediente.auditorias[0]
  if (!ultima) return 'Primero hay que auditar el expediente.'

  if (accion === 'adjudicar') {
    if (ultima.resultado !== 'VALIDADO') {
      return 'La última auditoría no es “Validado”: audite de nuevo o use “No adjudicar”.'
    }
    if (
      panel.puntajeMinimo !== null &&
      (expediente.puntajeTotal === null || expediente.puntajeTotal < panel.puntajeMinimo)
    ) {
      const total = expediente.puntajeTotal === null ? 'no disponible' : expediente.puntajeTotal.toFixed(2)
      return `El puntaje total (${total}) no alcanza el mínimo (${panel.puntajeMinimo.toFixed(2)}).`
    }
    if (panel.cuposDisponibles !== null && panel.cuposDisponibles <= 0) return 'Ya no quedan cupos disponibles.'
  }
  return null
}
