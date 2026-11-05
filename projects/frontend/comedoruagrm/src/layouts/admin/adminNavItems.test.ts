import { describe, expect, it } from 'vitest'

import { adminNavItems } from './adminNavItems'

describe('adminNavItems', () => {
  it('tiene las tres entradas de la gestión de usuarios con sus rutas', () => {
    const porEtiqueta = Object.fromEntries(adminNavItems.map((item) => [item.label, item.href]))

    expect(porEtiqueta['Administradores DUBSS']).toBe('/usuarios/administradores')
    expect(porEtiqueta['Directivos']).toBe('/usuarios/directivos')
    expect(porEtiqueta['Personal del Comedor']).toBe('/usuarios/personal-comedor')
  })

  it('ya no tiene la entrada genérica «Usuarios» y conserva el resto', () => {
    const etiquetas = adminNavItems.map((item) => item.label)

    expect(etiquetas).not.toContain('Usuarios')
    expect(etiquetas).toEqual(expect.arrayContaining(['Dashboard', 'Menús', 'Pedidos']))
  })
})
