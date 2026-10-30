import { SITE } from '@/config/site'
import { homePath, withBase } from '@/i18n/routes'
import type { Lang } from '@/i18n/utils'

export function absoluteUrl(path: string, site: URL): string {
  return new URL(path, site).href
}

// Datos estructurados de la organización para la página de inicio.
export function organizationJsonLd(lang: Lang, site: URL, slogan: string) {
  return {
    '@context': 'https://schema.org',
    '@type': 'Organization',
    name: SITE.name,
    url: absoluteUrl(homePath(lang), site),
    logo: absoluteUrl(withBase('/favicon.svg'), site),
    slogan,
    email: SITE.email,
    address: {
      '@type': 'PostalAddress',
      addressLocality: SITE.address.locality,
      addressCountry: SITE.address.country,
    },
  }
}
