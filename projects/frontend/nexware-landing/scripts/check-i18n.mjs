// Falla (exit 1) si algún idioma no tiene exactamente las mismas claves que
// el otro en alguna feature, o si el código usa una clave que no existe.
//   src/i18n/{Feature}/es.json  <->  src/i18n/{Feature}/en.json
import { readdir, readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { join } from 'node:path'

const root = fileURLToPath(new URL('../src/', import.meta.url))
const i18nDir = join(root, 'i18n')
const LANGS = ['es', 'en']
const errors = []

function flatten(value, prefix, out) {
  for (const [key, child] of Object.entries(value)) {
    const path = prefix ? `${prefix}.${key}` : key
    if (typeof child === 'string') out.add(path)
    else if (child && typeof child === 'object') flatten(child, path, out)
  }
  return out
}

const known = new Set()
const features = (await readdir(i18nDir, { withFileTypes: true })).filter((e) => e.isDirectory())

for (const feature of features) {
  const namespace = feature.name.charAt(0).toLowerCase() + feature.name.slice(1)
  const keysByLang = {}
  for (const lang of LANGS) {
    const file = join(i18nDir, feature.name, `${lang}.json`)
    try {
      keysByLang[lang] = flatten(JSON.parse(await readFile(file, 'utf8')), '', new Set())
    } catch {
      errors.push(`${feature.name}/${lang}.json: falta el archivo o no es JSON válido`)
      keysByLang[lang] = new Set()
    }
  }
  for (const lang of LANGS) {
    const other = LANGS.find((l) => l !== lang)
    for (const key of keysByLang[lang]) {
      if (!keysByLang[other].has(key))
        errors.push(`${feature.name}: la clave "${key}" está en ${lang} pero falta en ${other}`)
    }
  }
  for (const key of keysByLang.es) known.add(`${namespace}.${key}`)
}

// Claves escritas literalmente como t('feature.clave') en el código.
async function* walk(dir) {
  for (const entry of await readdir(dir, { withFileTypes: true })) {
    const full = join(dir, entry.name)
    if (entry.isDirectory()) yield* walk(full)
    else if (/\.(astro|ts)$/.test(entry.name)) yield full
  }
}

for await (const file of walk(root)) {
  const source = await readFile(file, 'utf8')
  for (const match of source.matchAll(/\bt\(\s*['"]([A-Za-z][\w.]*)['"]\s*\)/g)) {
    if (!known.has(match[1]))
      errors.push(
        `${file.replace(root, 'src/')}: la clave "${match[1]}" no existe en los diccionarios`
      )
  }
}

if (errors.length > 0) {
  console.error(`check:i18n falló (${errors.length}):\n- ${errors.join('\n- ')}`)
  process.exit(1)
}
console.log(`check:i18n OK: ${features.length} features, ${known.size} claves, es y en alineados.`)
