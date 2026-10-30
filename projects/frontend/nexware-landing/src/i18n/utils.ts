// Los textos viven en src/i18n/{Feature}/es.json y en.json (uno por feature,
// no un diccionario único). Cada feature es un namespace: la clave
// `title` de Hero/es.json se lee como `hero.title`. Los JSON pueden anidar
// objetos (`web.title`), que se aplanan con punto -> `services.web.title`.
export const LANGS = ['es', 'en'] as const
export type Lang = (typeof LANGS)[number]
export const DEFAULT_LANG: Lang = 'es'

type Dictionary = Record<string, string>

const modules = import.meta.glob<Record<string, unknown>>('./*/{es,en}.json', {
  eager: true,
  import: 'default',
})

function flatten(value: Record<string, unknown>, prefix: string, out: Dictionary): void {
  for (const [key, child] of Object.entries(value)) {
    const path = `${prefix}.${key}`
    if (typeof child === 'string') out[path] = child
    else if (child && typeof child === 'object')
      flatten(child as Record<string, unknown>, path, out)
  }
}

const dictionaries: Record<Lang, Dictionary> = { es: {}, en: {} }

for (const [path, content] of Object.entries(modules)) {
  const match = path.match(/^\.\/([^/]+)\/(es|en)\.json$/)
  if (!match) continue
  const [, folder, lang] = match
  const namespace = folder.charAt(0).toLowerCase() + folder.slice(1)
  flatten(content, namespace, dictionaries[lang as Lang])
}

export function isLang(value: string | undefined): value is Lang {
  return (LANGS as readonly string[]).includes(value ?? '')
}

export function getLangFromUrl(url: URL): Lang {
  const segments = url.pathname.split('/').filter(Boolean)
  const lang = segments.find((segment) => isLang(segment))
  return lang ?? DEFAULT_LANG
}

export function useTranslations(lang: Lang) {
  return (key: string): string => dictionaries[lang][key] ?? dictionaries[DEFAULT_LANG][key] ?? key
}

export function formatDate(date: Date, lang: Lang): string {
  const locale = lang === 'es' ? 'es-BO' : 'en-US'
  return new Intl.DateTimeFormat(locale, { dateStyle: 'long', timeZone: 'UTC' }).format(date)
}
