import { describe, expect, it } from 'vitest'

import { comedorNavItems } from './comedorNavItems'

describe('comedorNavItems', () => {
  it('tiene un solo ítem, «Panel», que lleva a /panel', () => {
    expect(comedorNavItems).toHaveLength(1)
    expect(comedorNavItems[0]).toMatchObject({ label: 'Panel', href: '/panel' })
  })
})
