import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../api/useSesion', () => ({ esperarSesionInicial: vi.fn() }))

import { esperarSesionInicial } from '../api/useSesion'
import { exigirSesion, redirigirSiHaySesion } from '../guards'

const usuario = { codigo: 'u1', nombres: 'Ana', apellidos: 'Perez', correo: 'ana@uagrm.edu.bo', rol: 'POSTULANTE' }

describe('exigirSesion', () => {
  beforeEach(() => vi.mocked(esperarSesionInicial).mockReset())

  it('sin sesión redirige a /login', async () => {
    vi.mocked(esperarSesionInicial).mockResolvedValue(null)

    await expect(exigirSesion()).rejects.toMatchObject({ options: { to: '/login' } })
  })

  it('con sesión no redirige y deja el usuario en el contexto', async () => {
    vi.mocked(esperarSesionInicial).mockResolvedValue(usuario)

    await expect(exigirSesion()).resolves.toEqual({ usuario })
  })
})

describe('redirigirSiHaySesion', () => {
  beforeEach(() => vi.mocked(esperarSesionInicial).mockReset())

  it('con sesión lleva al módulo del rol', async () => {
    vi.mocked(esperarSesionInicial).mockResolvedValue(usuario)

    await expect(redirigirSiHaySesion()).rejects.toMatchObject({ options: { to: '/panel' } })
  })

  it('sin sesión deja ver la pantalla', async () => {
    vi.mocked(esperarSesionInicial).mockResolvedValue(null)

    await expect(redirigirSiHaySesion()).resolves.toBeUndefined()
  })
})
