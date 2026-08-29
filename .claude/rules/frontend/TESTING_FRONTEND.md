---
globs: projects/frontend/**/*
---

# Testing — Frontend

## Setup (ya instalado)

**Vitest** (4.x), configurado directo en `vite.config.ts` — no hay un
`vitest.config.ts` separado, se agrega un bloque `test` al mismo config de
Vite (con `/// <reference types="vitest/config" />` arriba del archivo para
que TypeScript reconozca esa propiedad):

```ts
/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
// ...

export default defineConfig({
  // ...plugins, resolve, server...
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/setupTests.ts',
  },
})
```

- `@testing-library/react` + `@testing-library/user-event` para tests de
  componentes (render + interacción simulada).
- `@testing-library/jest-dom` para matchers (`toBeInTheDocument()`, etc.),
  importado una sola vez en `src/setupTests.ts`.
- Comando: `pnpm test` (= `vitest run`, corre una vez y termina — no queda
  en modo watch, así sirve para el hook de pre-commit y para CI).

## Dónde viven los tests

**Co-ubicados** junto al archivo que prueban, no en una carpeta `__tests__/`
separada:

```
src/utils/
├── debounce.ts
└── debounce.test.ts

src/components/ui/
├── button.tsx
└── button.test.tsx
```

Mismo criterio en `src/features/{feature}/` cuando exista código de
negocio: el test de `usuarioSchema.ts` es `usuarioSchema.test.ts` al lado,
no en otro directorio.

## Qué testear

- **Funciones puras / utilidades** (`src/utils/`, `src/lib/`): siempre que
  tengan lógica no trivial — ver `debounce.test.ts` como referencia (usa
  `vi.useFakeTimers()` para probar el timing sin esperas reales).
- **Componentes de `src/components/ui/`**: comportamiento (qué renderiza,
  qué eventos dispara), no implementación interna ni estilos.
- **Hooks de `api/`** (cuando existan): mockeando `apiClient` — no se pega
  contra un backend real en un test unitario.

No se exige cobertura exhaustiva de cada componente de UI trivial (un
layout sin lógica, un wrapper que solo pasa props) — mismo criterio de
proporcionalidad que [TESTING_MOBILE.md](../mobile/TESTING_MOBILE.md).

**El pre-commit no corre `vitest run`** (ver
[ESTANDARES_CODIGO.md](../ESTANDARES_CODIGO.md#enforcement-automático-pre-commit)) —
correr `pnpm test` antes de commitear queda a criterio de cada dev, no es
forzado por un hook. Si el código ya tiene un test escrito, igual se
espera actualizarlo cuando un cambio le rompe el comportamiento, pero nadie
te lo va a bloquear automáticamente si te olvidás.

## Convención de nombres

- Archivo: `{nombre}.test.ts` / `{nombre}.test.tsx` (sufijo `.test`, no
  `.spec`).
- `describe`: nombre del módulo/componente bajo test.
- `it`/`test`: describe el comportamiento en español, en minúscula, sin
  prefijo redundante (`it('renderiza el texto que recibe', ...)`, no
  `it('should render the text', ...)`).
