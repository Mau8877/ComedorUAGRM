import type { PanelAdjudicacion } from '../types'

/** Por qué todavía no se puede seleccionar ni decidir; lista vacía si `decisionesHabilitadas`. */
export function avisosDelPanel(panel: PanelAdjudicacion): string[] {
  if (panel.decisionesHabilitadas) return []
  const avisos: string[] = []
  if (panel.resultadosPublicados) avisos.push('Los resultados ya se publicaron.')
  if (panel.convocatoria.estado !== 'CERRADA') {
    avisos.push('La convocatoria debe estar cerrada para seleccionar finalistas y decidir.')
  }
  if (panel.cupos === null || panel.puntajeMinimo === null) {
    avisos.push('Faltan cupos o puntaje mínimo por definir con la DUBSS.')
  }
  return avisos
}
