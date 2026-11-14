import { describe, expect, it } from 'vitest'
import { LEGAL_PAGES, alternatePath, homePath, legalPath, otherLang, withBase } from './routes'

// El prefijo depende de astro.config.mjs; los tests no lo fijan a mano.
const base = import.meta.env.BASE_URL.replace(/\/$/, '')

describe('withBase', () => {
  it('antepone el base y normaliza la barra inicial', () => {
    expect(withBase('/es/')).toBe(`${base}/es/`)
    expect(withBase('es/')).toBe(`${base}/es/`)
  })
})

describe('rutas por idioma', () => {
  it('arma la ruta del inicio', () => {
    expect(homePath('es')).toBe(`${base}/es/`)
    expect(homePath('en')).toBe(`${base}/en/`)
  })

  it('cada página legal tiene una dirección distinta por idioma', () => {
    expect(legalPath('terms', 'es')).toBe(`${base}/es/terminos/`)
    expect(legalPath('terms', 'en')).toBe(`${base}/en/terms/`)
    expect(legalPath('legalNotice', 'en')).toBe(`${base}/en/legal-notice/`)
  })

  it('no repite slugs dentro de un mismo idioma', () => {
    for (const lang of ['es', 'en'] as const) {
      const slugs = LEGAL_PAGES.map((page) => page.slugs[lang])
      expect(new Set(slugs).size).toBe(slugs.length)
    }
  })
})

describe('alternatePath', () => {
  it('lleva al inicio del otro idioma', () => {
    expect(alternatePath('es')).toBe(`${base}/en/`)
    expect(alternatePath('en')).toBe(`${base}/es/`)
  })

  it('traduce la dirección de una página legal', () => {
    expect(alternatePath('es', 'privacy')).toBe(`${base}/en/privacy/`)
    expect(alternatePath('en', 'legalNotice')).toBe(`${base}/es/aviso-legal/`)
  })

  it('otherLang alterna entre los dos idiomas', () => {
    expect(otherLang('es')).toBe('en')
    expect(otherLang('en')).toBe('es')
  })
})
