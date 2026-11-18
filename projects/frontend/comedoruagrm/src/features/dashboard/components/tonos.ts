// * Colores de acento del dashboard: solo tokens del tema (claro/oscuro). Las clases van completas porque Tailwind
// * no detecta nombres armados con texto.
export type Tono = 'primary' | 'accent' | 'success' | 'info' | 'warning'

export const TONOS: Record<Tono, { tarjeta: string; chip: string }> = {
  primary: {
    tarjeta: 'border-t-4 border-t-primary bg-linear-to-br from-primary/10 to-card',
    chip: 'bg-primary/15 text-primary',
  },
  accent: {
    tarjeta: 'border-t-4 border-t-accent bg-linear-to-br from-accent/10 to-card',
    chip: 'bg-accent/15 text-accent',
  },
  success: {
    tarjeta: 'border-t-4 border-t-success bg-linear-to-br from-success/10 to-card',
    chip: 'bg-success/15 text-success',
  },
  info: {
    tarjeta: 'border-t-4 border-t-info bg-linear-to-br from-info/10 to-card',
    chip: 'bg-info/15 text-info',
  },
  warning: {
    tarjeta: 'border-t-4 border-t-warning bg-linear-to-br from-warning/10 to-card',
    chip: 'bg-warning/15 text-warning',
  },
}

/** Clases de una tarjeta con borde superior de color y fondo con un degradado suave. */
export function estiloTarjeta(tono: Tono): string {
  return `${TONOS[tono].tarjeta} shadow-md transition-shadow hover:shadow-lg`
}
