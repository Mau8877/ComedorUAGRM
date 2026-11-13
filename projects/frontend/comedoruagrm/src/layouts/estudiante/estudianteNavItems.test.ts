import { describe, expect, it } from 'vitest'

import { estudianteNavItems } from './estudianteNavItems'

describe('estudianteNavItems', () => {
  it('tiene la entrada «Resultados» que lleva a su pantalla (CU22)', () => {
    expect(estudianteNavItems).toContainEqual(expect.objectContaining({ label: 'Resultados', href: '/resultados' }))
  })

  it('conserva el resto de las entradas del postulante', () => {
    expect(estudianteNavItems.map((item) => item.label)).toEqual(
      expect.arrayContaining(['Mi Menú', 'Mis Pedidos', 'Perfil']),
    )
  })
})
