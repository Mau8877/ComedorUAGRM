/// <reference types="vitest/config" />
import { getViteConfig } from 'astro/config'

// getViteConfig aplica la config de Astro (alias, base, import.meta.glob)
// para que los tests vean el mismo entorno que el build.
export default getViteConfig({
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts'],
  },
})
