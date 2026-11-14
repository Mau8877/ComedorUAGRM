import { describe, expect, it } from 'vitest'

type Json = Record<string, unknown>

const files = import.meta.glob<Json>('./*/{es,en}.json', { eager: true, import: 'default' })

function keysOf(value: Json, prefix = ''): string[] {
  return Object.entries(value).flatMap(([key, child]) => {
    const path = prefix ? `${prefix}.${key}` : key
    return child && typeof child === 'object' ? keysOf(child as Json, path) : [path]
  })
}

const features = [...new Set(Object.keys(files).map((path) => path.split('/')[1]))]

describe('diccionarios i18n', () => {
  it('hay al menos una feature', () => {
    expect(features.length).toBeGreaterThan(0)
  })

  it.each(features)('%s tiene es.json y en.json con las mismas claves', (feature) => {
    const es = files[`./${feature}/es.json`]
    const en = files[`./${feature}/en.json`]
    expect(es, `${feature}/es.json`).toBeDefined()
    expect(en, `${feature}/en.json`).toBeDefined()
    expect(keysOf(en).sort()).toEqual(keysOf(es).sort())
  })

  it('ningún texto queda vacío', () => {
    for (const [path, content] of Object.entries(files)) {
      for (const key of keysOf(content)) {
        const value = key.split('.').reduce<unknown>((node, part) => (node as Json)[part], content)
        expect(value, `${path} -> ${key}`).not.toBe('')
      }
    }
  })
})
