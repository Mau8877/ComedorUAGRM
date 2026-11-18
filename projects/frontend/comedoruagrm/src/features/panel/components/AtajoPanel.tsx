import { Link } from '@tanstack/react-router'
import { ArrowRightIcon } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

import { ChipIcono, estiloTarjeta } from '@/features/dashboard'
import type { Tono } from '@/features/dashboard'

export interface AtajoPanelProps {
  titulo: string
  descripcion: string
  /** Ruta del módulo al que lleva. */
  href: string
  icono: LucideIcon
  tono: Tono
}

/** Un acceso directo a un módulo: tarjeta de color con ícono, clicable en toda su superficie. */
export function AtajoPanel({ titulo, descripcion, href, icono, tono }: AtajoPanelProps) {
  return (
    <Link
      to={href}
      className={`group flex items-center gap-3 rounded-xl p-4 outline-none focus-visible:ring-2 focus-visible:ring-ring ${estiloTarjeta(tono)}`}
    >
      <ChipIcono icono={icono} tono={tono} />
      <span className="flex min-w-0 flex-1 flex-col">
        <span className="font-heading text-base leading-tight">{titulo}</span>
        <span className="text-xs text-muted-foreground">{descripcion}</span>
      </span>
      <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-1" aria-hidden />
    </Link>
  )
}
