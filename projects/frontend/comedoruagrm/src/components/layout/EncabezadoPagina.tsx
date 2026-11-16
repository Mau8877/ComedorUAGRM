import { Fragment } from 'react'
import type { LucideIcon } from 'lucide-react'
import { ChevronRightIcon, HomeIcon } from 'lucide-react'

import { cn } from '@/lib/utils'

export interface MigaPagina {
  label: string
  /** Opcional: un tramo con `href` se muestra como enlace (salvo el último, que es la página actual). */
  href?: string
}

interface EncabezadoPaginaProps {
  icono?: LucideIcon
  titulo: string
  /** rol > sección > página. El último tramo se resalta como chip. */
  migas?: MigaPagina[]
  className?: string
}

// * Encabezado común de las páginas de gestión: ícono + título en mayúsculas (color de acento) y, debajo,
// * un breadcrumb pequeño con ícono de casa y el último tramo resaltado. Es presentacional: quien lo usa
// * decide el ícono y las migas (el shell las deriva del ítem activo si la pantalla no las declara).
export function EncabezadoPagina({ icono: Icono, titulo, migas = [], className }: EncabezadoPaginaProps) {
  return (
    <header className={cn('flex min-w-0 items-start gap-3', className)}>
      {Icono && <Icono aria-hidden className="mt-1 size-7 shrink-0 text-accent sm:size-8" />}
      <div className="flex min-w-0 flex-col gap-1.5">
        <h1 className="font-heading text-xl font-bold tracking-wide break-words text-accent uppercase sm:text-3xl">
          {titulo}
        </h1>
        {migas.length > 0 && (
          <nav aria-label="Ruta de la página">
            <ol className="flex flex-wrap items-center gap-1.5 text-[11px] font-medium tracking-wider text-muted-foreground uppercase">
              <li className="flex items-center">
                <HomeIcon aria-hidden className="size-3.5" />
              </li>
              {migas.map((miga, index) => {
                const ultima = index === migas.length - 1
                return (
                  <Fragment key={`${miga.label}-${index}`}>
                    <li aria-hidden className="flex items-center">
                      <ChevronRightIcon className="size-3" />
                    </li>
                    <li className="flex min-w-0 items-center">
                      {ultima ? (
                        <span
                          aria-current="page"
                          className="rounded-md bg-accent/10 px-2 py-0.5 font-semibold text-accent"
                        >
                          {miga.label}
                        </span>
                      ) : miga.href ? (
                        <a href={miga.href} className="transition-colors hover:text-foreground hover:underline">
                          {miga.label}
                        </a>
                      ) : (
                        <span>{miga.label}</span>
                      )}
                    </li>
                  </Fragment>
                )
              })}
            </ol>
          </nav>
        )}
      </div>
    </header>
  )
}
