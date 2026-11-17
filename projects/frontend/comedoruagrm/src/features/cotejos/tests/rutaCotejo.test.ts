import { describe, expect, it, vi } from 'vitest'

const guardia = vi.hoisted(() => vi.fn())
vi.mock('@/features/auth', () => ({ exigirRol: (rol: string) => guardia(rol) }))
vi.mock('@/features/cotejos', () => ({ CotejoScreen: () => null }))

describe('ruta de cotejo de asistencia', () => {
  it('exige el rol Administrador DUBSS: sin él la ruta no se muestra', async () => {
    guardia.mockReturnValue('guardia-admin')

    const { Route } = await import('../../../routes/_authenticated/cotejo-asistencia')

    expect(guardia).toHaveBeenCalledWith('ADMINISTRADOR_DUBSS')
    expect(Route.options.beforeLoad).toBe('guardia-admin')
  })
})
