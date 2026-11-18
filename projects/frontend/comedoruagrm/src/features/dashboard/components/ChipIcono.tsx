import type { LucideIcon } from 'lucide-react'

import { CardTitle } from '@/components/ui/card'

import { TONOS } from './tonos'
import type { Tono } from './tonos'

/** Ícono dentro de un círculo del color del tono (decorativo). */
export function ChipIcono({ icono: Icono, tono }: { icono: LucideIcon; tono: Tono }) {
  return (
    <span
      className={`flex size-9 shrink-0 items-center justify-center rounded-full ${TONOS[tono].chip}`}
      aria-hidden
    >
      <Icono className="size-5" />
    </span>
  )
}

/** Título de una tarjeta del análisis, con su ícono de color. */
export function TituloPanel({
  icono,
  tono,
  children,
}: {
  icono: LucideIcon
  tono: Tono
  children: string
}) {
  return (
    <CardTitle className="flex items-center gap-2">
      <ChipIcono icono={icono} tono={tono} />
      {children}
    </CardTitle>
  )
}
