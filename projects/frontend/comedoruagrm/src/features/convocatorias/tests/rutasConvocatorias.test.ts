import { describe, expect, it, vi } from 'vitest'

const guardia = vi.hoisted(() => vi.fn())
vi.mock('@/features/auth', () => ({ exigirRol: (rol: string) => guardia(rol) }))
vi.mock('@/features/convocatorias', () => ({ ConvocatoriasScreen: () => null, GestionesAcademicasScreen: () => null }))

describe('rutas del CU13', () => {
  it('/convocatorias exige el rol Administrador DUBSS: sin él la ruta no se muestra', async () => {
    guardia.mockReturnValue('guardia-admin')

    const { Route } = await import('../../../routes/_authenticated/convocatorias')

    expect(guardia).toHaveBeenCalledWith('ADMINISTRADOR_DUBSS')
    expect(Route.options.beforeLoad).toBe('guardia-admin')
  })

  it('/gestiones-academicas exige el rol Administrador DUBSS: sin él la ruta no se muestra', async () => {
    guardia.mockClear()
    guardia.mockReturnValue('guardia-admin')

    const { Route } = await import('../../../routes/_authenticated/gestiones-academicas')

    expect(guardia).toHaveBeenCalledWith('ADMINISTRADOR_DUBSS')
    expect(Route.options.beforeLoad).toBe('guardia-admin')
  })
})
