import { cn } from '@/lib/utils'

// * Marca de ComedorU (plato + U + cubiertos), de packages/comedoru-icons
// * (comedoru-mark-currentColor.svg): plato y cubiertos heredan el color del
// * texto (blanco sobre el header navy, primary sobre la landing) y la U
// * siempre va en el rojo de marca (accent).
export function ComedoruLogo({ className }: { className?: string }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 96 96"
      role="img"
      aria-label="ComedorU"
      className={cn('size-9', className)}
    >
      <circle cx="48" cy="48" r="24" fill="none" stroke="currentColor" strokeWidth="7" />
      <path
        d="M40 38.5V49.5a8 8 0 0 0 16 0V38.5"
        fill="none"
        className="stroke-accent"
        strokeWidth="8"
        strokeLinecap="round"
      />
      <path
        d="M3 22V32a6 6 0 0 0 12 0V22M9 22V76"
        fill="none"
        stroke="currentColor"
        strokeWidth="4"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <ellipse cx="89" cy="33" rx="6" ry="10" fill="currentColor" />
      <path d="M89 42V76" fill="none" stroke="currentColor" strokeWidth="4" strokeLinecap="round" />
    </svg>
  )
}
