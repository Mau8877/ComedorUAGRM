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

  it('tiene la entrada «Finalistas» que lleva a su pantalla (CU21)', () => {
    expect(adminNavItems).toContainEqual(expect.objectContaining({ label: 'Finalistas', href: '/finalistas' }))
  })

  it('tiene la entrada «Auditoría y adjudicación» que lleva a su pantalla (CU05)', () => {
    expect(adminNavItems).toContainEqual(
      expect.objectContaining({ label: 'Auditoría y adjudicación', href: '/adjudicacion' }),
    )
  })

  it('tiene la entrada «Publicar resultados» que lleva a su pantalla (CU22)', () => {
    expect(adminNavItems).toContainEqual(
      expect.objectContaining({ label: 'Publicar resultados', href: '/publicar-resultados' }),
    )
  })

  it('tiene la entrada «Actas» que lleva a su pantalla, justo después de «Publicar resultados» (CU08)', () => {
    const etiquetas = adminNavItems.map((item) => item.label)

    expect(adminNavItems).toContainEqual(expect.objectContaining({ label: 'Actas', href: '/actas' }))
    expect(etiquetas.indexOf('Actas')).toBe(etiquetas.indexOf('Publicar resultados') + 1)
  })

  it('tiene la entrada «Convocatorias» que lleva a su pantalla (CU13)', () => {
    expect(adminNavItems).toContainEqual(expect.objectContaining({ label: 'Convocatorias', href: '/convocatorias' }))
  })

  it('tiene la entrada «Planillas» que lleva a su pantalla, justo después del padrón de becados (CU09)', () => {
    const etiquetas = adminNavItems.map((item) => item.label)

    expect(adminNavItems).toContainEqual(expect.objectContaining({ label: 'Planillas', href: '/planillas' }))
    expect(etiquetas.indexOf('Planillas')).toBe(etiquetas.indexOf('Padrón de becados') + 1)
  })

  it('tiene la entrada «Cotejo de asistencia» que lleva a su pantalla, justo después de «Planillas» (CU11)', () => {
    const etiquetas = adminNavItems.map((item) => item.label)

    expect(adminNavItems).toContainEqual(
      expect.objectContaining({ label: 'Cotejo de asistencia', href: '/cotejo-asistencia' }),
    )
    expect(etiquetas.indexOf('Cotejo de asistencia')).toBe(etiquetas.indexOf('Planillas') + 1)
  })

  it('tiene la entrada «Parámetros del modelo» que lleva a su pantalla, justo después de «Lista priorizada» (CU26)', () => {
    const etiquetas = adminNavItems.map((item) => item.label)

    expect(adminNavItems).toContainEqual(
      expect.objectContaining({ label: 'Parámetros del modelo', href: '/parametros-modelo' }),
    )
    expect(etiquetas.indexOf('Parámetros del modelo')).toBe(etiquetas.indexOf('Lista priorizada') + 1)
  })

  it('tiene la entrada «Gestiones académicas» que lleva a su pantalla (CU13)', () => {
    expect(adminNavItems).toContainEqual(
      expect.objectContaining({ label: 'Gestiones académicas', href: '/gestiones-academicas' }),
    )
  })
})
