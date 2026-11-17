import { UsersIcon } from 'lucide-react'
import { describe, expect, it } from 'vitest'

import { migasPorDefecto } from './migasPorDefecto'

const navItems = [
  { label: 'Finalistas', href: '/finalistas', icon: UsersIcon },
  { label: 'Mis postulaciones', href: '/mis-postulaciones', icon: UsersIcon },
]

describe('migasPorDefecto', () => {
  it('la página principal de un ítem lleva solo su título, aunque difiera del nombre del menú', () => {
    expect(
      migasPorDefecto({
        title: 'Finalistas: notificación y documentación física',
        navItems,
        activeHref: '/finalistas',
        pathname: '/finalistas',
      }),
    ).toEqual([{ label: 'Finalistas: notificación y documentación física' }])
  })

  it('una página que cuelga del ítem lleva antes el ítem, enlazado', () => {
    expect(
      migasPorDefecto({ title: 'Auditar expediente', navItems, activeHref: '/finalistas', pathname: '/finalistas/abc' }),
    ).toEqual([{ label: 'Finalistas', href: '/finalistas' }, { label: 'Auditar expediente' }])
  })

  it('una página que no está bajo la ruta del ítem también cuelga de él (postular)', () => {
    expect(
      migasPorDefecto({ title: 'Nueva postulación', navItems, activeHref: '/mis-postulaciones', pathname: '/postular' }),
    ).toEqual([{ label: 'Mis postulaciones', href: '/mis-postulaciones' }, { label: 'Nueva postulación' }])
  })

  it('sin ítem activo en el menú queda solo el título', () => {
    expect(migasPorDefecto({ title: 'Panel', navItems, activeHref: undefined, pathname: '/panel' })).toEqual([
      { label: 'Panel' },
    ])
  })

  it('sin ruta conocida (tests, render del servidor) no inventa un tramo de más', () => {
    expect(migasPorDefecto({ title: 'X', navItems, activeHref: '/finalistas', pathname: '' })).toEqual([{ label: 'X' }])
  })
})
