import { describe, expect, it } from 'vitest'
import { formatDate, getLangFromUrl, isLang, useTranslations } from './utils'

describe('useTranslations', () => {
  it('lee una clave aplanada con namespace de la feature', () => {
    expect(useTranslations('es')('hero.title')).toBe('Si no existe, lo programamos.')
    expect(useTranslations('en')('hero.title')).toBe("If it doesn't exist, we build it.")
  })

  it('aplana objetos anidados con punto', () => {
    expect(useTranslations('es')('services.web.title')).toBe('Web a medida')
  })

  it('devuelve la clave tal cual si no existe en ningún idioma', () => {
    expect(useTranslations('en')('hero.noExiste')).toBe('hero.noExiste')
  })
})

describe('getLangFromUrl', () => {
  it('detecta el idioma aunque la URL lleve el prefijo base', () => {
    expect(getLangFromUrl(new URL('http://localhost/landing-page/en/terms/'))).toBe('en')
    expect(getLangFromUrl(new URL('http://localhost/landing-page/es/'))).toBe('es')
  })

  it('usa español cuando la URL no trae idioma', () => {
    expect(getLangFromUrl(new URL('http://localhost/landing-page/loquesea'))).toBe('es')
  })
})

describe('isLang', () => {
  it('acepta solo los idiomas soportados', () => {
    expect(isLang('es')).toBe(true)
    expect(isLang('fr')).toBe(false)
    expect(isLang(undefined)).toBe(false)
  })
})

describe('formatDate', () => {
  const date = new Date('2026-10-27T00:00:00Z')

  it('formatea según el idioma', () => {
    expect(formatDate(date, 'es')).toContain('octubre')
    expect(formatDate(date, 'en')).toContain('October')
  })
})
