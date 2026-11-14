import { defineCollection } from 'astro:content'
import { glob } from 'astro/loaders'
import { z } from 'astro/zod'

// Un Markdown por página legal e idioma: src/content/legal/{es,en}/*.md
const legal = defineCollection({
  // El id sale de la ruta (es/cookies), no del `slug` del frontmatter: en
  // cookies el slug es igual en los dos idiomas y el id chocaría.
  loader: glob({
    pattern: '**/*.md',
    base: './src/content/legal',
    generateId: ({ entry }) => entry.replace(/\.md$/, ''),
  }),
  schema: z.object({
    title: z.string(),
    slug: z.string(),
    lang: z.enum(['es', 'en']),
    updated: z.coerce.date(),
  }),
})

export const collections = { legal }
