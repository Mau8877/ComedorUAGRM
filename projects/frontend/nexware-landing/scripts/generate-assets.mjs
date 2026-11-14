// Genera los PNG de public/ (favicon-32, apple-touch-icon, og-image) a partir
// del logo. Se corre a mano con `npm run assets` cuando cambia la marca; los
// PNG resultantes se commitean, no se generan en cada build.
import { readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import sharp from 'sharp'

const publicDir = new URL('../public/', import.meta.url)
const path = (name) => fileURLToPath(new URL(name, publicDir))

const favicon = await readFile(path('favicon.svg'))
await sharp(favicon, { density: 384 }).resize(32, 32).png().toFile(path('favicon-32.png'))
await sharp(favicon, { density: 384 }).resize(180, 180).png().toFile(path('apple-touch-icon.png'))

const cells = [
  [0, 0],
  [0, 15],
  [0, 30],
  [0, 45],
  [0, 60],
  [60, 0],
  [60, 15],
  [60, 30],
  [60, 45],
  [60, 60],
  [15, 15],
  [45, 45],
]
const pixels = cells
  .map(([x, y]) => `<rect x="${x}" y="${y}" width="12" height="12" rx="2.5"/>`)
  .join('')

// La imagen usa tipografía genérica: sharp no carga Space Grotesk. Si hace
// falta fidelidad exacta, se exporta el og-image desde el mockup de diseño.
const og = `<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="630" viewBox="0 0 1200 630">
  <rect width="1200" height="630" fill="#f2f5f6"/>
  <g transform="translate(96 150) scale(4.5)">
    <g fill="#1e2a33">${pixels}</g>
    <rect x="30" y="30" width="12" height="12" rx="2.5" fill="#266b66"/>
  </g>
  <text x="480" y="280" font-family="Arial, Helvetica, sans-serif" font-size="76" font-weight="700" fill="#1e2a33" letter-spacing="-3">nexware</text>
  <text x="480" y="370" font-family="Arial, Helvetica, sans-serif" font-size="40" font-weight="600" fill="#1e2a33">Si no existe, lo programamos.</text>
  <text x="480" y="430" font-family="Arial, Helvetica, sans-serif" font-size="30" fill="#266b66">Tu idea, plasmada.</text>
</svg>`
await sharp(Buffer.from(og)).png().toFile(path('og-image.png'))

console.log('Assets generados en public/')
