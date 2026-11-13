import { describe, expect, it, vi } from 'vitest'

const guardia = vi.hoisted(() => vi.fn())
vi.mock('@/features/auth', () => ({ exigirRol: (rol: string) => guardia(rol) }))
vi.mock('@/features/resultados', () => ({ PublicarResultadosScreen: () => null, ResultadosScreen: () => null }))

describe('ruta de resultados del postulante', () => {
  it('exige el rol Postulante: sin él la ruta no se muestra', async () => {
    guardia.mockReturnValue('guardia-postulante')

    const { Route } = await import('../../../routes/_authenticated/resultados')

    expect(guardia).toHaveBeenCalledWith('POSTULANTE')
    expect(Route.options.beforeLoad).toBe('guardia-postulante')
  })
})
