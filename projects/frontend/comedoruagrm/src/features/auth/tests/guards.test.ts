import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../api/useSesion', () => ({ esperarSesionInicial: vi.fn() }))

import { esperarSesionInicial } from '../api/useSesion'
import { exigirRol, exigirSesion, redirigirSiHaySesion } from '../guards'

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

describe('exigirRol', () => {
  beforeEach(() => vi.mocked(esperarSesionInicial).mockReset())

  it('deja pasar al rol pedido y lo deja en el contexto', async () => {
    const admin = { ...usuario, rol: 'ADMINISTRADOR_DUBSS' }
    vi.mocked(esperarSesionInicial).mockResolvedValue(admin)

    await expect(exigirRol('ADMINISTRADOR_DUBSS')()).resolves.toEqual({ usuario: admin })
  })

  it.each(['POSTULANTE', 'DIRECTIVO', 'PERSONAL_COMEDOR'])('redirige al módulo de su rol si es %s', async (rol) => {
    vi.mocked(esperarSesionInicial).mockResolvedValue({ ...usuario, rol })

    await expect(exigirRol('ADMINISTRADOR_DUBSS')()).rejects.toMatchObject({ options: { to: '/panel' } })
  })

  it('sin sesión redirige a /login', async () => {
    vi.mocked(esperarSesionInicial).mockResolvedValue(null)

    await expect(exigirRol('ADMINISTRADOR_DUBSS')()).rejects.toMatchObject({ options: { to: '/login' } })
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
