import { Skeleton } from '@/components/ui/skeleton'

type Forma = 'tabla' | 'tarjetas' | 'detalle'

interface CargandoContenidoProps {
  /** Texto para lectores de pantalla (ej. «Cargando convocatorias…»). */
  etiqueta: string
  forma?: Forma
}

// * Estado de carga del contenedor central: reemplaza al texto «Cargando…» suelto. Solo se dibuja en el área
// * de contenido, nunca en header/sidebar. El texto sigue en el DOM (sr-only) para accesibilidad.
export function CargandoContenido({ etiqueta, forma = 'tabla' }: CargandoContenidoProps) {
  return (
    <div role="status" aria-busy="true" className="flex flex-col gap-3 py-2">
      <span className="sr-only">{etiqueta}</span>
      {forma === 'tabla' && (
        <>
          <Skeleton className="h-9 w-full" />
          {Array.from({ length: 6 }, (_, i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </>
      )}
      {forma === 'tarjetas' && (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }, (_, i) => (
            <Skeleton key={i} className="h-28 w-full" />
          ))}
        </div>
      )}
      {forma === 'detalle' && (
        <>
          <Skeleton className="h-8 w-1/3" />
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-24 w-full" />
        </>
      )}
    </div>
  )
}
