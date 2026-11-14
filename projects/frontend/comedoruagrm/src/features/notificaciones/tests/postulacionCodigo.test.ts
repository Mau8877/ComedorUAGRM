import { describe, expect, it } from 'vitest'

import type { NotificacionApi } from '../api'
import { aNotificationItem } from '../api/useNotificaciones'

function aviso(extra: Partial<NotificacionApi> = {}): NotificacionApi {
  return { id: 'n-1', tipo: 'INFO', titulo: 'T', detalle: 'D', fecha: '2026-11-13T15:00:00Z', leida: false, ...extra }
}

describe('aNotificationItem: el código de la postulación del aviso (CU04)', () => {
  it('pasa el código de la postulación cuando el aviso lo trae', () => {
    expect(aNotificationItem(aviso({ postulacionCodigo: 'post-1' })).postulacionCodigo).toBe('post-1')
  })

  it('un aviso sin postulación (null o ausente) queda sin código', () => {
    expect(aNotificationItem(aviso({ postulacionCodigo: null })).postulacionCodigo).toBeUndefined()
    expect(aNotificationItem(aviso()).postulacionCodigo).toBeUndefined()
  })
})
