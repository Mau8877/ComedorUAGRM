import { describe, expect, it } from 'vitest'

import { directivoNavItems } from './directivoNavItems'

describe('directivoNavItems', () => {
  it('tiene un solo ítem, «Dashboard», que lleva a /dashboard (CU12)', () => {
    expect(directivoNavItems).toHaveLength(1)
    expect(directivoNavItems[0]).toMatchObject({ label: 'Dashboard', href: '/dashboard' })
  })

  it('no incluye la opción «Reportes» del mockup', () => {
    expect(directivoNavItems.map((item) => item.label)).not.toContain('Reportes')
  })
})
