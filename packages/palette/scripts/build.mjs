// Genera palette.css (frontend) y lib/comedoru_palette.dart (mobile) a partir
// de palette.json -- esa es la única fuente de verdad de colores y estilo.
// Se corre solo: predev/prebuild del frontend, el plugin de Vite al editar
// palette.json, y el pre-commit. A mano: node packages/palette/scripts/build.mjs
import { readFileSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const { style, light, dark } = JSON.parse(readFileSync(join(root, 'palette.json'), 'utf8'))
const HEADER = 'GENERADO desde palette.json -- no editar a mano'

const vars = (tokens) =>
  Object.entries(tokens)
    .map(([name, hex]) => `  --${name}: ${hex};`)
    .join('\n')

const css = `/* ${HEADER} */
@theme inline {
  --font-heading: '${style.fonts.heading.cssFamily}', sans-serif;
  --font-sans: '${style.fonts.body.cssFamily}', sans-serif;
}

:root {
  --radius: ${style.radius};
${vars(light)}
}

.dark {
${vars(dark)}
}
`
writeFileSync(join(root, 'palette.css'), css)

const camel = (s) => s.replace(/-(\w)/g, (_, c) => c.toUpperCase())
// #RRGGBB o #RRGGBBAA (CSS) -> 0xAARRGGBB (Dart)
const toDart = (hex) => {
  const h = hex.slice(1).toUpperCase()
  return `0x${h.length === 8 ? h.slice(6) + h.slice(0, 6) : 'FF' + h}`
}
const dartMode = (mode, tokens) =>
  Object.entries(tokens)
    .map(
      ([name, hex]) =>
        `  static const ${mode}${camel(name).replace(/^./, (c) => c.toUpperCase())} = Color(${toDart(hex)});`
    )
    .join('\n')

const dart = `// ${HEADER}
import 'package:flutter/painting.dart';

/// Colores de marca de ComedorU, compartidos con el frontend web.
abstract final class ComedoruPalette {
${dartMode('light', light)}

${dartMode('dark', dark)}
}

/// Tipografías (nombres de Google Fonts) y radio de bordes de ComedorU.
abstract final class ComedoruStyle {
  static const fontHeading = '${style.fonts.heading.family}';
  static const fontBody = '${style.fonts.body.family}';

  /// ${style.radius} en px lógicos (1rem = 16).
  static const radius = ${(parseFloat(style.radius) * 16).toFixed(1)};
}
`
writeFileSync(join(root, 'lib', 'comedoru_palette.dart'), dart)
