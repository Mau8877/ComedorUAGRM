import { describe, expect, it } from 'vitest'

import type { PanelAdjudicacion } from '@/features/adjudicacion'

import { motivoBloqueoAdjudicacion } from '../components'
import { auditoria, expediente } from './fixtures'

function panel(extra: Partial<PanelAdjudicacion> = {}): PanelAdjudicacion {
  return {
    convocatoria: { codigo: 'c-1', nombre: 'Beca Comedor 2026', estado: 'CERRADA' },
    cupos: 10,
    puntajeMinimo: 60,
    adjudicados: 2,
    noAdjudicados: 1,
    cuposDisponibles: 8,
    cantidadFinalistas: 15,
    totalFinalistas: 12,
    calificadasSinSeleccionar: 30,
    resultadosPublicados: false,
    decisionesHabilitadas: true,
    ...extra,
  }
}

describe('motivoBloqueoAdjudicacion', () => {
  it('con todo en regla no hay motivo para ninguna de las dos acciones', () => {
    expect(motivoBloqueoAdjudicacion(expediente(), panel(), 'adjudicar')).toBeNull()
    expect(motivoBloqueoAdjudicacion(expediente(), panel(), 'no-adjudicar')).toBeNull()
  })

  it.each([
    ['adjudicada', 'La postulación ya está adjudicada.'],
    ['no_adjudicada', 'La postulación ya fue marcada como no adjudicada.'],
    ['archivada', 'La postulación ya está archivada.'],
    ['devuelta', 'La postulación ya no está calificada.'],
  ])('con la postulación %s lo explica, en las dos acciones', (estado, motivo) => {
    const e = expediente({ estadoPostulacion: estado })

    expect(motivoBloqueoAdjudicacion(e, panel(), 'adjudicar')).toBe(motivo)
    expect(motivoBloqueoAdjudicacion(e, panel(), 'no-adjudicar')).toBe(motivo)
  })

  it('mientras el panel no cargó espera: no asume que se puede', () => {
    expect(motivoBloqueoAdjudicacion(expediente(), undefined, 'adjudicar')).not.toBeNull()
    expect(motivoBloqueoAdjudicacion(expediente(), undefined, 'no-adjudicar')).not.toBeNull()
  })

  it('con los resultados publicados lo dice, en las dos acciones', () => {
    const p = panel({ resultadosPublicados: true, decisionesHabilitadas: false })

    expect(motivoBloqueoAdjudicacion(expediente(), p, 'adjudicar')).toBe('Los resultados ya se publicaron.')
    expect(motivoBloqueoAdjudicacion(expediente(), p, 'no-adjudicar')).toBe('Los resultados ya se publicaron.')
  })

  it('con las decisiones deshabilitadas (convocatoria no lista) lo dice, en las dos acciones', () => {
    const p = panel({ decisionesHabilitadas: false, convocatoria: { codigo: 'c-1', nombre: 'x', estado: 'ABIERTA' } })
    const motivo = 'La convocatoria debe estar cerrada y tener cupos y puntaje mínimo definidos.'

    expect(motivoBloqueoAdjudicacion(expediente(), p, 'adjudicar')).toBe(motivo)
    expect(motivoBloqueoAdjudicacion(expediente(), p, 'no-adjudicar')).toBe(motivo)
  })

  it('sin ninguna auditoría pide auditar primero, en las dos acciones', () => {
    const e = expediente({ auditorias: [], puntajeTotal: null, puntajeAcademico: null })

    expect(motivoBloqueoAdjudicacion(e, panel(), 'adjudicar')).toBe('Primero hay que auditar el expediente.')
    expect(motivoBloqueoAdjudicacion(e, panel(), 'no-adjudicar')).toBe('Primero hay que auditar el expediente.')
  })

  it.each(['OBSERVADO', 'NO_VALIDO'] as const)(
    'con la última auditoría «%s» no se adjudica pero sí se puede no adjudicar',
    (resultado) => {
      const e = expediente({ auditorias: [auditoria({ resultado, puntajeAcademico: null })] })

      expect(motivoBloqueoAdjudicacion(e, panel(), 'adjudicar')).toContain('La última auditoría no es “Validado”')
      expect(motivoBloqueoAdjudicacion(e, panel(), 'no-adjudicar')).toBeNull()
    },
  )

  it('con puntaje total menor al mínimo no se adjudica (con los dos valores) pero sí se puede no adjudicar', () => {
    const e = expediente({ puntajeTotal: 45.5 })

    expect(motivoBloqueoAdjudicacion(e, panel({ puntajeMinimo: 60 }), 'adjudicar')).toBe(
      'El puntaje total (45.50) no alcanza el mínimo (60.00).',
    )
    expect(motivoBloqueoAdjudicacion(e, panel({ puntajeMinimo: 60 }), 'no-adjudicar')).toBeNull()
  })

  it('con puntaje igual al mínimo se puede adjudicar', () => {
    expect(motivoBloqueoAdjudicacion(expediente({ puntajeTotal: 60 }), panel({ puntajeMinimo: 60 }), 'adjudicar')).toBeNull()
  })

  it('sin puntaje total no se adjudica y lo dice', () => {
    expect(motivoBloqueoAdjudicacion(expediente({ puntajeTotal: null }), panel(), 'adjudicar')).toBe(
      'El puntaje total (no disponible) no alcanza el mínimo (60.00).',
    )
  })

  it('con los cupos agotados no se adjudica pero sí se puede no adjudicar', () => {
    const p = panel({ cuposDisponibles: 0 })

    expect(motivoBloqueoAdjudicacion(expediente(), p, 'adjudicar')).toBe('Ya no quedan cupos disponibles.')
    expect(motivoBloqueoAdjudicacion(expediente(), p, 'no-adjudicar')).toBeNull()
  })

  it('respeta el orden: el estado de la postulación gana sobre todo lo demás', () => {
    const e = expediente({ estadoPostulacion: 'adjudicada', auditorias: [] })

    expect(motivoBloqueoAdjudicacion(e, panel({ resultadosPublicados: true }), 'adjudicar')).toBe(
      'La postulación ya está adjudicada.',
    )
  })
})
