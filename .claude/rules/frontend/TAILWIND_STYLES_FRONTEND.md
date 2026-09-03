---
globs: projects/frontend/**/*
---

# Tailwind / Estilos — Frontend

## Siempre variables de tema, nunca valores arbitrarios

Se usan las clases de tema que ya trae shadcn/Tailwind 4
(`bg-primary`, `text-foreground`, `border-border`, `bg-muted`,
`text-destructive`, etc. — el set completo lo define
`src/index.css`/`components.json`). **No se usan valores arbitrarios**
(`w-[123px]`, `bg-[#ff0000]`, `text-[15px]`) salvo una excepción puntual y
justificada, y en ese caso el propio código lleva un comentario explicando
por qué no había alternativa dentro del sistema de diseño:

```tsx
// * Ancho fijo del ícono del proveedor externo (SVG con viewBox no estándar,
// * no hay token de tamaño del sistema que lo represente sin distorsión)
<img src={providerIcon} className="w-[37px]" />
```

Un valor arbitrario sin ese comentario es una señal de que se está
esquivando el sistema de diseño en vez de extenderlo — si hace falta un
tamaño/color nuevo con frecuencia, se agrega como token al tema, no se
repite el valor arbitrario en cada lugar que lo necesita.

## Paleta de marca (ComedorU)

Definida en `src/index.css`, sobre las mismas variables de tema que ya
espera shadcn/Tailwind 4 (`:root` para modo claro, `.dark` para modo
oscuro) — no se tocó `components.json` (`baseColor: "neutral"` sigue
siendo el base de shadcn, la marca se aplica pisando sus variables, no
reemplazando el sistema).

| Token (`bg-*`/`text-*`/`border-*`) | Uso | Claro | Oscuro |
| --- | --- | --- | --- |
| `primary` | Header, botones, íconos activos | `#1E3A56` (azul marino) | `#5F84A6` (azul claro) |
| `accent` | Acento | `#E8B657` (miel) | `#E8B657` (miel) |
| `background` | Fondo de la app | `#EAEEF3` | `#141C26` |
| `card` / `popover` / `sidebar` | Superficie / tarjetas | `#FFFFFF` | `#1C2530` |
| `foreground` / `card-foreground` | Texto principal | `#141C26` | `#E7ECF1` |
| `muted-foreground` | Texto secundario | `#5B6A78` | `#8794A2` |
| `success` | Éxito / vegetariano | `#5E7A5C` | `#7FA97D` |
| `destructive` | Error / alerta | `#B24A3C` | `#D97A66` |

`success`/`success-foreground` son tokens propios del proyecto (shadcn no
trae uno por default) — se agregaron al lado de `destructive` en
`index.css` y `@theme inline`, mismo patrón que cualquier otro color del
sistema (`bg-success`, `text-success-foreground` ya funcionan como clase de
Tailwind). Si hace falta un tono nuevo con frecuencia (ej. "advertencia"),
se agrega como token acá, no como valor arbitrario.

## Tipografía

Dos familias, vía `@fontsource-variable` (mismo mecanismo que ya traía
`Inter Variable`, ahora reemplazado por estas dos):

- **Títulos** (`font-heading`, aplicado automático a `h1`-`h6` en
  `@layer base`): **Familjen Grotesk** (`@fontsource-variable/familjen-grotesk`),
  weight 400-500.
- **Cuerpo, botones, formularios, etiquetas** (`font-sans`, default de
  `html`): **Jost** (`@fontsource-variable/jost`), weight 300-400.

Un componente que necesita la tipografía de títulos fuera de un `h1`-`h6`
(ej. un `<span>` que visualmente es un título) usa la clase `font-heading`
explícita — no se fuerza a que sea literalmente un heading semántico si no
corresponde.

## Agregar componentes vía CLI de shadcn

```bash
pnpm dlx shadcn@latest add <componente>
```

- El output del CLI aterriza en `src/components/ui/` (según
  `components.json` → `aliases.ui`).
- **No se edita a mano el output del CLI** salvo una necesidad puntual real
  (ej. un fix de accesibilidad que el componente base no cubre). Si se edita,
  se dejar constancia en un comentario arriba del cambio — de otra forma, la
  próxima vez que alguien re-corra el CLI para actualizar ese componente,
  pisa el cambio manual sin que nadie se dé cuenta de que existía.
- Componentes específicos de una sola feature (que no son de uso general del
  sistema de diseño) van en `src/features/{feature}/components/`, no en
  `src/components/ui/` — esa carpeta es exclusivamente para lo que el CLI de
  shadcn genera o lo que extiende directamente ese sistema de diseño
  compartido.
