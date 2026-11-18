import { describe, expect, it } from 'vitest'

import { estudianteNavItems } from './estudianteNavItems'

describe('estudianteNavItems', () => {
  it('tiene la entrada «Resultados» que lleva a su pantalla (CU22)', () => {
    expect(estudianteNavItems).toContainEqual(expect.objectContaining({ label: 'Resultados', href: '/resultados' }))
  })

  it('tiene la entrada «Mis postulaciones» que lleva a su pantalla (CU04)', () => {
    expect(estudianteNavItems).toContainEqual(
      expect.objectContaining({ label: 'Mis postulaciones', href: '/mis-postulaciones' }),
    )
  })

  it('tiene la entrada «Mi beca» que lleva a su pantalla (CU24)', () => {
    expect(estudianteNavItems).toContainEqual(expect.objectContaining({ label: 'Mi beca', href: '/mi-beca' }))
  })

  it('conserva «Perfil» y ya no tiene «Mi Menú» ni «Mis Pedidos» (sin pantalla)', () => {
    const etiquetas = estudianteNavItems.map((item) => item.label)

    expect(etiquetas).toContain('Perfil')
    expect(etiquetas).not.toContain('Mi Menú')
    expect(etiquetas).not.toContain('Mis Pedidos')
  })

  it('agrupa las entradas según el recorrido del postulante, cada grupo en un tramo seguido', () => {
    const grupos = estudianteNavItems.map((item) => item.grupo)
    const tramos = grupos.filter((g, i) => g !== grupos[i - 1])

    expect(tramos).toEqual(['Postulación', 'Beca', 'Cuenta'])
    expect(estudianteNavItems.map((item) => item.label)).toEqual([
      'Mis postulaciones',
      'Resultados',
      'Mi beca',
      'Perfil',
    ])
  })
})
