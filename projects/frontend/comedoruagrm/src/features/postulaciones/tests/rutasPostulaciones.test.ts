import { beforeEach, describe, expect, it, vi } from 'vitest'

const guardia = vi.hoisted(() => vi.fn())
vi.mock('@/features/auth', () => ({ exigirRol: (rol: string) => guardia(rol) }))
vi.mock('@/features/postulaciones', () => ({ MisPostulacionesScreen: () => null, DetallePostulacionScreen: () => null }))

describe('rutas de mis postulaciones', () => {
  beforeEach(() => {
    guardia.mockReset()
    guardia.mockReturnValue('guardia-postulante')
  })

  it('la lista exige el rol Postulante: sin él la ruta no se muestra', async () => {
    const { Route } = await import('../../../routes/_authenticated/mis-postulaciones')

    expect(guardia).toHaveBeenCalledWith('POSTULANTE')
    expect(Route.options.beforeLoad).toBe('guardia-postulante')
  })

  it('el detalle exige el rol Postulante', async () => {
    const { Route } = await import('../../../routes/_authenticated/mis-postulaciones_.$codigo')

    expect(guardia).toHaveBeenCalledWith('POSTULANTE')
    expect(Route.options.beforeLoad).toBe('guardia-postulante')
  })
})
