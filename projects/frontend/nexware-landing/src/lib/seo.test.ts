import { describe, expect, it } from 'vitest'
import { homePath } from '@/i18n/routes'
import { absoluteUrl, organizationJsonLd } from './seo'

const site = new URL('https://nexware.example')

describe('absoluteUrl', () => {
  it('resuelve una ruta contra el dominio del sitio', () => {
    expect(absoluteUrl('/landing-page/es/', site)).toBe('https://nexware.example/landing-page/es/')
  })
})

describe('organizationJsonLd', () => {
  it('describe a Nexware como Organization con dirección en Bolivia', () => {
    const data = organizationJsonLd('en', site, 'slogan')
    expect(data['@type']).toBe('Organization')
    expect(data.url).toBe(absoluteUrl(homePath('en'), site))
    expect(data.address.addressCountry).toBe('BO')
  })
})
