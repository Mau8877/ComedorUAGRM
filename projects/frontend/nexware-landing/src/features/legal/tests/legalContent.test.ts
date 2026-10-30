import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { SITE } from '@/config/site'
import { LEGAL_PAGES } from '@/i18n/routes'
import { LANGS } from '@/i18n/utils'

const contentDir = new URL('../../../content/legal/', import.meta.url)

function frontmatter(source: string): Record<string, string> {
  const block = source.match(/^---\n([\s\S]*?)\n---/)?.[1] ?? ''
  return Object.fromEntries(
    block.split('\n').map((line) => {
      const [key, ...rest] = line.split(':')
      return [key.trim(), rest.join(':').trim()]
    })
  )
}

describe('contenido legal', () => {
  const cases = LANGS.flatMap((lang) => LEGAL_PAGES.map((page) => ({ lang, page })))

  it.each(cases)(
    '$lang/$page.id existe y su frontmatter coincide con routes.ts',
    ({ lang, page }) => {
      const slug = page.slugs[lang]
      const source = readFileSync(new URL(`${lang}/${slug}.md`, contentDir), 'utf8')
      const data = frontmatter(source)
      expect(data.slug).toBe(slug)
      expect(data.lang).toBe(lang)
      expect(data.title).toBeTruthy()
      expect(Number.isNaN(Date.parse(data.updated))).toBe(false)
      // El correo de los textos legales no puede divergir de SITE.email.
      if (source.includes('@nexware.example')) expect(source).toContain(SITE.email)
    }
  )
})
