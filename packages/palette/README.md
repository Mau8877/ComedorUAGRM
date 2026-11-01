# @comedoru/palette

Fuente única de los colores de ComedorU, usada por el frontend web (CSS) y
mobile (Dart).

- `palette.json` — **lo único que se edita**: tokens `light` y `dark`
  (hex `#RRGGBB`, o `#RRGGBBAA` para colores con transparencia).
- `palette.css` — generado: variables `:root` / `.dark` (el frontend lo importa).
- `lib/comedoru_palette.dart` — generado: `ComedoruPalette.lightPrimary`, etc.

Tras cambiar `palette.json`:

```bash
node packages/palette/scripts/build.mjs
```

y commitear los archivos generados (así Flutter no necesita Node).
