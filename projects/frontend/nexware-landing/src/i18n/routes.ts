import type { Lang } from './utils'

// El prefijo `base` de astro.config.mjs (/landing-page) se antepone a todo
// enlace interno: nginx reenvía esa ruta tal cual, sin recortarla.
const BASE = import.meta.env.BASE_URL.replace(/\/$/, '')

export function withBase(path: string): string {
  return `${BASE}${path.startsWith('/') ? path : `/${path}`}`
}

// Las páginas legales cambian de dirección entre idiomas
// (/es/terminos/ <-> /en/terms/), así que se resuelven con este mapa.
export const LEGAL_PAGES = [
  { id: 'terms', slugs: { es: 'terminos', en: 'terms' } },
  { id: 'privacy', slugs: { es: 'privacidad', en: 'privacy' } },
  { id: 'cookies', slugs: { es: 'cookies', en: 'cookies' } },
  { id: 'legalNotice', slugs: { es: 'aviso-legal', en: 'legal-notice' } },
] as const satisfies readonly { id: string; slugs: Record<Lang, string> }[]

export type LegalId = (typeof LEGAL_PAGES)[number]['id']

export function homePath(lang: Lang): string {
  return withBase(`/${lang}/`)
}

export function legalPath(id: LegalId, lang: Lang): string {
  const page = LEGAL_PAGES.find((p) => p.id === id)!
  return withBase(`/${lang}/${page.slugs[lang]}/`)
}

export function otherLang(lang: Lang): Lang {
  return lang === 'es' ? 'en' : 'es'
}

// Misma página en el otro idioma. `legalId` es undefined en el inicio.
export function alternatePath(lang: Lang, legalId?: LegalId): string {
  const target = otherLang(lang)
  return legalId ? legalPath(legalId, target) : homePath(target)
}
